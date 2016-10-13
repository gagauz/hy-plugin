package hybris.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import hybris.messages.Messages;
import hybristools.AntCommand;

public class AntAllHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();

        final IProject platformProject = workspace.getRoot().getProject("platform");

        if (platformProject.exists()) {

            Job job = new Job(Messages.WizardProjectsImportPage_runningAntAll) {
                @Override
                public IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask(Messages.WizardProjectsImportPage_runningAntAll, 1);
                        if (!monitor.isCanceled()) {
                            new AntCommand(platformProject.getLocation(), "clean", "all")
                                    .accept(platformProject.getLocation().toFile(), monitor);
                            monitor.worked(1);
                        } else {
                            return Status.CANCEL_STATUS;
                        }
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return new Status(IStatus.ERROR, "hybris", e.getMessage(), e);
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
