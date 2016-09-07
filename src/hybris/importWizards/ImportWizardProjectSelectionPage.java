package hybris.importWizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.internal.wizards.datatransfer.WizardProjectsImportPage;

public class ImportWizardProjectSelectionPage extends WizardProjectsImportPage {

    public ImportWizardProjectSelectionPage(String string, IStructuredSelection selection) {
        super(string, string, selection);
    }

}
