package hybris.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class ImportPlatformWizard extends Wizard implements IImportWizard {

    ImportPlatformPage mainPage;

    public ImportPlatformWizard() {
        super();
    }

    /**
     *
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
     *      org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Hybris Import Wizard"); // NON-NLS-1
        setNeedsProgressMonitor(true);
        mainPage = new ImportPlatformPage(workbench);
    }

    /**
     *
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
        super.addPages();
        addPage(mainPage);
    }

    /**
     * @see Wizard#performCancel()
     */
    @Override
    public boolean performCancel() {
        mainPage.performCancel();
        return true;
    }

    /**
     * @see Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        return mainPage.createProjects();
    }
}
