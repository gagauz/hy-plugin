package com.xl0e.hybris.handlers.ant;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.IJavaProject;

import com.xl0e.hybris.messages.Messages;

public class AntAllHandler extends AntBuildExtensionHandler {

    @Override
    protected String getMessagePattern() {
        return Messages.AntCommand_CleanAllFor;
    }

    @Override
    protected List<String> getArguments() {
        return Arrays.asList("clean", "all");
    }

    @Override
    protected IJavaProject getTargetProject(ExecutionEvent event) {
        return getPlatformProject();
    }
}
