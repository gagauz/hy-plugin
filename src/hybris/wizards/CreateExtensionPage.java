package hybris.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

import hybris.Constants;
import hybris.ant.AntCommand;
import hybris.ant.AntCommandConsoleTracker;
import hybris.extension.Extension;
import hybris.extension.ExtensionFixer;
import hybris.extension.ExtensionResolver;
import hybris.messages.Messages;
import hybristools.utils.EclipseUtils;
import hybristools.utils.XmlManipulator;
import hybristools.utils.XmlUtils;

public class CreateExtensionPage extends WizardPage {

    public static GridData FILL_TOP = new GridData(SWT.FILL, SWT.FILL, true, false);

    private IWorkbench workbench;
    private Text extNameText;
    private Text extPackageText;
    private Combo templateCombobox;
    private IProject platformProject;
    private Button addToLocalExtButton;
    private Button runAntCleanAllButton;
    private boolean templatesFetched = false;
    private boolean addToLocalChecked = false;
    private boolean runAntCleanAllChecked = false;
    private List<String> templates;

    private Listener listener = new Listener() {

        @Override
        public void handleEvent(Event event) {
            if (!templatesFetched) {
                templateCombobox.setEnabled(false);
                findTemplates();
                templateCombobox.setEnabled(true);
                templatesFetched = true;
            }
        }
    };

    protected CreateExtensionPage(IWorkbench workbench) {
        super("Create new hybris extension");
        this.workbench = workbench;
        setTitle("Hybris extension");
        setDescription("Create new hybris extension from template");
    }

    private Composite createGroup(Composite parent) {
        Composite group1 = new Composite(parent, SWT.NONE);
        group1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.makeColumnsEqualWidth = false;
        layout.marginLeft = -5;
        layout.marginRight = 0;
        layout.marginBottom = 0;
        layout.horizontalSpacing = 0;
        group1.setLayout(layout);

        return group1;
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite workArea = new Composite(parent, SWT.NONE);
        setControl(workArea);

        GridLayout gl = new GridLayout(1, false);
        workArea.setLayout(gl);

        createNameText(workArea);

        createPackageNameText(workArea);

        createTemplateCombo(workArea);

        createOptions(createGroup(workArea));

        Dialog.applyDialogFont(workArea);
        parent.removeListener(SWT.Activate, listener);
        parent.addListener(SWT.Activate, listener);
    }

    void createOptions(Composite workArea) {

        addToLocalExtButton = EclipseUtils.createCheckbox(workArea, "Add extension to localextensions.xml", (button, event) -> {
            runAntCleanAllButton.setEnabled(button.getSelection());
            if (!runAntCleanAllButton.isEnabled()) {
                runAntCleanAllButton.setSelection(false);
            }
            addToLocalChecked = button.getSelection();
        });
        addToLocalExtButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        runAntCleanAllButton = EclipseUtils.createCheckbox(workArea, "Run ant clean all on finish", (button, event) -> {
            runAntCleanAllChecked = button.getSelection();
        });
        runAntCleanAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addToLocalExtButton.setSelection(false);
        runAntCleanAllButton.setEnabled(false);
    }

