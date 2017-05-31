package hybris.handlers.ant;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import hybris.messages.Messages;

public class AntBuildExtensionHandler extends AbstractAntHandler {

    public AntBuildExtensionHandler() {
        super(Messages.AntCommand_BuildFor);
    }

    public AntBuildExtensionHandler(String string) {
        super(string);
    }

    @Override
    protected List<String> getArguments() {
        return Arrays.asList("clean", "build");
    }

    @Override
    protected IJavaProject getTarget(ExecutionEvent event) {
        return extractProjectAndFileFromInitiatingEvent(event);
    }

    private IJavaProject extractProjectAndFileFromInitiatingEvent(ExecutionEvent event) {
        // ============================================================================================================
        // The execute method of the handler is invoked to handle the event. As
        // we only contribute to Explorer
        // Navigator views we expect to get a selection tree event
        // ============================================================================================================
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        // Get the active WorkbenchPage
        IWorkbenchPage activePage = window.getActivePage();

        // Get the Selection from the active WorkbenchPage page
        ISelection selection = activePage.getSelection();
        if (selection instanceof ITreeSelection) {
            TreeSelection treeSelection = (TreeSelection) selection;
            TreePath[] treePaths = treeSelection.getPaths();
            TreePath treePath = treePaths[0];

            // The TreePath contains a series of segments in our usage:
            // o The first segment is usually a project
            // o The last segment generally refers to the file

            // The first segment should be a IProject
            for (int i = 0; i < treePath.getSegmentCount(); i++) {
                Object segmentObj = treePath.getSegment(i);
                if (IJavaProject.class.isAssignableFrom(segmentObj.getClass())) {
                    return (IJavaProject) segmentObj;
                }
            }
            return null;
        }
        return null;
    }

}
