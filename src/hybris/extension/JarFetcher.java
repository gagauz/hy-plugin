package hybris.extension;

import java.io.File;
import java.util.function.Consumer;

import hybris.ant.DirVisitor;

public class JarFetcher {
    private File extension;
    private String[] folder;

    public JarFetcher(File extension, String... folder) {
        this.extension = extension;
        this.folder = folder;
    }

    public void fetch(Consumer<File> consumer) {
        if (folder.length > 0) {
            for (String inFolder : folder) {
                new DirVisitor(new File(extension, inFolder), 0).visit(jar -> {
                    if (jar.getName().endsWith(".jar")) {
                        consumer.accept(jar);
                    }
                });
            }
        } else {
            new DirVisitor(extension, 4).visitRecursive(jar -> {
                if (jar.getName().endsWith(".jar")) {
                    consumer.accept(jar);
                }
            });
        }
    }

}
