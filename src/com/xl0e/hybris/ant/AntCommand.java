package com.xl0e.hybris.ant;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;

public class AntCommand extends AbstractCommand {

    public AntCommand(IPath platforPath, List<String> commands, String... attrNameAndValues) {
        super(platforPath, commands, attrNameAndValues);
    }

    private static void attachStreamListener(final IProcess process, final SubMonitor subMonitor) {
        final IStreamListener listener = new IStreamListener() {
            @Override
            public void streamAppended(String text, IStreamMonitor monitor) {
                if (subMonitor.isCanceled() && process.canTerminate()) {
                    monitor.removeListener(this);
                    try {
                        process.terminate();
                    } catch (DebugException e) {
                        e.printStackTrace();
                    }
                    subMonitor.done();
                    return;
                }
                subMonitor.worked(1);
                subMonitor.setTaskName(trim(text, 1000));
            }
        };
        process.getStreamsProxy().getOutputStreamMonitor().addListener(listener);
    }

    private static String trim(String string, int toLength) {
        if (null != string && string.length() > toLength) {
            return string.substring(0, toLength) + "...";
        }
        return string;
    }

    @Override
    protected String getGroupName() {
        return "ant";
    }

    @Override
    protected ILaunch createLaunch(String string) {
        return new Launch(null, ILaunchManager.RUN_MODE, getSourceLocator());
    }

    @Override
    protected String getApplicationPath() {
        final IPath path = platformPath.append("/apache-ant-1.9.1/bin/ant." + getScriptExtension()).makeAbsolute();
        return path.toOSString();
    }

}
