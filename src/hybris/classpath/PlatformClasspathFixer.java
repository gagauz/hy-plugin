package hybris.classpath;

import static hybris.extension.ClasspathTools.createElement;
import static hybris.extension.ClasspathTools.createLibClasspathEntry;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hybris.ant.DirVisitor;
import hybris.ant.ImportOption;
import hybris.extension.Extension;
import hybris.extension.JarFetcher;
import hybris.extension.LocalExtensionVisitor;
import hybristools.utils.FileUtiles;
import hybristools.utils.XmlManipulator;
import hybristools.utils.XmlUtils;

public class PlatformClasspathFixer {
    private Extension platform;

    public PlatformClasspathFixer(Extension platform) {
        this.platform = platform;
    }

    /*
     Implement resource filters
      <filteredResources>
        <filter>
            <id>1477042328867</id>
            <name></name>
            <type>10</type>
            <matcher>
                <id>org.eclipse.ui.ide.multiFilter</id>
                <arguments>1.0-name-matches-false-false-ext</arguments>
            </matcher>
        </filter>
    </filteredResources>
     */

    public void fixClasspath() {

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProject[] projects = workspace.getRoot().getProjects();
        final Set<IProject> workspaceProject = new HashSet<>(projects.length);
        final Set<String> workspaceProjectNames = new HashSet<>(projects.length);

        if (null == ImportOption.currentOption) {

            ImportOption.currentOption = ImportOption.CUSTOM_ONLY;
            workspaceProject.addAll(Arrays.asList(projects));
            workspaceProject.forEach(project -> {
                workspaceProjectNames.add(project.getName());
                if (project.getLocation().toPortableString().contains("/platform/ext/")) {
                    ImportOption.currentOption = ImportOption.ALL;
                } else if (!ImportOption.ALL.is() && project.getLocation().toPortableString().contains("/bin/ext-")) {
                    ImportOption.currentOption = ImportOption.BIN_NOT_PLATFORM;
                }
            });
        }

        try {
            XmlUtils.doWithXmlFile(platform.getClasspath(), doc -> {
                NodeList nodes = doc.getElementsByTagName("classpath");
                for (int i = 0; i < nodes.getLength();) {
                    Node node = nodes.item(i);

                    while (node.hasChildNodes())
                        node.removeChild(node.getFirstChild());

                    final Map<String, Element> nodeMap = new HashMap<>();
                    final Map<String, File> sourceMap = new HashMap<>();

                    final XmlManipulator projectXml = new XmlManipulator(platform.getProject());

                    //                    File classesDir = new File(ramDir, "modelclasses");
                    //
                    //                    if (!classesDir.exists()) {
                    //                        classesDir.mkdir();
                    //                    }

                    //                    try {
                    //                        projectXml.setNodeValue("projectDescription/linkedResources/link/name", "eclipsebin");
                    //                        projectXml.setNodeValue("projectDescription/linkedResources/link/type", "2");
                    //                        projectXml.setNodeValue("projectDescription/linkedResources/link/location",
                    //                                new Path(ramDir.getAbsolutePath()).toPortableString());
                    //                        projectXml.saveDocument();
                    //                    } catch (Exception e) {
                    //                        e.printStackTrace();
                    //                    }

                    FileUtiles.removeFolder(new File(platform.getFolder(), "eclipsebin"));
                    FileUtiles.removeFolder(new File(platform.getFolder(), "bootstrap/modelclasses"));

                    createElement(node, "classpathentry", "kind", "output", "path", "eclipsebin/notused");
                    createElement(node, "classpathentry", "kind", "con", "path", "org.eclipse.jdt.launching.JRE_CONTAINER");
                    //                    createElement(node, "classpathentry", "kind", "con", "path", JavaCore.USER_LIBRARY_CONTAINER_ID + '/' + Constants.CUSTOM_PROJECT_CP_LIB);
                    createElement(node, "classpathentry", "kind", "src", "path", "/config", "exported", "true");
                    createElement(node, "classpathentry", "kind", "src", "path", "bootstrap/gensrc", "output", "bootstrap/modelclasses",
                            "exported", "true");
                    createLibClasspathEntry(node, "bootstrap/resources", true);
                    IPath platformPath = new Path(platform.getFolder().getAbsolutePath());

                    Set<String> addedJars = new HashSet<>(100);
                    // Exclude models.jar
                    addedJars.add("models.jar");
                    addedJars.add("zul-8.0.0.jar");
                    addedJars.add("jrebel-activation.jar");

                    Consumer<File> fetchJar = f -> {

                        if (addedJars.add(f.getName())) {
                            if (f.getName().endsWith("-sources.jar")) {
                                String jar = f.getName().replace("-sources.jar", ".jar");
                                sourceMap.put(jar, f);
                            } else {
                                IPath jarPath = new Path(f.getAbsolutePath());
                                String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                Element el = createLibClasspathEntry(node, jarPathString, true);
                                nodeMap.put(f.getName(), el);
                            }
                        }
                    };

                    new JarFetcher(platform.getFileInFolder("bootstrap")).fetch(fetchJar);

                    new JarFetcher(platform.getFolder(), "lib").fetch(fetchJar);
                    new JarFetcher(platform.getFileInFolder("tomcat")).fetch(fetchJar);

                    // Get libs from custom
                    new LocalExtensionVisitor(platform.getFolder()).visit(ext -> {
                        if (addedJars.add(ext.getFolder().getAbsolutePath())) {
                            new JarFetcher(ext.getFolder(), "bin", "lib", "web/webroot/WEB-INF/lib").fetch(fetchJar);
                            if (!ext.isCustom()) {
                                if (ext.hasFolder("classes") && addedJars.add(ext.getFolder().getAbsolutePath() + "/classes")) {
                                    IPath jarPath = new Path(ext.getFolder().getAbsolutePath() + "/classes");
                                    String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                    createLibClasspathEntry(node, jarPathString, true);
                                }
                                if (ext.hasFolder("web/webroot/WEB-INF/classes")
                                        && addedJars.add(ext.getFolder().getAbsolutePath() + "/web/webroot/WEB-INF/classes")) {
                                    IPath jarPath = new Path(ext.getFolder().getAbsolutePath() + "/web/webroot/WEB-INF/classes");
                                    String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                    createLibClasspathEntry(node, jarPathString, true);
                                }
                                if (ext.hasFolder("resources")) {
                                    IPath jarPath = new Path(ext.getFolder().getAbsolutePath() + "/resources");
                                    String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                    createLibClasspathEntry(node, jarPathString, true);
                                }

                            }
                        }
                    });

                    // platform/ext extensions as libraries
                    new DirVisitor(new File(platform.getFolder(), "ext"), 3).visitRecursive(f0 -> {
                        if (f0.isDirectory() && new File(f0, "extensioninfo.xml").isFile()) {
                            if (addedJars.add(f0.getAbsolutePath())) {

                                if (new File(f0, "classes").isDirectory()) {
                                    IPath jarPath = new Path(f0.getAbsolutePath() + "/classes");
                                    String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                    createLibClasspathEntry(node, jarPathString, true);
                                }
                                if (new File(f0, "resources").isDirectory()) {
                                    IPath jarPath = new Path(f0.getAbsolutePath() + "/resources");
                                    String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                    createLibClasspathEntry(node, jarPathString, true);
                                }

                                new DirVisitor(f0, 2).visitRecursive(f -> {
                                    if (f.getName().endsWith(".jar")) {
                                        fetchJar.accept(f);
                                    }
                                });
                            }
                        }
                    });

                    sourceMap.forEach((k, v) -> {
                        nodeMap.computeIfPresent(k, (k0, v0) -> {
                            IPath jarPath = new Path(v.getAbsolutePath());
                            String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                            v0.setAttribute("sourcepath", jarPathString);
                            return v0;
                        });
                    });

                    break;
                }
                return true;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean hasProject(String name) {
        try {
            IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
            return p.exists();
        } catch (Exception e) {
        }
        return false;
    }

    private static String getExtensionClassesPath(Extension extension) {
        return new Path("R:/" + extension.getName() + "/classes").toPortableString();
    }
}
