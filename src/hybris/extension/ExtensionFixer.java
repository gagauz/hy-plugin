package hybris.extension;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import hybris.classpath.CustomClasspathFixer;
import hybris.classpath.PlatformClasspathFixer;
import hybris.messages.Messages;
import hybristools.utils.XmlManipulator;

public class ExtensionFixer {

    private static final Set<String> allowedNatures = new HashSet<>(Arrays.asList(
            "org.jspresso.contrib.sjsplugin.spock.nature",
            "org.zeroturnaround.eclipse.jrebelNature",
            "org.eclipse.jdt.core.javanature",
            "org.zeroturnaround.eclipse.remoting.remotingNature"

    ));

    private static final Set<String> allowedBuilders = new HashSet<>(Arrays.asList(
            "org.eclipse.jdt.core.javabuilder",
            "org.zeroturnaround.eclipse.rebelXmlBuilder",
            "org.eclipse.jdt.core.javanature",
            "org.zeroturnaround.eclipse.remoting.remotingBuilder"

    ));

    private final Extension extension;

    public ExtensionFixer(Extension extension) {
        this.extension = extension;
    }

    public void fix(IProgressMonitor monitor) {
        if (null == extension) {
            return;
        }

        try {
            XmlManipulator projectXml = new XmlManipulator(extension.getProject());
            monitor.beginTask(Messages.bind(Messages.WizardProjectsImportPage_fixExtension, extension.getName()), 6);
            if (!extension.getProject().isFile()) {
                monitor.setTaskName("Creating .project file for " + extension.getName());
                Files.copy(getClass().getResourceAsStream("/tpl/empty_project.xml"), extension.getProject().toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                try {
                    projectXml.setNodeValue("projectDescription/name", extension.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            monitor.worked(1);

            final Holder<Boolean> hasTomcatHolder = new Holder<>();
            hasTomcatHolder.set(false);

            monitor.setTaskName("Removeing natures");

            final Holder<Boolean> natureHolder = new Holder<>();
            natureHolder.set(false);

            projectXml.walkNodes("projectDescription/natures/nature", node -> {
                natureHolder.set(true);
                if (!allowedNatures.contains(node.getTextContent().trim())) {
                    node.getParentNode().removeChild(node);
                }
            });

            if (!natureHolder.get()) {
                projectXml.setNodeValue("projectDescription/natures/nature", "org.eclipse.jdt.core.javanature");
            }

            monitor.worked(1);

            monitor.setTaskName("Removing builders");

            projectXml.walkNodes("projectDescription/buildSpec/buildCommand/name", node -> {
                if (null != node.getTextContent()) {
                    if (!allowedBuilders.contains(node.getTextContent().trim())) {
                        node.getParentNode().getParentNode().removeChild(node.getParentNode());
                    }
                }
            });

            monitor.worked(1);

            if (!extension.getClasspath().isFile()) {
                monitor.setTaskName("Create classpath");
                Files.copy(getClass().getResourceAsStream("/tpl/empty_classpath.xml"), extension.getClasspath().toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            monitor.worked(1);
            if (!extension.getSettings().isDirectory()) {
                monitor.setTaskName("Create settings");
                extension.getSettings().mkdir();
            } else {
                monitor.setTaskName("Clear project specific settings");
                JdtCorePrefsCleaner.EXT_PREFS_CLEANER.accept(extension);
            }
            monitor.worked(1);

            if (extension.isPlatform()) {
                monitor.setTaskName("Fix classpath" + extension.getName());
                new PlatformClasspathFixer(extension).fixClasspath();
            } else {
                monitor.setTaskName("Fix classpath for " + extension.getName());
                new CustomClasspathFixer(extension).fixClasspath();
            }
            projectXml.saveDocument();
            monitor.worked(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Holder<T> {
        T value;

        public T get() {
            return this.value;
        }

        public void set(T value) {
            this.value = value;
        }
    }
}
