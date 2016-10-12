package hybristools;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ExtensionResolver {

    public static final Map<String, Extension> CACHE = new HashMap<>();

    static Consumer<File> getVisitor(File platformHome) {
        return v -> {
            if (!v.isDirectory()) {
                return;
            }
            if (new File(v, Constants.EXTENSIONINFO_FILE_NAME).isFile()) {
                Extension e = new Extension(v, platformHome);
                System.out.println("Add extension " + e.getName());
                CACHE.putIfAbsent(e.getName(), e);
                return;
            }
        };
    }

    static void init(File platformFolder) {
        File platformBinDir = platformFolder.getParentFile();
        new DirVisitor(platformBinDir, 5).visitRecursive(getVisitor(platformFolder));
    }

    public static Extension findExtension(File platformFolder, String name) {
        if (CACHE.isEmpty()) {
            init(platformFolder);
        }
        return CACHE.get(name);
    }

    public static Map<String, Extension> getAllExtensions(File platformFolder) {
        if (CACHE.isEmpty()) {
            init(platformFolder);
        }
        return CACHE;
    }

    public static void clearCache() {
        CACHE.clear();
    }
}