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
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

import hybristools.AntCommand;

public class AntBuildExtensionHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();

        final IProject platforProject = workspace.getRoot().getProject("platform");

        if (platforProject.exists()) {

            Job job = new Job("Ant build") {
                @Override
                public IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask("Ant build", 1);
                        if (!monitor.isCanceled()) {
                            new AntCommand(platforProject.getLocation(), "clean", "build")
                                    .accept(platforProject.getLocation().toFile(), monitor);
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

    private MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++)
            if (name.equals(existing[i].getName()))
                return (MessageConsole) existing[i];
        // no console found, so create a new one
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }
}
