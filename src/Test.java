import java.io.File;

import com.xl0e.hybris.utils.XmlManipulator;

public class Test {
    public static void main(String[] args) {
        try {
            File file = new File(Test.class.getResource("/text.xml").toURI());
            if (!file.canWrite()) {
                throw new IllegalStateException();
            }
            XmlManipulator xml = new XmlManipulator(file);
            xml.addNode("hybrisconfig/a/b/c", "1", "d", "e");
            xml.addNode("hybrisconfig/extensions/extension", null, "name", "text");
            xml.saveDocument();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
