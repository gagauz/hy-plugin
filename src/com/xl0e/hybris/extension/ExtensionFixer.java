package com.xl0e.hybris.extension;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import com.xl0e.hybris.classpath.CustomClasspathFixer;
import com.xl0e.hybris.classpath.PlatformClasspathFixer;
import com.xl0e.hybris.messages.Messages;
import com.xl0e.hybris.utils.XmlManipulator;

public class ExtensionFixer {

    private static final Set<String> allowedNatures = new HashSet<>(Arrays.asList(
            "org.jspresso.contrib.sjsplugin.spock.nature",
            "org.eclipse.jdt.core.javanature"));

    private static final Set<String> allowedBuilders = new HashSet<>(Arrays.asList(
            "org.eclipse.jdt.core.javabuilder",
            "org.eclipse.jdt.core.javanature"

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
            setupProject(monitor);
            setupSettings(monitor);
            setupClasspath(monitor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupClasspath(final IProgressMonitor monitor) throws Exception {
        if (extension.isPlatform()) {
            monitor.setTaskName("Fix classpath" + extension.getName());
            new PlatformClasspathFixer(extension).fixClasspath();
        } else {
            monitor.setTaskName("Fix classpath for " + extension.getName());
            new CustomClasspathFixer(extension).fixClasspath();
        }
        monitor.worked(1);
    }

    private void setupSettings(final IProgressMonitor monitor) throws Exception {
        if (!extension.getSettings().isDirectory()) {
            monitor.setTaskName("Create settings");
            extension.getSettings().mkdir();
        } else {
            monitor.setTaskName("Clear project specific settings");
            EclipseSettingsCleaner.cleanupPrefs(extension);
        }
        monitor.worked(1);
    }

    private void setupProject(final IProgressMonitor monitor) throws Exception {
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
                if (!extension.isCustom()
                        || !allowedBuilders.contains(node.getTextContent().trim())) {
                    node.getParentNode().getParentNode().removeChild(node.getParentNode());
                }
            }
        });
        projectXml.saveDocument();

        monitor.worked(1);
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
