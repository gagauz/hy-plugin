package hybristools;

import java.io.File;
import java.io.FilenameFilter;

public class Constants {

    public static final String HYBRIS_BIN_DIR_CLASSPATH_VARIABLE = "HYBRIS_BIN_DIR";

    public static final String EXTENSIONINFO_FILE_NAME = "extensioninfo.xml";
    public static final String CLASSPATH_FILE_NAME = ".classpath";
    public static final String PROJECT_FILE_NAME = ".project";
    public static final String SETTINGS_FILE_NAME = ".settings";
    public static final String LIB_DIR_NAME = "lib";
    public static final FilenameFilter JAR_FILTER = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };
    public static final String WEBINF_LIB_DIR_NAME = "web/webroot/WEB-INF/lib";
}
