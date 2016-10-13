package hybristools;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.MessageConsole;

public class AntCommand implements BiConsumer<File, IProgressMonitor> {

    private static final Map<String, Integer> estimatedWork = new HashMap<>();

    private final IPath platformPath;
    private final String[] command;

    public AntCommand(IPath platforPath, String... command) {
        this.platformPath = platforPath;
        this.command = command;
    }

    @Override
    public void accept(File ext, IProgressMonitor monitor) {
        try {
            List<String> commands = new ArrayList<>();
            IPath path = platformPath.makeAbsolute().append("/apache-ant-1.9.1/bin/ant.bat");
            commands.add(path.toOSString());
            commands.addAll(Arrays.asList(command));
            final String commandName = path.lastSegment() + ' ' + String.join(" ", command);
            int estimated = estimatedWork.getOrDefault(commandName, 100);
            SubMonitor subMonitor = SubMonitor.convert(monitor, "Running " + commandName + " ...", estimated);
            IOConsole console = new MessageConsole(commandName, null);
            console.activate();
            ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });
            Process p = new ProcessBuilder(commands).directory(ext).redirectErrorStream(true).start();
            OutputStream out = console.newOutputStream();
            byte[] data = new byte[1024];
            int r, realWork = 0;
            while ((r = p.getInputStream().read(data)) > -1) {
                if (subMonitor.isCanceled()) {
                    p.destroyForcibly();
                    throw new InterruptedException();
                }
                out.write(data, 0, r);
                subMonitor.worked(1);
                realWork++;
            }

            int res = p.waitFor();

            estimatedWork.put(commandName, realWork);
            if (res == 0) {
            }
            subMonitor.done();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
        }
    }
}
