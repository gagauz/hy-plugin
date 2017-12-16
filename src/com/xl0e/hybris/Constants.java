package com.xl0e.hybris;

import java.io.File;
import java.io.FilenameFilter;

public class Constants {

    public interface Files {
        String extensioninfo_xml = "extensioninfo.xml";
        String localextensions_xml = "localextensions.xml";
        String project_properties = "project.properties";
        String _classpath = ".classpath";
        String _project = ".project";
        String _settings = ".settings";
        String org_eclipse_jdt_core_prefs = "org.eclipse.jdt.core.prefs";
        String org_eclipse_jdt_ui_prefs = "org.eclipse.jdt.ui.prefs";

        interface Hybris {
            String platform = "platform";
            String bin = "bin";
        }
    }

    public interface Templates {

        String empty_classpath_xml = "/tpl/empty_classpath.xml";

    }

    public static final String CUSTOM_PROJECT_CP_LIB = "Common libs";
    public static final String HYBRIS_BIN_DIR_CLASSPATH_VARIABLE = "HYBRIS_BIN_DIR";

    public static final String LIB_DIR_NAME = "lib";
    public static final FilenameFilter JAR_FILTER = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };
    public static final String WEBINF_LIB_DIR_NAME = "web/webroot/WEB-INF/lib";

}
