package hybris.handlers.ant;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import hybris.messages.Messages;
import hybristools.AntCommand;

public class AbstractAntHandler extends AbstractHandler {

    protected IProject getTarget(ExecutionEvent event) {
        return ResourcesPlugin.getWorkspace().getRoot().getProject("platform");
    }

    protected List<String> getArguments() {
        return Arrays.asList("clean", "all");
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        IProject targetProject = getTarget(event);

        if (targetProject.exists()) {

            Job job = new Job(Messages.WizardProjectsImportPage_runningAntAll) {
                @Override
                public IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask(Messages.WizardProjectsImportPage_runningAntAll, 1);
                        if (!monitor.isCanceled()) {
                            new AntCommand(targetProject.getLocation(), getArguments())
                                    .accept(targetProject.getLocation().toFile(), monitor);
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
