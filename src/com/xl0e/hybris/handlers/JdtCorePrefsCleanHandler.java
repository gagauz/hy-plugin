package com.xl0e.hybris.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import com.xl0e.hybris.extension.EclipseSettingsCleaner;
import com.xl0e.hybris.extension.LocalExtensionVisitor;

public class JdtCorePrefsCleanHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProject platformProject = workspace.getRoot().getProject("platform");
        if (platformProject.exists()) {
            new LocalExtensionVisitor(platformProject.getLocation().toFile()).visit(EclipseSettingsCleaner.CONSUMER);
        }
        return null;
    }
}
