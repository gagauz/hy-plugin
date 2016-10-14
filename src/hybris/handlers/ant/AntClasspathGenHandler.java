package hybris.handlers.ant;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;

public class AntClasspathGenHandler extends AbstractAntHandler {

    @Override
    protected List<String> getArguments() {
        return Arrays.asList("classpathgen");
    }

    @Override
    protected IProject getTarget(ExecutionEvent event) {
        return super.getTarget(event);
    }
}
