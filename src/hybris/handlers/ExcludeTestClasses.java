package hybris.handlers;

public class ExcludeTestClasses extends AbstractClasspathFixerHandler {

    @Override
    protected boolean isExcludeTestClasses() {
        return true;
    }

    @Override
    protected boolean isDisableAntBuilder() {
        return true;
    }

}
