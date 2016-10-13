package hybris.handlers;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;

public class DebugServerHandler extends RunServerHandler {
    @Override
    protected List<String> getCommands(IPath command) {
        return Arrays.asList(command.toOSString(), "debug");
    }
}
