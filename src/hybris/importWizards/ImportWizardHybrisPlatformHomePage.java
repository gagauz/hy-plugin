package hybris.importWizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.dialogs.ImportPage;


public class ImportWizardHybrisPlatformHomePage extends ImportPage {

    //    protected FileFieldEditor editor;
    protected DirectoryFieldEditor editor;

    public ImportWizardHybrisPlatformHomePage(IWorkbench workbench, String pageName, IStructuredSelection selection) {
        super(workbench, selection);
        setTitle(pageName); //NON-NLS-1
        setDescription("Import folder of hybris platform (usually hybris/bin/platform)"); // NON-NLS-1
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createAdvancedControls(org.eclipse.swt.widgets.Composite)
     */
    protected void createAdvancedControls(Composite parent) {
        Composite folderSelectionArea = new Composite(parent, SWT.NONE);
        GridData folderSelectionData = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.FILL_HORIZONTAL);
        folderSelectionArea.setLayoutData(folderSelectionData);

        GridLayout fileSelectionLayout = new GridLayout();
        fileSelectionLayout.numColumns = 3;
        fileSelectionLayout.makeColumnsEqualWidth = false;
        fileSelectionLayout.marginWidth = 0;
        fileSelectionLayout.marginHeight = 0;
        folderSelectionArea.setLayout(fileSelectionLayout);

        //		editor = new FileFieldEditor("fileSelect","Select File: ",folderSelectionArea); //NON-NLS-1 //NON-NLS-2
        editor = new DirectoryFieldEditor("folderSelect", "Select hybris platform folder: ", folderSelectionArea); //NON-NLS-1 //NON-NLS-2
        editor.getTextControl(folderSelectionArea).addModifyListener(new ModifyListener(){
            @Override
            public void modifyText(ModifyEvent e) {
                IPath path = new Path(ImportWizardHybrisPlatformHomePage.this.editor.getStringValue());

            }
        });
        //		editor.setFileExtensions(extensions);
        folderSelectionArea.moveAbove(null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createLinkTarget()
     */
    protected void createLinkTarget() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#getInitialContents()
     */
    protected InputStream getInitialContents() {
        try {
            return new FileInputStream(new File(editor.getStringValue()));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#getNewFileLabel()
     */
    protected String getNewFileLabel() {
        return "New File Name:"; //NON-NLS-1
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
     */
    protected IStatus validateLinkedResource() {
        return new Status(IStatus.OK, "hybris", IStatus.OK, "", null); //NON-NLS-1 //NON-NLS-2
    }

}
