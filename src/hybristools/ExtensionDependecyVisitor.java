package hybristools;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ExtensionDependecyVisitor {

    static Set<String> VISITOR_CACHE = new HashSet<>();
    final Extension extension;

    public ExtensionDependecyVisitor(Extension extension) {
        this.extension = extension;
    }

    public void visit(final Predicate<Extension> handler) {
        System.out.println("Visiting required extensions of " + extension.getName());
        if (VISITOR_CACHE.add(extension.getFolder().getAbsolutePath())) {
            try {
                extension.getRequiredExtensions().forEach(ext -> {
                    if (null != handler) {
                        handler.test(ext);
                    }
                    new ExtensionDependecyVisitor(ext).visit(handler);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void reset() {
        VISITOR_CACHE.clear();
        VISITOR_CACHE.add("core");
    }

    public static boolean hasExtension(String extName) {
        return VISITOR_CACHE.contains(extName);
    }
}