package hybris.handlers.ant;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.IJavaProject;

import hybris.messages.Messages;

public class AntGenSourceHandler extends AntBuildExtensionHandler {

    public AntGenSourceHandler() {
        super(Messages.AntCommand_GensourceFor);
    }

    @Override
    protected List<String> getArguments() {
        return Arrays.asList("gensource");
    }

    @Override
    protected IJavaProject getTarget(ExecutionEvent event) {
        return super.getTarget(event);
    }
}