    private void createTemplateCombo(Composite workArea) {

        Label label = new Label(workArea, SWT.LEFT);
        label.setText("Select template extension");
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite projectGroup = new Composite(workArea, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        layout.marginWidth = 0;
        layout.makeColumnsEqualWidth = true;
        layout.marginHeight = 0;
        projectGroup.setLayout(layout);
        projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        templateCombobox = new Combo(projectGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        templateCombobox.setLayoutData(gd);

        Button refresh = EclipseUtils.createButton(projectGroup, "Refresh", (button, event) -> {
            templatesFetched = false;
            Event e = new Event();
            e.widget = event.widget;
            listener.handleEvent(e);
        });
        refresh.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    private void createNameText(Composite workArea) {
        EclipseUtils.createLeftLabel(workArea, "Enter the name for extension");
        extNameText = EclipseUtils.createLeftText(workArea, (t, e) -> {
            if (null == t.getText() || "".equals(t.getText().trim())
                    || null != ExtensionResolver.findExtension(platformProject.getLocation().toFile(), extNameText.getText())) {
                setMessage("Enter the unique name of extension", IMessageProvider.ERROR);
            } else {
                setMessage(null);
            }
        });
    }

    private void createPackageNameText(Composite workArea) {
        EclipseUtils.createLeftLabel(workArea, "Enter the package name for your extension");
        extPackageText = EclipseUtils.createLeftText(workArea, (t, e) -> {

        });
    }

    public boolean createExtension() {
        int idx = templateCombobox.getSelectionIndex();
        String tpl;
        String name;
        String packageName;
        if (idx > -1) {
            tpl = templateCombobox.getItem(idx);
        } else {
            setMessage("Select extension template", IMessageProvider.ERROR);
            return false;
        }

        if (null == extNameText.getText() || "".equals(extNameText.getText().trim())
                || null != ExtensionResolver.findExtension(platformProject.getLocation().toFile(), extNameText.getText())) {
            setMessage("Enter the unique name of extension", IMessageProvider.ERROR);
            return false;
        } else {
            name = extNameText.getText();
        }

        if (null == extPackageText.getText() || "".equals(extPackageText.getText().trim())) {
            setMessage("Enter the package name of your extension", IMessageProvider.ERROR);
            return false;
        } else {
            packageName = extPackageText.getText();
        }

        Job job = new Job("Looking for template extensions") {
            @Override
            public IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask("Looking for template extensions", 1);
                    if (!monitor.isCanceled()) {

                        int res = new AntCommand(platformProject.getLocation(), Arrays.asList("extgen"), IProcess.ATTR_PROCESS_TYPE,
                                AntCommandConsoleTracker.ANT_PROCESS_TYPE,
                                AntCommandConsoleTracker.EXT_TEMPLATE_ATTR, tpl,
                                AntCommandConsoleTracker.EXT_NAME_ATTR, name,
                                AntCommandConsoleTracker.EXT_PACKAGE_NAME_ATTR, packageName).accept(platformProject.getLocation().toFile(),
                                        monitor);

                        monitor.worked(1);
                        if (addToLocalChecked && res == 0) {

                            Extension config = ExtensionResolver.getConfig(platformProject.getLocation().toFile());
                            if (null == config) {
                                config = Extension.create(platformProject.getLocation().removeLastSegments(2).append("/config").toFile(),
                                        platformProject.getLocation().toFile());
                            }
                            File file = config.getFileInFolder(Constants.LOCALEXTENSIONS);

                            XmlManipulator xml = new XmlManipulator(file);
                            xml.walkNodes("hybrisconfig/extensions/extension", node -> {
                                if (name.equals(node.getAttributes().getNamedItem("name").getNodeValue())) {
                                    node.getParentNode().removeChild(node);
                                }
                            });
                            xml.addNode("hybrisconfig/extensions/extension", null, "name", name);
                            xml.saveDocument();
                        }

                        ExtensionResolver.clearCache();
                        Extension newExt = ExtensionResolver.findExtension(platformProject.getLocation().toFile(), name);
                        new ExtensionFixer(newExt).fix(monitor);
                        IProject project = EclipseUtils.createProject(new Path(newExt.getFolder().getAbsolutePath()), name,
                                SubMonitor.convert(monitor, 1));
                        EclipseUtils.addProjectToWorkingSet(workbench, Messages.WorkingSet_Custom, project);
                    } else {
                        return Status.CANCEL_STATUS;
                    }
                    ResourcesPlugin.getWorkspace().getRoot().refreshLocal(3, monitor);
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return new Status(IStatus.ERROR, "hybris", e.getMessage(), e);
                } finally {
                    monitor.done();
                }
            }
        };
        job.schedule();

        return true;
    }

    void findTemplates() {
        platformProject = ResourcesPlugin.getWorkspace().getRoot().getProject("platform");
        if (platformProject.exists()) {

            WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
                @Override
                protected void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Fetching extension templates...", 100);
                    templates = Collections.synchronizedList(new ArrayList<>());
                    ExtensionResolver.getAllExtensions(platformProject.getLocation().toFile(), f -> {
                        monitor.worked(1);
                    }).forEach((k, ext) -> {
                        try {
                            XmlUtils.parseDocument(ext.getExtensioninfo(), "meta", node -> {
                                if (null != node.getAttributes().getNamedItem("key")
                                        && node.getAttributes().getNamedItem("key").getNodeValue()
                                                .equalsIgnoreCase("extgen-template-extension")
                                        && null != node.getAttributes().getNamedItem("value")
                                        && node.getAttributes().getNamedItem("value").getNodeValue().equalsIgnoreCase("true")) {
                                    templates.add(ext.getName());
                                }
                                return false;
                            });
                            monitor.worked(1);
                        } catch (Exception e) {
                            IWorkbenchWindow win = workbench.getActiveWorkbenchWindow();
                            MessageDialog.openError(win.getShell(), "Error", "Failed to visit local extensions " + e.getMessage());
                        }
                    });
                    Collections.sort(templates);
                    monitor.done();
                }
            };
            // run the new project creation operation
            try {
                getContainer().run(true, true, op);
            } catch (InterruptedException | InvocationTargetException e) {
                IStatus status;
                String message = e.getMessage();
                if (e instanceof CoreException) {
                    status = ((CoreException) e).getStatus();
                } else {
                    status = new Status(IStatus.ERROR, IDEWorkbenchPlugin.IDE_WORKBENCH, 1, message, e);
                }
                ErrorDialog.openError(getShell(), message, null, status);
            } finally {
                templateCombobox.setItems(templates.toArray(new String[templates.size()]));
            }

        }
    }
}
