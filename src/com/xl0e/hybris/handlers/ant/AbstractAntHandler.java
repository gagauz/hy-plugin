package com.xl0e.hybris.handlers.ant;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.xl0e.hybris.Constants;
import com.xl0e.hybris.ant.AntCommand;

public abstract class AbstractAntHandler extends AbstractHandler {

    protected abstract List<String> getArguments();

    protected abstract String createMessage(ExecutionEvent event);

    protected abstract IJavaProject getTargetProject(ExecutionEvent event);

    protected IJavaProject getPlatformProject() {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(Constants.Files.Hybris.platform);
        try {
            if (project.hasNature(JavaCore.NATURE_ID)) {
                return JavaCore.create(project);
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        IJavaProject targetProject = getTargetProject(event);

        if (null != targetProject && targetProject.exists()) {

            final String message = createMessage(event);

            Job job = new Job(message) {
                @Override
                public IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask(message, 1);
                        if (!monitor.isCanceled()) {
                            new AntCommand(getPlatformProject().getProject().getLocation(), getArguments())
                                    .accept(targetProject.getProject().getLocation().toFile(), monitor);
                            monitor.worked(1);
                        } else {
                            return Status.CANCEL_STATUS;
                        }
                        if (!monitor.isCanceled()) {
                            ResourcesPlugin.getWorkspace().getRoot().refreshLocal(3, monitor);
                        }
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return new Status(IStatus.ERROR, "com.xl0e.hybris", e.getMessage(), e);
                    } finally {
                        monitor.done();
                    }
                }
            };
            job.setRule(ResourcesPlugin.getWorkspace().getRoot());
            job.schedule();
        }
        return null;
    }
}
