package hybris.ant;

import java.io.File;
import java.util.function.Consumer;

public class DirVisitor {

    private File directory;

    private final int maxdepth;

    public DirVisitor(File directory, int maxdepth) {
        this.directory = directory;
        this.maxdepth = maxdepth;
    }

    public void visit(Consumer<File> visitor) {
        if (null != directory && directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                try {
                    visitor.accept(file);
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    public void visitRecursive(Consumer<File> visitor) {
        for (File file : directory.listFiles()) {
            try {
                visitor.accept(file);
                if (file.isDirectory() && maxdepth > -1) {
                    new DirVisitor(file, maxdepth - 1).visitRecursive(visitor);
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
