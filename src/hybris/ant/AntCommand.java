package hybris.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class AntCommand {
    private static final Map<String, Integer> estimatedWork = new HashMap<>();
    private static ProcessConsoleManager consoleManager = new ProcessConsoleManager();

    private final IPath platformPath;
    private final List<String> commands;
    private final String[] attrNameAndValues;

    public AntCommand(IPath platforPath, List<String> commands, String... attrNameAndValues) {
        this.platformPath = platforPath;
        this.commands = commands;
        this.attrNameAndValues = attrNameAndValues;
    }

    @SuppressWarnings({ "restriction", "unused" })
    public int accept(File ext, IProgressMonitor monitor) {
        try {
            List<String> processCommands = new ArrayList<>();
            IPath path = platformPath.append("/apache-ant-1.9.1/bin/ant.bat").makeAbsolute();
            processCommands.add(path.toFile().getAbsolutePath());
            processCommands.addAll(commands);
            final String commandName = path.lastSegment() + ' ' + String.join(" ", commands);
            int estimated = estimatedWork.getOrDefault(commandName, 100);
            SubMonitor subMonitor = SubMonitor.convert(monitor, "Running " + commandName + " ...", estimated);

            ISourceLookupDirector sourceLocator = new JavaSourceLookupDirector();
            sourceLocator.setSourcePathComputer(DebugPlugin.getDefault().getLaunchManager()
                    .getSourcePathComputer("org.eclipse.jdt.launching.sourceLookup.javaSourcePathComputer"));
            ILaunch launch = new Launch(null, ILaunchManager.DEBUG_MODE, sourceLocator);

            final List<String> envsList = new ArrayList<>();
            System.getenv().forEach((k, v) -> {
                envsList.add(k + '=' + v);
            });

            Process antProccess = Runtime.getRuntime().exec(processCommands.toArray(new String[processCommands.size()]),
                    envsList.toArray(new String[envsList.size()]),
                    ext);

            final Map<String, String> attributeMap = new HashMap<>();
            for (int i = 0; i < attrNameAndValues.length; i = i + 2) {
                attributeMap.put(attrNameAndValues[i], attrNameAndValues[i + 1]);
            }
            if (!attributeMap.containsKey(IProcess.ATTR_PROCESS_TYPE)) {
                attributeMap.put(IProcess.ATTR_PROCESS_TYPE, IJavaLaunchConfigurationConstants.ID_JAVA_PROCESS_TYPE);
            }

            IProcess procc = new RuntimeProcess(launch, antProccess, commandName, attributeMap);

            consoleManager.launchAdded(launch);
            int result = antProccess.waitFor();
            subMonitor.done();
            //consoleManager.launchRemoved(launch);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
        return -1;
    }

}
