package hybristools;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtensionVisitor {

    public static class Tree {
        final int level;
        File extension;
        List<Tree> children = new ArrayList<>();
        boolean last = true;

        Tree(Tree parent, File extension) {
            this.extension = extension;
            this.level = null == parent ? 0 : parent.level + 1;
            if (null != parent) {
                if (!parent.children.isEmpty()) {
                    parent.children.get(parent.children.size() - 1).last = false;
                }
                parent.children.add(this);
            }
        }

        public void print() {
            if (level > 0) {
                System.out.println(padLeft(extension.getName(), level, last ? '\\' : '+'));
            } else {
                System.out.println(extension.getName());
            }
            for (Tree c : children) {
                c.print();
            }
        }
    }

    public static String padLeft(String s, int n, char x) {
        char[] a = new char[3 * n];
        Arrays.fill(a, ' ');
        a[0] = '|';
        a[3 * n - 3] = x;
        a[3 * n - 2] = '-';
        return new String(a) + s;
    }

    public static final FilenameFilter EXTINFO_XML = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(Constants.EXTENSIONINFO_FILE_NAME);
        }
    };

    static Map<File, Tree> TREE_MAP = new HashMap<>();
}
