package com.xl0e.hybris.handlers;

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
