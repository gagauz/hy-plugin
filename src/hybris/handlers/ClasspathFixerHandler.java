package hybris.handlers;

public class ClasspathFixerHandler extends AbstractClasspathFixerHandler {

    @Override
    protected boolean isExcludeTestClasses() {
        return false;
    }

    @Override
    protected boolean isDisableAntBuilder() {
        return false;
    }
}
