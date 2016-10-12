package hybristools;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hybristools.utils.XmlUtils;

public class CustomClasspathFixer extends ClasspathTools {

    private static final Set<String> workspaceProject = new HashSet<>();
    private final Extension extension;

    public CustomClasspathFixer(Extension extension) {
        this.extension = extension;
        workspaceProject.clear();
    }

    public void fixClasspath() {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProject[] projects = workspace.getRoot().getProjects();

        for (IProject project : projects) {
            workspaceProject.add(project.getName());
        }

        if (null == ImportOption.currentOption) {

            final Set<IProject> workspaceProject = new HashSet<>(projects.length);
            ImportOption.currentOption = ImportOption.CUSTOM_ONLY;
            workspaceProject.addAll(Arrays.asList(projects));
            workspaceProject.forEach(project -> {
                if (project.getLocation().toPortableString().contains("/platform/ext/")) {
                    ImportOption.currentOption = ImportOption.ALL;
                } else if (!ImportOption.ALL.is() && project.getLocation().toPortableString().contains("/bin/ext-")) {
                    ImportOption.currentOption = ImportOption.BIN_NOT_PLATFORM;
                }
            });

        }

        try {
            System.out.println("Fix classpath for " + extension.getFolder());
            if (!extension.getClasspath().isFile()) {
                extension.getClasspath().createNewFile();
                InputStream is = getClass().getResourceAsStream("/tpl/empty_classpath.xml");
                Files.copy(is, extension.getClasspath().toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            Document doc = XmlUtils.readXmlFile(extension.getClasspath());
            NodeList nodes = doc.getElementsByTagName("classpath");
            for (int i = 0; i < nodes.getLength();) {
                Node node = nodes.item(i);

                while (node.hasChildNodes())
                    node.removeChild(node.getFirstChild());

                createElement(node, "classpathentry", "kind", "output", "path", "eclipsebin/notused");
                createElement(node, "classpathentry", "kind", "con", "path", "org.eclipse.jdt.launching.JRE_CONTAINER");
                if (!extension.isConfig() && !extension.isPlatform() && !extension.isPlatformExt()) {
                    createElement(node, "classpathentry", "kind", "src", "path", "/platform");
                }
                final Set<String> dublicateChecker = new HashSet<>();
                dublicateChecker.add("zul-8.0.0.jar");

                if (extension.isCustom()) {
                    createMultipleSrcFolder(extension, node, new String[] { "src", "gensrc", "hmc/src", "backoffice/src" }, "classes",
                            true);
                    createMultipleSrcFolder(extension, node, new String[] { "web/src", "web/gensrc" }, "web/webroot/WEB-INF/classes", true);
                    createMultipleSrcFolder(extension, node, new String[] { "testsrc", "hmc/testsrc", "backoffice/testsrc" }, "testclasses",
                            false, "*.java");
                    createSrcFolder(extension, node, "web/testsrc", "web/webroot/WEB-INF/testclasses", false, "*.java");

                    if (extension.hasFolder("resources")) {
                        createLibClasspathEntry(node, "resources", true);
                    }

                    if (extension.hasFolder("commonweb/testsrc")) {
                        createSrcClasspathEntry(node, "commonweb/testsrc", "commonweb/webroot/WEB-INF/testclasses", true);
                    }
                    if (extension.hasFolder("commonweb/src")) {
                        createSrcClasspathEntry(node, "commonweb/src", "commonweb/webroot/WEB-INF/classes", true);
                    }
                    visitFolder(extension.getFolder(), "web/commonwebsrc", folder -> {
                        System.out.println("register addon " + folder.getName());
                        createSrcClasspathEntry(node, "web/commonwebsrc/" + folder.getName(), "web/webroot/WEB-INF/classes", true);
                    });
                } else {
                    if (extension.hasFolder("web/webroot/WEB-INF/classes")) {
                        createLibClasspathEntry(node, "web/webroot/WEB-INF/classes", "web/src", true);
                    }
                    if (extension.hasFolder("classes")) {
                        createLibClasspathEntry(node, "classes", "src", true);
                    }
                    if (extension.hasFolder("hmc/classes")) {
                        createLibClasspathEntry(node, "hmc/classes", "hmc/src", true);
                    }
                }

                visitFolder(extension.getFolder(), "web/webroot/WEB-INF/lib", lib -> {
                    if (lib.getName().endsWith(".jar")) {
                        if (dublicateChecker.add(lib.getName())) {
                            createLibClasspathEntry(node, "web/webroot/WEB-INF/lib/" + lib.getName(), true);
                        }
                    }
                });

                new DirVisitor(extension.getFolder(), 4).visitRecursive(lib -> {
                    if (lib.getName().endsWith(".jar") && !lib.getName().endsWith("-sources.jar")) {
                        if (dublicateChecker.add(lib.getName())) {
                            Path path = extension.getFolder().toPath().relativize(lib.toPath());
                            createLibClasspathEntry(node, path.toString(), true);
                        }
                    }
                });

                final Set<String> dependentExtension = new HashSet<>();
                dependentExtension.add("zul-8.0.0.jar");
                Consumer<Extension> depfetcher = getDependencyFetcher(extension.getFolder().toPath(), dependentExtension, node);
                extension.getRequiredExtensions().forEach(depfetcher);
                break;
            }
            XmlUtils.saveXmlDocument(doc, extension.getClasspath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Consumer<Extension> getDependencyFetcher(final Path extensionPath, final Set<String> dependentExtension,
            final Node node) {
        return requiredExt -> {
            if (dependentExtension.add(requiredExt.getFolder().getAbsolutePath())) {
                if (ImportOption.ALL.is() || (ImportOption.CUSTOM_ONLY.is() && requiredExt.isCustom())
                        || (ImportOption.BIN_NOT_PLATFORM.is() && requiredExt.isPlatformExt())) {
                    createElement(node, "classpathentry", "kind", "src", "path", "/" + requiredExt.getName(), "exported", "true");
                }
                // if (workspaceProject.contains(requiredExt.getName())) {
                // createElement(node, "classpathentry", "kind", "src", "path",
                // "/" + requiredExt.getName(), "exported", "true");
                // } else {
                // new DirVisitor(requiredExt.getFolder(),
                // 4).visitRecursive(file -> {
                // if (dependentExtension.add(file.getName())) {
                // if (file.getName().endsWith(".jar")) {
                // System.out.println("register jar " + file.getName());
                // Path path = extensionPath.relativize(file.toPath());
                // createLibClasspathEntry(node, path.toString(), true);
                // } else if (file.getName().equals("classes")) {
                //
                // if (requiredExt.hasFolder("src") ||
                // requiredExt.hasFolder("web/src")) {
                //
                // System.out.println("register " + file.getName());
                // Path pathClasses = extensionPath.relativize(file.toPath());
                // Path pathSrc =
                // extensionPath.relativize(requiredExt.getFileInFolder("src").toPath());
                // createElement(node, "classpathentry", "kind", "lib", "path",
                // pathClasses.toString(), "exported",
                // "true", "sourcepath", pathSrc.toString());
                // } else {
                // System.out.println("register " + file.getName());
                // Path path = extensionPath.relativize(file.toPath());
                // createLibClasspathEntry(node, path.toString(), true);
                // }
                // }
                // }
                // });
                // requiredExt.getRequiredExtensions().forEach(getDependencyFetcher(extensionPath,
                // dependentExtension, node));
                // }
                // new DirVisitor(requiredExt.getFolder(),
                // 4).visitRecursive(file -> {
                // if (dependentExtension.add(file.getName())) {
                // if (file.getName().endsWith(".jar")) {
                // System.out.println("register jar " + file.getName());
                // Path path = extensionPath.relativize(file.toPath());
                // createLibClasspathEntry(node, path.toString(), true);
                // } else if (file.getName().equals("classes")) {
                // System.out.println("register " + file.getName());
                // Path path = extensionPath.relativize(file.toPath());
                // createLibClasspathEntry(node, path.toString(), true);
                // }
                // }
                // });
                //
                // requiredExt.getRequiredExtensions().forEach(getDependencyFetcher(extensionPath,
                // dependentExtension, node));
            }
        };
    }

    public static boolean hasFolder(File parent, String file) {
        return new File(parent, file).isDirectory();
    }

    public static void visitFolder(File parent, String file, Consumer<File> c) {
        File folder = new File(parent, file);
        if (folder.isDirectory()) {
            new DirVisitor(folder, 1).visit(c);
        }
    }
}
