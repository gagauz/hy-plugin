package com.xl0e.hybris.classpath;

import static com.xl0e.hybris.extension.ClasspathTools.createElement;
import static com.xl0e.hybris.extension.ClasspathTools.createLibClasspathEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.xl0e.hybris.Constants;
import com.xl0e.hybris.ant.ImportOption;
import com.xl0e.hybris.extension.Extension;
import com.xl0e.hybris.extension.JarFetcher;
import com.xl0e.hybris.extension.LocalExtensionVisitor;
import com.xl0e.hybris.utils.DirVisitor;
import com.xl0e.hybris.utils.FileUtiles;
import com.xl0e.hybris.utils.XmlUtils;

public class PlatformClasspathFixer {

    private static final Set<String> excludedJars = new HashSet<>();
    static {
        excludedJars.add("models.jar");
        excludedJars.add("zul-8.0.0.jar");
        excludedJars.add("jrebel-activation.jar");
        excludedJars.add("webfragmentCore_backofficeContextClassloaderFilter.jar");
        excludedJars.add("webfragmentCore_backofficefilterchain.jar");
        excludedJars.add("webfragmentCore_backofficeMobileFilter.jar");
        excludedJars.add("webfragmentCore_requestContextFilter.jar");
        excludedJars.add("webfragmentCore_springSecurityFilterChain.jar");
        excludedJars.add("webfragmentCore_XSSFilter.jar");
    }

    private Extension platform;
    private IPath platformPath;
    private Map<String, File> sourceMap = new HashMap<>();

    public PlatformClasspathFixer(Extension platform) {
        this.platform = platform;
        this.platformPath = new Path(platform.getFolder().getAbsolutePath());
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

    private String getSource(final File jarFile) {
        return Optional.ofNullable(sourceMap.get(jarFile.getName()))
                .map(this::relativizeToPlatform).orElse(null);
    }

    private String relativizeToPlatform(final File file) {
        return relativizeToPlatform(file.getAbsolutePath());
    }

    private String relativizeToPlatform(final String filePath) {
        IPath jarPath = new Path(filePath);
        String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
        return jarPathString;
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

                    final Map<String, File> jarMap = new HashMap<>();
                    sourceMap = new HashMap<>();

                    platform.getFilesInFolder("sources", Constants.JAR_FILTER)
                            .forEach(jarFile -> sourceMap.put(jarFile.getName().replace("-sources.jar", ".jar"), jarFile));

                    FileUtiles.removeFolder(new File(platform.getFolder(), "eclipsebin"));
                    FileUtiles.removeFolder(new File(platform.getFolder(), "bootstrap/modelclasses"));

                    createElement(node, "classpathentry", "kind", "output", "path", "eclipsebin/notused");
                    createElement(node, "classpathentry", "kind", "con", "path", "org.eclipse.jdt.launching.JRE_CONTAINER");
                    createElement(node, "classpathentry", "kind", "src", "path", "/config", "exported", "true");
                    createElement(node, "classpathentry", "kind", "src", "path", "bootstrap/gensrc", "output", "bootstrap/modelclasses",
                            "exported", "true");
                    createLibClasspathEntry(node, "bootstrap/resources", true);

                    Set<String> addedJars = new HashSet<>(200);
                    // Exclude models.jar
                    addedJars.addAll(excludedJars);

                    Consumer<File> jarCollector = jarFile -> {
                        if (addedJars.add(jarFile.getName())) {
                            jarMap.put(jarFile.getName(), jarFile);
                        }
                    };

                    new JarFetcher(platform.getFileInFolder("bootstrap")).fetch(jarCollector);
                    new JarFetcher(platform.getFolder(), "lib").fetch(jarCollector);
                    new JarFetcher(platform.getFileInFolder("tomcat")).fetch(jarCollector);

                    // Get libs from custom
                    new LocalExtensionVisitor(platform.getFolder()).visit(ext -> {
                        if (addedJars.add(ext.getFolder().getAbsolutePath())) {
                            new JarFetcher(ext.getFolder(), "bin", "lib", "web/webroot/WEB-INF/lib", "resources/backoffice")
                                    .fetch(jarCollector);
                            if (!ext.isCustom()) {
                                if (ext.hasFolder("classes") && addedJars.add(ext.getFolder().getAbsolutePath() + "/classes")) {
                                    createLibClasspathEntry(node, relativizeToPlatform(ext.getFolder().getAbsolutePath() + "/classes"),
                                            true);
                                }
                                if (ext.hasFolder("web/webroot/WEB-INF/classes")
                                        && addedJars.add(ext.getFolder().getAbsolutePath() + "/web/webroot/WEB-INF/classes")) {
                                    createLibClasspathEntry(node,
                                            relativizeToPlatform(ext.getFolder().getAbsolutePath() + "/web/webroot/WEB-INF/classes"), true);
                                }
                                if (ext.hasFolder("resources")) {
                                    createLibClasspathEntry(node, relativizeToPlatform(ext.getFolder().getAbsolutePath() + "/resources"),
                                            true);
                                }

                            }
                        }
                    });

                    // platform/ext extensions as libraries
                    new DirVisitor(new File(platform.getFolder(), "ext"), 3).visitRecursive(f0 -> {
                        if (f0.isDirectory() && new File(f0, "extensioninfo.xml").isFile()) {
                            if (addedJars.add(f0.getAbsolutePath())) {

                                if (new File(f0, "classes").isDirectory()) {
                                    createLibClasspathEntry(node, relativizeToPlatform(f0.getAbsolutePath() + "/classes"), true);
                                }
                                if (new File(f0, "resources").isDirectory()) {
                                    createLibClasspathEntry(node, relativizeToPlatform(f0.getAbsolutePath() + "/resources"), true);
                                }

                                new DirVisitor(f0, 2).visitRecursive(f -> {
                                    if (f.getName().endsWith(".jar")) {
                                        jarCollector.accept(f);
                                    }
                                });
                            }
                        }
                    });

                    buildClasspathEntriesForJars(node, jarMap);

                    break;
                }
                return true;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildClasspathEntriesForJars(final Node node, final Map<String, File> jarMap) {
        List<File> files = new ArrayList<>(jarMap.values());

        sort(files, jarMap.keySet());

        files.forEach(jarFile -> {
            IPath jarPath = new Path(jarFile.getAbsolutePath());
            String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
            createLibClasspathEntry(node, jarPathString, getSource(jarFile), true);
        });

    }

    private void sort(List<File> files, Collection<String> names) {
        files.sort((a, b) -> {
            return b.getName().compareTo(a.getName());
        });
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
