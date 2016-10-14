package hybristools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.internal.ui.views.console.ProcessConsoleManager;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class AntCommand implements BiConsumer<File, IProgressMonitor> {

    private static final Map<String, Integer> estimatedWork = new HashMap<>();
    private static ProcessConsoleManager consoleManager = new ProcessConsoleManager();

    private final IPath platformPath;
    private final List<String> commands;

    public AntCommand(IPath platforPath, List<String> commands) {
        this.platformPath = platforPath;
        this.commands = commands;
    }

    @Override
    public void accept(File ext, IProgressMonitor monitor) {
        try {
            List<String> processCommands = new ArrayList<>();
            IPath path = platformPath.makeAbsolute().append("/apache-ant-1.9.1/bin/ant.bat");
            processCommands.add(path.toOSString());
            processCommands.addAll(commands);
            final String commandName = path.lastSegment() + ' ' + String.join(" ", commands);
            int estimated = estimatedWork.getOrDefault(commandName, 100);
            SubMonitor subMonitor = SubMonitor.convert(monitor, "Running " + commandName + " ...", estimated);

            ISourceLookupDirector sourceLocator = new JavaSourceLookupDirector();
            sourceLocator.setSourcePathComputer(DebugPlugin.getDefault().getLaunchManager()
                    .getSourcePathComputer("org.eclipse.jdt.launching.sourceLookup.javaSourcePathComputer"));
            ILaunch launch = new Launch(null, ILaunchManager.DEBUG_MODE, sourceLocator);

            Process antProccess = new ProcessBuilder(processCommands).directory(ext).start();
            IProcess procc = new RuntimeProcess(launch, antProccess, commandName, null);
            procc.setAttribute(IProcess.ATTR_PROCESS_TYPE, IJavaLaunchConfigurationConstants.ID_JAVA_PROCESS_TYPE);
            consoleManager.launchAdded(launch);
            subMonitor.done();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
    }

}
