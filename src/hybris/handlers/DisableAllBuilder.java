package hybris.handlers;

public class DisableAllBuilder extends AbstractClasspathFixerHandler {

    @Override
    protected boolean isExcludeTestClasses() {
        return false;
    }

    @Override
    protected boolean isDisableAntBuilder() {
        return true;
    }

}
