package com.xl0e.hybris.extension;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModelManager;

import com.xl0e.hybris.Constants;

public class CommonLibsBuilder {
    private final File platformHome;

    public CommonLibsBuilder(File platformHome) {
        this.platformHome = platformHome;
    }

    public void build(final IProgressMonitor monitor) throws InterruptedException {
        final Set<IClasspathEntry> classpaths = new LinkedHashSet<>();
        monitor.beginTask("Fetching libs from required extensions", ExtensionResolver.getAllExtensions(platformHome).size());
        try {
            new LocalExtensionVisitor(platformHome).visit(extension -> {
                if (extension.isCustom()) {
                    classpaths.add(
                            JavaCore.newLibraryEntry(new Path(extension.getFileInRamFolder("classes").getAbsolutePath()), null, null,
                                    true));
                }
            });
            JavaModelManager.getUserLibraryManager().setUserLibrary(Constants.CUSTOM_PROJECT_CP_LIB,
                    classpaths.toArray(new IClasspathEntry[classpaths.size()]), false);

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new InterruptedException();
        }
    }
}
