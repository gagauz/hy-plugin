package hybristools;

import static hybristools.ClasspathTools.createElement;
import static hybristools.ClasspathTools.createLibClasspathEntry;
import static hybristools.ClasspathTools.createSrcClasspathEntry;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hybristools.utils.XmlUtils;

public class PlatformClasspathFixer {
    private Extension platform;

    public PlatformClasspathFixer(Extension platform) {
        this.platform = platform;
    }

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

                    createElement(node, "classpathentry", "kind", "output", "path", "eclipsebin/notused");
                    createElement(node, "classpathentry", "kind", "con", "path", "org.eclipse.jdt.launching.JRE_CONTAINER");
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
                        if (addedJars.add(f.getName()) && !f.getName().endsWith("-sources.jar")) {
                            IPath jarPath = new Path(f.getAbsolutePath());
                            String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                            createLibClasspathEntry(node, jarPathString, true);
                        }
                    };

                    new JarFetcher(platform.getFileInFolder("bootstrap")).fetch(fetchJar);

                    new JarFetcher(platform.getFolder(), "lib").fetch(fetchJar);
                    new JarFetcher(platform.getFileInFolder("tomcat")).fetch(fetchJar);

                    new LocalExtensionVisitor(platform.getFolder()).visit(ext -> {
                        if (addedJars.add(ext.getFolder().getAbsolutePath())) {
                            new JarFetcher(ext.getFolder(), "bin", "lib", "web/webroot/WEB-INF/lib").fetch(jar -> {
                                if (addedJars.add(jar.getName())) {
                                    IPath jarPath = new Path(jar.getAbsolutePath());
                                    String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                    createLibClasspathEntry(node, jarPathString, true);
                                }
                            });
                            if (ImportOption.CUSTOM_ONLY.is()) {
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
                            }
                        }
                    });

                    // platform/ext extensions as libraries
                    new DirVisitor(new File(platform.getFolder(), "ext"), 2).visitRecursive(f0 -> {
                        if (f0.isDirectory() && new File(f0, "extensioninfo.xml").isFile()) {
                            if (addedJars.add(f0.getAbsolutePath())) {
                                if (!ImportOption.ALL.is() || !hasProject(f0.getName())) {
                                    new DirVisitor(f0, 2).visitRecursive(f -> {
                                        if (f.getName().endsWith(".jar") && addedJars.add(f.getName())) {
                                            IPath jarPath = new Path(f.getAbsolutePath());
                                            String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                            createLibClasspathEntry(node, jarPathString, true);
                                        } else if (f.isDirectory() && new File(f, "extensioninfo.xml").isFile()) {
                                            if (new File(f, "src").isDirectory()) {
                                                IPath jarPath = new Path(f.getAbsolutePath() + "/src");
                                                String dirPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                                createSrcClasspathEntry(node, dirPathString, true);
                                            } else if (new File(f, "classes").isDirectory()) {
                                                IPath jarPath = new Path(f.getAbsolutePath() + "/classes");
                                                String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                                createLibClasspathEntry(node, jarPathString, true);
                                            }
                                        }
                                    });
                                } else {
                                    createSrcClasspathEntry(node, '/' + f0.getName(), true);
                                }
                            }
                        }
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
}
