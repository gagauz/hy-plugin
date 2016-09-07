package hybris.importWizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class ImportHybrisWizard extends Wizard implements IImportWizard {

    ImportWizardHybrisPlatformHomePage page1;
    ImportWizardProjectSelectionPage page2;

    public ImportHybrisWizard() {
        super();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        //		IFile file = mainPage.createNewFile();
        //        if (file == null)
        //            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Hybris Import Wizard"); // NON-NLS-1
        setNeedsProgressMonitor(true);
        page1 = new ImportWizardHybrisPlatformHomePage(workbench, "Select platform home", selection); // NON-NLS-1
        page2 = new ImportWizardProjectSelectionPage("Select projects to import", selection); // NON-NLS-1
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
        super.addPages();
        addPage(page1);
        addPage(page2);
    }

}
