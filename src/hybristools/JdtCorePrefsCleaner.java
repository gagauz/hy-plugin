package hybristools;

import java.io.File;
import java.util.function.Predicate;

public class JdtCorePrefsCleaner {

    public static final Predicate<Extension> EXT_PREFS_CLEANER = new Predicate<Extension>() {

        @Override
        public boolean test(Extension ext) {
            cleanupPrefs(ext);
            return false;
        }
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
