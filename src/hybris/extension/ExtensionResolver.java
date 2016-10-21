package hybris.extension;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import hybris.Constants;
import hybris.ant.DirVisitor;

public class ExtensionResolver {

    public static final Map<String, Extension> CACHE = new HashMap<>();

    static Consumer<File> getVisitor(File platformHome) {
        return v -> {
            if (!v.isDirectory()) {
                return;
            }
            if (new File(v, Constants.EXTENSIONINFO_FILE_NAME).isFile()) {
                Extension e = Extension.create(v, platformHome);
                if (null != e) {
                    System.out.println("Add extension " + e.getName());
                    CACHE.putIfAbsent(e.getName(), e);
                }
                return;
            }
        };
    }

    static void init(File platformFolder, Consumer<File> consumer) {
        File platformBinDir = platformFolder.getParentFile();
        Consumer<File> fileConsumer = getVisitor(platformFolder);
        if (null != consumer) {
            fileConsumer = fileConsumer.andThen(consumer);
        }
        new DirVisitor(platformBinDir, 5).visitRecursive(fileConsumer);
    }

    public static Extension findExtension(File platformFolder, String name) {
        if (CACHE.isEmpty()) {
            init(platformFolder, null);
        }
        return CACHE.get(name);
    }

    public static Map<String, Extension> getAllExtensions(File platformFolder) {
        if (CACHE.isEmpty()) {
            init(platformFolder, null);
        }
        return CACHE;
    }

    public static Map<String, Extension> getAllExtensions(File platformFolder, Consumer<File> consumer) {
        if (CACHE.isEmpty()) {
            init(platformFolder, consumer);
        }
        return CACHE;
    }

    public static Extension getConfig(File platformFolder) {
        if (CACHE.isEmpty()) {
            init(platformFolder, null);
        }
        return CACHE.get("config");
    }

    public static void clearCache() {
        CACHE.clear();
    }
}