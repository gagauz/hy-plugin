package hybris.extension;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ExtensionDependecyVisitor {

    private static Set<String> visited = new HashSet<>();
    final Extension extension;

    public ExtensionDependecyVisitor(Extension extension) {
        this.extension = extension;
    }

    public void visit(final Consumer<Extension> handler) {
        System.out.println("Visiting required extensions of " + extension.getName());
        if (visited.add(extension.getFolder().getAbsolutePath())) {
            try {
                extension.getRequiredExtensions().forEach(ext -> {
                    if (null != handler) {
                        handler.accept(ext);
                    }
                    new ExtensionDependecyVisitor(ext).visit(handler);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void reset() {
        visited.clear();
    }
}