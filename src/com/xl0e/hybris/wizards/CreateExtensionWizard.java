package com.xl0e.hybris.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

public class CreateExtensionWizard extends Wizard implements IWorkbenchWizard {

    CreateExtensionPage mainPage;

    /**
     *
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
     *      org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Create Hybris extension wizard"); // NON-NLS-1
        setNeedsProgressMonitor(true);
        mainPage = new CreateExtensionPage(workbench);
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
        return true;
    }

    @Override
    public boolean performFinish() {
        return mainPage.createExtension();
    }

}
