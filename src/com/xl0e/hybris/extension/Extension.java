package com.xl0e.hybris.extension;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IPath;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.xl0e.hybris.Constants;
import com.xl0e.hybris.utils.DirVisitor;
import com.xl0e.hybris.utils.FileUtiles;
import com.xl0e.hybris.utils.XmlManipulator;
import com.xl0e.hybris.utils.XmlUtils;

public class Extension {

    public static Extension create(File folder, File platformHome) {
        if (new File(folder, Constants.Files.project_properties).isFile()
                || new File(folder, Constants.Files.extensioninfo_xml).isFile()
                || new File(folder, Constants.Files.localextensions_xml).isFile()) {
            return new Extension(folder, platformHome);
        }
        return null;
    }

    private final File platformHome;

    static enum Type {
        CUSTOM,
        PLATFORM,
        CONFIG,
        PLATFORM_EXT,
        EXT
    }

    private final File folder;
    private final Type type;
    private String webContext;
    private File classpath;
    private File project;
    private File settings;
    private File extensioninfo;
    private Set<Extension> requiredExtensions;
    private String packageroot;
    private boolean localExtension;
    private String name;
    private IPath path;

    private Extension(File folder, File platformHome) {
        this.folder = folder;
        this.platformHome = platformHome;
        String path = folder.getAbsolutePath().replace('\\', '/');
        if (platformHome.equals(folder)) {
            type = Type.PLATFORM;
        } else if (path.contains("/bin/custom/")) {
            type = Type.CUSTOM;
        } else if (path.contains("/bin/ext-")) {
            type = Type.EXT;
        } else if (path.contains("/platform/ext/")) {
            type = Type.PLATFORM_EXT;
        } else if (folder.getName().equals("config")) {
            type = Type.CONFIG;
        } else {
            throw new IllegalStateException("Unresolved extension type" + folder);
        }
    }

    public File getFolder() {
        return folder;
    }

    public String getName() {
        if (name == null) {
            if (getExtensioninfo().isFile()) {
                try {
                    XmlUtils.parseDocument(getExtensioninfo(), "extension", new Predicate<Node>() {
                        @Override
                        public boolean test(Node nNode) {
                            Element eElement = (Element) nNode;
                            String name = eElement.getAttribute("name");
                            Extension.this.name = name;
                            return false;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (null == name) {
                name = getFolder().getName();
            }
        }
        return name;
    }

    public boolean isCustom() {
        return type == Type.CUSTOM;
    }

    public File getClasspath() {
        if (null == classpath) {
            classpath = getFileInFolder(Constants.Files._classpath);
        }
        return classpath;
    }

    public File getProject() {
        if (null == project) {
            project = getFileInFolder(Constants.Files._project);
        }
        return project;
    }

    public File getSettings() {
        if (null == settings) {
            settings = getFileInFolder(Constants.Files._settings);
        }
        return settings;
    }

    public File getExtensioninfo() {
        if (null == extensioninfo) {
            extensioninfo = getFileInFolder(Constants.Files.extensioninfo_xml);
        }
        return extensioninfo;
    }

    public boolean isWeb() {
        if (null == webContext) {
            try {
                XmlUtils.parseDocument(getExtensioninfo(), "webmodule", new Predicate<Node>() {
                    @Override
                    public boolean test(Node nNode) {
                        Element eElement = (Element) nNode;
                        String webrootName = eElement.getAttribute("webroot");
                        System.out.println("Found webroot " + webrootName + "");
                        Extension.this.webContext = webrootName;
                        return false;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null != webContext;
    }

    public Set<File> getLibs() {
        Set<File> result = new HashSet<>(getFilesInFolder(Constants.LIB_DIR_NAME, Constants.JAR_FILTER));
        return result;
    }

    public Set<File> getWebinfLibs() {
        Set<File> result = new HashSet<>(getFilesInFolder(Constants.WEBINF_LIB_DIR_NAME, Constants.JAR_FILTER));
        return result;
    }

    public Set<Extension> getRequiredExtensions() {
        if (null == requiredExtensions) {
            requiredExtensions = new HashSet<>();
            File infoFile = getFileInFolder(Constants.Files.extensioninfo_xml);
            if (infoFile.isFile()) {
                try {
                    XmlManipulator xml = new XmlManipulator(infoFile);
                    xml.walkNodes("extensioninfo/extension/requires-extension", nNode -> {
                        Element eElement = (Element) nNode;
                        String extName = eElement.getAttribute("name");
                        System.out.println("Found required extension <requires-extension name=\"" + extName + "\"/>");
                        Extension ext0 = ExtensionResolver.findExtension(getPlatformHome(), extName);
                        if (null != ext0) {
                            // if (!ext0.isPlatformExt()) {
                            requiredExtensions.add(ext0);
                            // }
                        } else {
                            // throw new IllegalStateException("Extension "
                            // +
                            // extName + " was not found");
                            System.err.println("Extension folder " + extName + " was not fond for " + getName());
                        }
                    });

                    xml.walkNodes("extensioninfo/extension/coremodule", nNode -> {
                        Element eElement = (Element) nNode;
                        packageroot = eElement.getAttribute("packageroot");
                        System.out.println("Found coremodule <coremodule packageroot=" + packageroot + "/>");
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            } else {
                System.err.println("Extension " + getName() + " doesnt contain extensioninfo.xml");
            }

        }
        return requiredExtensions;

    }

    public boolean isPlatformExt() {
        return type == Type.PLATFORM_EXT;
    }

    public boolean hasFolder(String name) {
        File folder = getFileInFolder(name);
        return folder.isDirectory();
    }

    @Override
    public int hashCode() {
        return folder.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return folder.equals(obj);
    }

    public File getFileInFolder(String name) {
        return new File(folder, name);
    }

    public File getFileInRamFolder(String name) {
        return new File("R:\\", getName() + "/" + name);
    }

    public Collection<File> getFilesInFolder(String name, FilenameFilter filter) {
        File file = getFileInFolder(name);
        if (file.isDirectory()) {
            return Arrays.asList(file.listFiles(filter));
        }
        return Collections.emptyList();
    }

    public boolean isPlatform() {
        return type == Type.PLATFORM;
    }

    public boolean isConfig() {
        return type == Type.CONFIG;
    }

    public boolean isLocalExtension() {
        return localExtension;
    }

    public void setLocalExtension(boolean b) {
        this.localExtension = b;
    }

    public String getWebContext() {
        return webContext;
    }

    @Override
    public String toString() {
        return getName();
    }

    public File getPlatformHome() {
        return platformHome;
    }

    public String getPackageroot() {
        return packageroot;
    }

    public Iterable<Path> matchFolders(String src) {
        IPath p0 = new org.eclipse.core.runtime.Path(src);
        p0 = p0.removeLastSegments(1);
        File dir = new File(getFolder(), p0.toOSString());

        if (dir.isDirectory()) {
            List<Path> list = new ArrayList<>();
            new DirVisitor(dir, 0).visit(f -> {
                if (f.isDirectory()) {
                    list.add(f.toPath());
                }
            });
            return list;
        }
        return Collections.emptyList();
    }

    public void removeFolders(String... names) {
        for (String name : names) {
            FileUtiles.removeFolder(new File(getFolder(), name));
        }
    }

    public IPath getPath() {
        if (null == path) {
            this.path = new org.eclipse.core.runtime.Path(getFolder().getAbsolutePath());
        }
        return path;
    }
}
