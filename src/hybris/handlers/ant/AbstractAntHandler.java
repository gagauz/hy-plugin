package hybris.handlers.ant;

import java.util.Arrays;
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

import hybris.ant.AntCommand;

public class AbstractAntHandler extends AbstractHandler {
    private final String message;

    public AbstractAntHandler(String message) {
        this.message = message;
    }

    protected IJavaProject getTarget(ExecutionEvent event) {
        return getPlatformProject();
    }

    protected IJavaProject getPlatformProject() {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("platform");
        try {
            if (project.hasNature(JavaCore.NATURE_ID)) {
                return JavaCore.create(project);
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected List<String> getArguments() {
        return Arrays.asList("clean", "all");
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        IJavaProject targetProject = getTarget(event);

        if (null != targetProject && targetProject.exists()) {

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
                        ResourcesPlugin.getWorkspace().getRoot().refreshLocal(3, monitor);
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
