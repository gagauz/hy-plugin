package com.xl0e.hybris.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.xl0e.hybris.forms.ManageExtensionsForm;

public class OpenWindowHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProject platformProject = workspace.getRoot().getProject("platform");

        IWorkbenchWindow activeWindow = HandlerUtil.getActiveWorkbenchWindowChecked(event);

        try {

            Shell parent = activeWindow.getShell();
            ManageExtensionsForm dialog = new ManageExtensionsForm(parent);
            dialog.create();
            dialog.open();

        } catch (Exception ex) {
            throw new ExecutionException("error creating wizard", ex); //$NON-NLS-1$
        }

        return null;
    }
}
