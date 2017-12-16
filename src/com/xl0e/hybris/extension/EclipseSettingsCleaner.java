package com.xl0e.hybris.extension;

import java.io.File;
import java.util.function.Consumer;

import com.xl0e.hybris.Constants;

public class EclipseSettingsCleaner {

    public static final Consumer<Extension> CONSUMER = EclipseSettingsCleaner::cleanupPrefs;

    public static void cleanupPrefs(Extension extension) {
        if (!extension.getSettings().isDirectory()) {
            return;
        }
        for (File f : extension.getSettings().listFiles()) {
            if (f.getName().equals(Constants.Files.org_eclipse_jdt_core_prefs)
                    || f.getName().equals(Constants.Files.org_eclipse_jdt_ui_prefs)) {
                f.delete();
            }
        }
    }
}
