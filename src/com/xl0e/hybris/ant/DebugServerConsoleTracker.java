package com.xl0e.hybris.ant;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.IRegion;

public class DebugServerConsoleTracker implements IConsoleLineTracker {

    private IProcess process;
    private IConsole console;

    @Override
    public void init(IConsole console) {
        process = console.getProcess();
        this.console = console;
        //        this.console.addPatternMatchListener(new IPatternMatchListener() {
        //
        //            private String escape(String path) {
        //                StringBuffer buffer = new StringBuffer(path);
        //                int index = buffer.indexOf("\\"); //$NON-NLS-1$
        //                while (index >= 0) {
        //                    buffer.insert(index, '\\');
        //                    index = buffer.indexOf("\\", index + 2); //$NON-NLS-1$
        //                }
        //                return buffer.toString();
        //            }
        //
        //            @Override
        //            public String getPattern() {
        //                return fFilePath;
        //            }
        //
        //            @Override
        //            public void matchFound(PatternMatchEvent event) {
        //                try {
        //                    addHyperlink(new ConsoleLogFileHyperlink(fFilePath), event.getOffset(), event.getLength());
        //                    removePatternMatchListener(this);
        //                } catch (BadLocationException e) {
        //                }
        //            }
        //
        //            @Override
        //            public int getCompilerFlags() {
        //                return 0;
        //            }
        //
        //            @Override
        //            public String getLineQualifier() {
        //                return null;
        //            }
        //
        //            @Override
        //            public void connect(TextConsole console) {
        //            }
        //
        //            @Override
        //            public void disconnect() {
        //            }
        //        });
    }

    @Override
    public void lineAppended(IRegion line0) {
    }

    @Override
    public void dispose() {
    }

}
