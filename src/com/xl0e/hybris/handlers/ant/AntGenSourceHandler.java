package com.xl0e.hybris.handlers.ant;

import java.util.Arrays;
import java.util.List;

import com.xl0e.hybris.messages.Messages;

public class AntGenSourceHandler extends AntBuildExtensionHandler {

    @Override
    protected String getMessagePattern() {
        return Messages.AntCommand_GensourceFor;
    }

    @Override
    protected List<String> getArguments() {
        return Arrays.asList("gensource");
    }
}
