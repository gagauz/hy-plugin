package hybristools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ClasspathTools {
    public static Element createSrcClasspathEntry(Node node, String path, String output, boolean export) {
        return createElement(node, "classpathentry", "kind", "src", "path", path, "output", output, "exported", String.valueOf(export));
    }

    public static Element createSrcClasspathEntry(Node node, String path, boolean export) {
        return createElement(node, "classpathentry", "kind", "src", "path", path, "exported", String.valueOf(export));
    }

    public static Element createLibClasspathEntry(Node node, String path, boolean export) {
        return createElement(node, "classpathentry", "kind", "lib", "path", path, "exported", String.valueOf(export));
    }

    public static Element createLibClasspathEntry(Node node, String path, String sourcepath, boolean export) {
        return createElement(node, "classpathentry", "kind", "lib", "path", path, "sourcepath", sourcepath, "exported",
                String.valueOf(export));
    }

    public static void createSrcFolder(Extension extension, Node node, String path, String output, boolean exported, String including) {
        if (extension.hasFolder(path)) {
            createElement(node, "classpathentry", "kind", "src", "path", path, "output", output, "exported", String.valueOf(exported),
                    "including", including);
        }
    }

    public static void createMultipleSrcFolder(Extension extension, Node node, String[] sources, String output, boolean exported) {
        for (String src : sources) {
            if (extension.hasFolder(src)) {
                createSrcClasspathEntry(node, src, output, exported);
            }
        }
    }

    public static void createMultipleSrcFolder(Extension extension, Node node, String[] sources, String output, boolean exported,
            String including) {
        for (String src : sources) {
            if (extension.hasFolder(src)) {
                createElement(node, "classpathentry", "kind", "src", "path", src, "output", output, "exported", String.valueOf(exported),
                        "including", including);
            }
        }
    }

    public static Element createElement(Node node, String name, String... nameAndValues) {
        Document doc = node.getOwnerDocument();

        Element element = doc.createElement(name);
        for (int i = 0; i < nameAndValues.length; i += 2) {
            element.setAttribute(nameAndValues[i], nameAndValues[i + 1]);
        }
        node.appendChild(element);
        return element;
    }
}
