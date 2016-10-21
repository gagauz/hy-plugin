package hybris.extension;

import java.io.File;
import java.util.function.Consumer;

public class JdtCorePrefsCleaner {

    public static final Consumer<Extension> EXT_PREFS_CLEANER = ext -> {
        cleanupPrefs(ext);
    };

    private static void cleanupPrefs(Extension extension) {
        if (!extension.getSettings().isDirectory()) {
            return;
        }
        for (File f : extension.getSettings().listFiles()) {
            if (f.getName().equals("org.eclipse.jdt.core.prefs") || f.getName().equals("org.eclipse.jdt.ui.prefs")) {
                System.out.println("Cleaning up " + f);
                f.delete();
            }
        }
    }
}
