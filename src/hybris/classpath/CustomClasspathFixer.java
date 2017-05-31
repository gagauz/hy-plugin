package hybris.classpath;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import hybris.ant.DirVisitor;
import hybris.ant.ImportOption;
import hybris.extension.ClasspathTools;
import hybris.extension.Extension;
import hybristools.utils.FileUtiles;
import hybristools.utils.XmlManipulator;
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
                    createElement(node, "classpathentry", "kind", "src", "path", "/platform", "exported", "true");
                }

                if (extension.isCustom()) {
                    //                    final Path outputClasses = Paths.get("eclipsebin/classes");
                    //                    final Path outputTestClasses = Paths.get("eclipsebin/testclasses");
                    //                    final Path outputWebClasses = outputClasses;
                    //                    final Path outputWebTestClasses = outputTestClasses;

                    final Path outputClasses = Paths.get("classes");
                    final Path outputTestClasses = Paths.get("testclasses");
                    final Path outputWebClasses = Paths.get("web/webroot/WEB-INF/classes");
                    final Path outputWebTestClasses = Paths.get("web/testclasses");

                    final XmlManipulator projectXml = new XmlManipulator(extension.getProject());

                    File ramDir = new File("R:\\" + extension.getName());
                    if (!ramDir.exists()) {
                        ramDir.mkdir();
                    }
                    File classesDir = new File(ramDir, "classes");
                    File testClassesDir = new File(ramDir, "testclasses");

                    if (!classesDir.exists()) {
                        classesDir.mkdir();
                    }
                    if (!testClassesDir.exists()) {
                        testClassesDir.mkdir();
                    }

                    FileUtiles.removeFolder(new File(extension.getFolder(), "classes"));
                    FileUtiles.removeFolder(new File(extension.getFolder(), "testclasses"));
                    FileUtiles.removeFolder(new File(extension.getFolder(), "eclipsebin"));
                    FileUtiles.removeFolder(new File(extension.getFolder(), "web/webroot/WEB-INF/classes"));
                    FileUtiles.removeFolder(new File(extension.getFolder(), "web/webroot/WEB-INF/testclasses"));
                    FileUtiles.removeFolder(new File(extension.getFolder(), "web/testclasses"));

                    //                    projectXml.setNodeValue("projectDescription/linkedResources/link/name", "eclipsebin");
                    //                    projectXml.setNodeValue("projectDescription/linkedResources/link/type", "2");
                    //                    projectXml.setNodeValue("projectDescription/linkedResources/link/location",
                    //                            new org.eclipse.core.runtime.Path(ramDir.getAbsolutePath()).toPortableString());
                    //                    projectXml.saveDocument();

                    createMultipleSrcFolder(extension, node, new String[] { "src", "gensrc", "hmc/src", "backoffice/src" },
                            outputClasses.toString(),
                            true);
                    createMultipleSrcFolder(extension, node,
                            new String[] { "web/src", "web/gensrc", "web/commonwebsrc/*", "web/addonsrc/*",
                                    "acceleratoraddon/web/src" },
                            outputWebClasses.toString(), true);
                    createMultipleSrcFolder(extension, node, new String[] { "testsrc", "hmc/testsrc", "backoffice/testsrc" },
                            outputTestClasses.toString(),
                            false, "**/*.java");
                    createSrcFolder(extension, node, "web/testsrc", outputWebTestClasses.toString(), false, "**/*.java");

                    if (extension.hasFolder("resources")) {
                        createLibClasspathEntry(node, "resources", true);
                    }

                    if (extension.hasFolder("commonweb/testsrc")) {
                        createSrcClasspathEntry(node, "commonweb/testsrc", outputTestClasses.toString(), true);
                    }
                    if (extension.hasFolder("commonweb/src")) {
                        createSrcClasspathEntry(node, "commonweb/src", outputClasses.toString(), true);
                    }
                    final Set<String> dependentExtension = new HashSet<>();
                    Consumer<Extension> depfetcher = getDependencyFetcher(extension.getFolder().toPath(), dependentExtension, node);
                    extension.getRequiredExtensions().forEach(depfetcher);
                }

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
            if (requiredExt.isCustom()) {
                createElement(node, "classpathentry", "kind", "src", "path", "/" + requiredExt.getName(), "exported", "true");
            }
        };

    }

    public static void visitFolder(File parent, String file, Consumer<File> c) {
        File folder = new File(parent, file);
        if (folder.isDirectory()) {
            new DirVisitor(folder, 1).visit(c);
        }
    }
}
