package hybris.handlers;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.MessageConsole;

import hybris.messages.Messages;

public class RunServerHandler extends AbstractHandler {

    protected List<String> getCommands(IPath command) {
        return Arrays.asList(command.toOSString());
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();

        final IProject platformProject = workspace.getRoot().getProject("platform");

        if (platformProject.exists()) {

            Job job = new Job(Messages.WizardProjectsImportPage_runningAntAll) {
                @Override
                public IStatus run(IProgressMonitor monitor) {
                    try {

                        IPath path = platformProject.getLocation().makeAbsolute().append("/hybrisserver.bat");
                        List<String> commands = getCommands(path);
                        IOConsole console = new MessageConsole("Hybris server", null);
                        console.activate();
                        ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });
                        Process p = new ProcessBuilder(commands).directory(platformProject.getLocation().toFile()).redirectErrorStream(true)
                                .start();
                        OutputStream out = console.newOutputStream();
                        byte[] data = new byte[1024];
                        int r;
                        while ((r = p.getInputStream().read(data)) > -1) {
                            if (monitor.isCanceled()) {
                                p.destroyForcibly();
                                throw new InterruptedException();
                            }
                            out.write(data, 0, r);
                        }

                        monitor.beginTask(Messages.WizardProjectsImportPage_runningAntAll, 1);
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
