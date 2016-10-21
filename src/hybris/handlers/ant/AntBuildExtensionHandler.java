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

public class AntBuildExtensionHandler extends AbstractAntHandler {

    @Override
    protected List<String> getArguments() {
        return Arrays.asList("build");
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
            /*
             * Object firstSegmentObj = treePath.getFirstSegment(); IProject
             * theProject = ((IAdaptable)
             * firstSegmentObj).getAdapter(IProject.class); if (theProject ==
             * null) { MessageDialog.openInformation(window.getShell(),
             * "Navigator Popup", getClassHierarchyAsMsg(
             * "Expected the first segment to be IAdapatable to an IProject.\nBut got the following class hierarchy instead."
             * , "Make sure to directly select a file.", firstSegmentObj));
             * return false; }
             *
             * // The last segment should be an IResource Object lastSegmentObj
             * = treePath.getLastSegment(); IResource theResource =
             * ((IAdaptable) lastSegmentObj).getAdapter(IResource.class); if
             * (theResource == null) {
             * MessageDialog.openInformation(window.getShell(),
             * "Navigator Popup", getClassHierarchyAsMsg(
             * "Expected the last segment to be IAdapatable to an IResource.\nBut got the following class hierarchy instead."
             * , "Make sure to directly select a file.", firstSegmentObj));
             * return false; }
             *
             * // As the last segment is an IResource we should be able to get
             * an // IFile reference from it IFile theFile = ((IAdaptable)
             * lastSegmentObj).getAdapter(IFile.class);
             *
             * // Extract additional information from the IResource and IProject
             * String workspaceName =
             * theResource.getWorkspace().getRoot().getLocation().toOSString();
             * String projectName = theProject.getName(); String fileName =
             * theResource.getName();
             *
             * return true; } else { String selectionClass =
             * selection.getClass().getSimpleName();
             * MessageDialog.openError(window.getShell(),
             * "Unexpected Selection Class", String.
             * format("Expected a TreeSelection but got a %s instead.\nProcessing Terminated."
             * , selectionClass)); }
             *
             * return false;
             */
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private static String getClassHierarchyAsMsg(String msgHeader, String msgTrailer, Object theObj) {
        String msg = msgHeader + "\n\n";

        Class theClass = theObj.getClass();
        while (theClass != null) {
            msg = msg + String.format("Class=%s\n", theClass.getName());
            Class[] interfaces = theClass.getInterfaces();
            for (Class theInterface : interfaces) {
                msg = msg + String.format("    Interface=%s\n", theInterface.getName());
            }
            theClass = theClass.getSuperclass();
        }

        msg = msg + "\n" + msgTrailer;

        return msg;
    }
}
