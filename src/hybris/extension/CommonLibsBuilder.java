package hybris.extension;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import hybris.ant.DirVisitor;

public class CommonLibsBuilder {
    private final File platformHome;
    private final Set<String> platformLibs = new HashSet<>();

    public CommonLibsBuilder(File platformHome, IClasspathEntry[] platformLibs) {
        this.platformHome = platformHome;
        Arrays.asList(platformLibs).forEach(classapth -> {
            this.platformLibs.add(classapth.getPath().lastSegment());
        });

    }

    public void build(final IProgressMonitor monitor) throws InterruptedException {
        final Set<IClasspathEntry> classpaths = new LinkedHashSet<>();
        monitor.beginTask("Fetching libs from required extensions", ExtensionResolver.getAllExtensions(platformHome).size());
        final IPath platformBin = new Path(platformHome.getParentFile().getAbsolutePath());
        try {
            ExtensionResolver.getAllExtensions(platformHome).forEach((name, ext) -> {
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    throw new RuntimeException();
                }
                File folder = ext.getFolder();
                IPath ext0 = new Path(ext.getFolder().getAbsolutePath());
                ext0 = ext0.makeRelativeTo(platformBin);

                if (ext0.segment(0).equals("custom") || ext0.segment(0).equals("platform")) {
                    return;
                }

                monitor.setTaskName("Fetching libs from " + name);

                final Set<String> addedJars = new HashSet<>(100);

                File classes = ext.getFileInFolder("classes");
                if (classes.exists()) {
                    classpaths.add(JavaCore.newLibraryEntry(new Path(classes.getAbsolutePath()), null, null, true));
                }
                File resources = ext.getFileInFolder("resources");
                if (resources.exists()) {
                    classpaths.add(JavaCore.newLibraryEntry(new Path(resources.getAbsolutePath()), null, null, true));
                }

                new DirVisitor(folder, 4).visitRecursive(f -> {
                    if (monitor.isCanceled()) {
                        throw new RuntimeException();
                    }
                    if (f.getName().endsWith(".jar") && addedJars.add(f.getName()) && platformLibs.add(f.getName())) {
                        IPath jarPath = new Path(f.getAbsolutePath());
                        classpaths.add(JavaCore.newLibraryEntry(jarPath, null, null, true));
                    }
                });
            });

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new InterruptedException();
        }
    }
}
