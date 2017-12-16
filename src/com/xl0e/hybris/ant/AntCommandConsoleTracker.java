package com.xl0e.hybris.ant;

import java.io.IOException;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;

public class AntCommandConsoleTracker implements IConsoleLineTracker {
    public static final String ANT_PROCESS_TYPE = "com.xl0e.hybris.ant.AntCommand";
    public static final String EXT_TEMPLATE_ATTR = "com.xl0e.hybris.extgen.ext_template";
    public static final String EXT_NAME_ATTR = "com.xl0e.hybris.extgen.ext_name";
    public static final String EXT_PACKAGE_NAME_ATTR = "com.xl0e.hybris.extgen.ext_package_name";

    private boolean done = false;
    private boolean extNameString = false;
    private boolean templateString = false;
    private boolean extPackageNameString = false;
    private IProcess process;
    private IConsole console;

    @Override
    public void init(IConsole console) {
        process = console.getProcess();
        this.console = console;
    }

    @Override
    public void lineAppended(IRegion line0) {
        if (!done) {
            String line = "";
            try {
                line = console.getDocument().get(line0.getOffset(), line0.getLength()).trim();

                if (line.startsWith("[input] Please choose a template for generation")) {
                    templateString = true;
                }
                if (templateString && line.startsWith("[input] Press [Enter] to use the default value")) {
                    process.getStreamsProxy().write(process.getAttribute(EXT_TEMPLATE_ATTR) + "\n\r");
                    templateString = false;
                }

                if (line.startsWith("[input] Please choose the name of your extension")) {
                    extNameString = true;
                }
                if (extNameString && line.startsWith("[input] Press [Enter] to use the default")) {
                    process.getStreamsProxy().write(process.getAttribute(EXT_NAME_ATTR) + "\n\r");
                    extNameString = false;
                }

                if (line.startsWith("[input] Please choose the package name of your extension")) {
                    extPackageNameString = true;
                }
                if (extPackageNameString && line.startsWith("[input] Press [Enter] to use the default value")) {
                    process.getStreamsProxy().write(process.getAttribute(EXT_PACKAGE_NAME_ATTR) + "\n\r");
                    done = true;
                }
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void dispose() {
    }

}
