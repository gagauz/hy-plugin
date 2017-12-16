package com.xl0e.hybris.extension;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.xl0e.hybris.utils.XmlUtils;

public class LocalExtensionVisitor {

    private File platformFolder;
    private static Set<Extension> localExtensions = new HashSet<>();

    public LocalExtensionVisitor(File platformFolder) {
        localExtensions.clear();
        this.platformFolder = platformFolder;
    }

    public void visit(Consumer<Extension> handler) {

        Consumer<Extension> checkHandler = ext -> {
            if (localExtensions.add(ext)) {
                handler.accept(ext);
            }
        };

        File localExtension = new File(platformFolder.getParentFile().getParentFile(), "/config/localextensions.xml");

        if (localExtension.isFile()) {
            try {
                XmlUtils.parseDocument(localExtension, "extension", new Predicate<Node>() {

                    @Override
                    public boolean test(Node nNode) {
                        Element eElement = (Element) nNode;
                        String extName = eElement.getAttribute("name");
                        System.out.println("Found local <extension name=\"" + extName + "\"/>");
                        Extension extension = ExtensionResolver.findExtension(LocalExtensionVisitor.this.platformFolder, extName);
                        if (null != extension) {
                            extension.setLocalExtension(true);
                            checkHandler.accept(extension);
                            new ExtensionDependecyVisitor(extension).visit(checkHandler);
                        } else {
                            System.err.println("Ext folder " + extName + " is not found");
                        }
                        return false;
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}