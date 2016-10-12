package hybristools;

import static hybristools.ClasspathTools.createElement;
import static hybristools.ClasspathTools.createLibClasspathEntry;
import static hybristools.ClasspathTools.createSrcClasspathEntry;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hybristools.utils.XmlUtils;

public class PlatformClasspathFixer {
    private Extension platformFolder;

    public PlatformClasspathFixer(Extension platformFolder) {
        this.platformFolder = platformFolder;
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
            XmlUtils.doWithXmlFile(platformFolder.getClasspath(), doc -> {
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
                    IPath platformPath = new Path(platformFolder.getFolder().getAbsolutePath());

                    Set<String> addedJars = new HashSet<>(100);
                    // Exclude models.jar
                    addedJars.add("models.jar");
                    addedJars.add("zul-8.0.0.jar");
                    addedJars.add("jrebel-activation.jar");

                    new DirVisitor(new File(platformFolder.getFolder(), "bootstrap"), 2).visitRecursive(f -> {
                        if (f.getName().endsWith(".jar") && addedJars.add(f.getName())) {
                            IPath jarPath = new Path(f.getAbsolutePath());
                            String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                            createLibClasspathEntry(node, jarPathString, true);
                        }
                    });

                    new DirVisitor(new File(platformFolder.getFolder(), "lib"), 3).visitRecursive(f -> {

                        if (f.getName().endsWith(".jar") && addedJars.add(f.getName())) {
                            IPath jarPath = new Path(f.getAbsolutePath());
                            String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                            createLibClasspathEntry(node, jarPathString, true);
                        }
                    });

                    new DirVisitor(new File(platformFolder.getFolder(), "tomcat"), 3).visitRecursive(f -> {
                        if (f.getName().endsWith(".jar") && addedJars.add(f.getName())) {
                            IPath jarPath = new Path(f.getAbsolutePath());
                            String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                            createLibClasspathEntry(node, jarPathString, true);
                        }
                    });

                    // platform/ext extensions as libraries
                    new DirVisitor(new File(platformFolder.getFolder(), "ext"), 2).visitRecursive(f0 -> {
                        if (f0.isDirectory() && new File(f0, "extensioninfo.xml").isFile()) {
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
                    });

                    // bin/ext- extensions as libraries
                    if (ImportOption.CUSTOM_ONLY.is()) {
                        new DirVisitor(new File(platformFolder.getFolder().getParent()), 0).visit(ext -> {
                            if (ext.getName().startsWith("ext-")) {
                                new DirVisitor(ext, 5).visitRecursive(f -> {
                                    if (f.getName().endsWith(".jar") && addedJars.add(f.getName())) {
                                        IPath jarPath = new Path(f.getAbsolutePath());
                                        String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                        createLibClasspathEntry(node, jarPathString, true);
                                    } else if (f.isDirectory() && new File(f, "extensioninfo.xml").isFile()) {
                                        if (new File(f, "classes").isDirectory() && addedJars.add(f.getAbsolutePath() + "/classes")) {
                                            IPath jarPath = new Path(f.getAbsolutePath() + "/classes");
                                            String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                            createLibClasspathEntry(node, jarPathString, true);
                                        }
                                        if (new File(f, "web/webroot/WEB-INF/classes").isDirectory()
                                                && addedJars.add(f.getAbsolutePath() + "/web/webroot/WEB-INF/classes")) {
                                            IPath jarPath = new Path(f.getAbsolutePath() + "/web/webroot/WEB-INF/classes");
                                            String jarPathString = jarPath.makeRelativeTo(platformPath).toPortableString();
                                            createLibClasspathEntry(node, jarPathString, true);
                                        }
                                    }
                                });
                            }

                        });
                    }

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
