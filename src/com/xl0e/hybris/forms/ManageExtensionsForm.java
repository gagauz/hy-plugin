package com.xl0e.hybris.forms;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

import com.xl0e.hybris.extension.ExtensionResolver;
import com.xl0e.hybris.messages.Messages;
import com.xl0e.hybris.utils.ProjectRecord;

public class ManageExtensionsForm extends Window {

    private FormToolkit toolkit;
    private ScrolledForm form;
    private IProject platformProject;
    private List<ProjectRecord> selectedProjects;
    private CheckboxTreeViewer projectsListCheckbox;

    public ManageExtensionsForm(Shell parentShell) {
        super(new SameShellProvider(parentShell));
        if (parentShell == null && Policy.DEBUG_DIALOG_NO_PARENT) {
            Policy.getLog().log(
                    new Status(IStatus.INFO, Policy.JFACE, IStatus.INFO, this
                            .getClass()
                            + " created with no shell", //$NON-NLS-1$
                            new Exception()));
        }

        setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.MAX | SWT.RESIZE
                | getDefaultOrientation());

        setBlockOnOpen(true);

    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     *
     * @return
     */
    @Override
    public Control createContents(final Composite parent) {
        initializeBounds();
        parent.setLayoutData(new GridData(GridData.FILL_BOTH));
        toolkit = new FormToolkit(parent.getDisplay());
        form = toolkit.createScrolledForm(parent);
        form.setText("Disable or enable extensions");
        form.setLayout(new GridLayout());
        form.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        form.getBody().setLayout(new GridLayout());
        form.getBody().setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        createProjectsList(form.getBody());

        return form;
    }

    /**
     * Passing the focus request to the form.
     */
    public void setFocus() {
        form.setFocus();
    }

    /**
     * Disposes the toolkit
     */
    public void dispose() {
        toolkit.dispose();
    }

    private void findWebExtension() {
        platformProject = ResourcesPlugin.getWorkspace().getRoot().getProject("platform");
        if (platformProject.exists()) {

            WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
                @Override
                protected void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Fetching extension templates...", 100);
                    selectedProjects = Collections.synchronizedList(new ArrayList<>());
                    ExtensionResolver.getAllExtensions(platformProject.getLocation().toFile(), f -> {
                        monitor.worked(1);
                    }).forEach((k, ext) -> {
                        if (ext.isWeb()) {
                            selectedProjects.add(new ProjectRecord(ext));
                        }
                        monitor.worked(1);

                    });
                    Collections.sort(selectedProjects);
                    monitor.done();
                }
            };

            ProgressMonitorDialog dialog = new ProgressMonitorDialog(getParentShell());
            //            final IProgressMonitor monitor = dialog.getProgressMonitor();
            dialog.setCancelable(true);

            try {
                dialog.run(true, true, op);
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
            }
        }
    }

    /**
     * Create the checkbox list for the found projects.
     *
     * @param workArea
     */
    private void createProjectsList(Composite workArea) {

        Label title = new Label(workArea, SWT.NONE);
        title.setText(Messages.WizardProjectsImportPage_ProjectsListTitle);

        Composite listComposite = new Composite(workArea, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.makeColumnsEqualWidth = false;
        listComposite.setLayout(layout);

        listComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));

        projectsListCheckbox = new CheckboxTreeViewer(listComposite, SWT.BORDER);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = new PixelConverter(projectsListCheckbox.getControl()).convertWidthInCharsToPixels(25);
        gridData.heightHint = new PixelConverter(projectsListCheckbox.getControl()).convertHeightInCharsToPixels(10);
        projectsListCheckbox.getControl().setLayoutData(gridData);
        projectsListCheckbox.setContentProvider(new ITreeContentProvider() {

            @Override
            public Object[] getChildren(Object parentElement) {
                return null;
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return getProjectRecords();
            }

            @Override
            public boolean hasChildren(Object element) {
                return false;
            }

            @Override
            public Object getParent(Object element) {
                return null;
            }

            @Override
            public void dispose() {

            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }

        });

        projectsListCheckbox.setLabelProvider(new ProjectLabelProvider());
        //        projectsListCheckbox.addCheckStateListener(event -> {
        //            ProjectRecord element = (ProjectRecord) event.getElement();
        //            setPageComplete(projectsListCheckbox.getCheckedElements().length > 0);
        //        });

        projectsListCheckbox.setInput(this);
        projectsListCheckbox.setComparator(new ViewerComparator());
    }

    public ProjectRecord[] getProjectRecords() {
        if (null == selectedProjects) {
            findWebExtension();
        }
        return selectedProjects.toArray(new ProjectRecord[selectedProjects.size()]);
    }

    private final class ProjectLabelProvider extends LabelProvider implements IColorProvider {

        @Override
        public String getText(Object element) {
            return ((ProjectRecord) element).getProjectName() + ' ' + '(' + ((ProjectRecord) element).getExtension().getWebContext() + ')';
        }

        @Override
        public Color getBackground(Object element) {
            return null;
        }

        @Override
        public Color getForeground(Object element) {
            return null;
        }
    }
}