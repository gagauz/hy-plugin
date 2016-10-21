package hybristools.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlManipulator {

    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private final File xmlFile;
    private Document doc;

    public XmlManipulator(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    public void walkNodes(String entryPath, Consumer<Node> handler) throws Exception {
        walkNodes(getDoc(), entryPath, handler, false);
    }

    public void saveDocument() {
        try {
            XmlUtils.saveXmlDocument(getDoc(), xmlFile);
        } catch (TransformerException | ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public void setNodeValue(String nodePath, final String value) throws Exception {
        walkNodes(getDoc(), nodePath, (Consumer<Node>) (node -> {
            node.setTextContent(value);
        }), true);

    }

    private Document getDoc() throws ParserConfigurationException, SAXException, IOException {
        if (null == doc) {
            doc = XmlUtils.readXmlFile(xmlFile);
        }
        return doc;
    }

    private void walkNodes(Node parentNode, String entryPath, Consumer<Node> handler, boolean create) throws Exception {
        final String[] nodeNames = entryPath.split("/", 2);
        List<Node> nodeList = getChildNodes(parentNode, nodeNames[0]);
        if (nodeList.isEmpty() && create) {
            Node child = getDoc().createElement(nodeNames[0]);
            parentNode.appendChild(child);
            nodeList.add(child);
        }
        for (Node node : nodeList) {
            if (nodeNames.length == 1) {
                handler.accept(node);
            } else {
                walkNodes(node, nodeNames[1], handler, create);
            }
        }
    }

    private Node createNodes(Node parentNode, String entryPath) throws Exception {
        final String[] nodeNames = entryPath.split("/", 2);
        List<Node> nodeList = getChildNodes(parentNode, nodeNames[0]);
        if (nodeList.isEmpty() || nodeNames.length == 1) {
            Node child = getDoc().createElement(nodeNames[0]);
            parentNode.appendChild(child);
            nodeList.add(child);
        }
        Node lastNode = null;
        for (Node node : nodeList) {
            if (nodeNames.length != 1) {
                return createNodes(node, nodeNames[1]);
            }
            lastNode = node;
        }
        return lastNode;
    }

    private List<Node> getChildNodes(Node parentNode, String name) {
        NodeList children = parentNode.getChildNodes();
        int length = children.getLength();
        List<Node> result = new ArrayList<>(length);
        for (int temp = 0; temp < length; temp++) {
            Node node = children.item(temp);
            if (node.getNodeType() == Node.ELEMENT_NODE
                    && node.getNodeName().equalsIgnoreCase(name)) {
                result.add(node);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return xmlFile.toString();
    }

    public void addNode(String entryPath, String value, String... attrNamesAndValues) throws Exception {
        Node node = createNodes(getDoc(), entryPath);
        if (null != value) {
            node.setTextContent(value);
        }
        NamedNodeMap attrMap = node.getAttributes();
        for (int i = 0; i < attrNamesAndValues.length; i += 2) {
            Node attr = attrMap.getNamedItem(attrNamesAndValues[i]);
            if (null == attr) {
                attr = getDoc().createAttribute(attrNamesAndValues[i]);
            }
            attr.setNodeValue(attrNamesAndValues[i + 1]);
            attrMap.setNamedItem(attr);
        }
    }
}
