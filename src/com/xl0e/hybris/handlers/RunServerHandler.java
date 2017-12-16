package com.xl0e.hybris.handlers;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import com.xl0e.hybris.ant.AbstractCommand;
import com.xl0e.hybris.messages.Messages;

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
                        String timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                                .format(new Date(System.currentTimeMillis()));
                        new RunServerCommand(platformProject.getLocation(), Collections.emptyList(), new String[] {
                                DebugPlugin.ATTR_PATH,
                                platformProject.getLocation().toOSString(),
                                IProcess.ATTR_CMDLINE,
                                DebugPlugin.renderArguments(new String[0], null),
                                DebugPlugin.ATTR_LAUNCH_TIMESTAMP,
                                timestamp,
                                DebugPlugin.ATTR_WORKING_DIRECTORY,
                                platformProject.getLocation().toFile().getAbsolutePath(),
                                IProcess.ATTR_PROCESS_TYPE,
                                IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION
                        }).accept(platformProject.getLocation().toFile(), monitor);

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

    private static class RunServerCommand extends AbstractCommand {

        public RunServerCommand(IPath platformPath, List<String> arguments, String[] attrNameAndValues) {
            super(platformPath, arguments, attrNameAndValues);
        }

        @Override
        protected String getGroupName() {
            return "run com.xl0e.hybris server";
        }

        @Override
        protected String getApplicationPath() {
            final IPath path = platformPath.append("/hybrisserver." + getScriptExtension()).makeAbsolute();
            return path.toOSString();
        }

        @Override
        protected ILaunch createLaunch(String string) {
            return new Launch(null, ILaunchManager.RUN_MODE, getSourceLocator());
        }
    };
}
