package hybris.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardDataTransferPage;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.StatusUtil;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.eclipse.ui.statushandlers.StatusManager;

import hybris.ant.ImportOption;
import hybris.extension.Extension;
import hybris.extension.ExtensionFixer;
import hybris.extension.ExtensionFixer.Holder;
import hybris.extension.ExtensionResolver;
import hybris.extension.LocalExtensionVisitor;
import hybris.messages.Messages;
import hybristools.utils.EclipseUtils;

public class ImportPlatformPage extends WizardDataTransferPage {

    /**
     * The name of the folder containing metadata information for the workspace.
     */
    public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

    private ILeveledImportStructureProvider structureProvider;

    private final IWorkbench workbench;

    private final class ProjectLabelProvider extends LabelProvider implements IColorProvider {

        @Override
        public String getText(Object element) {
            return ((ProjectRecord) element).getProjectLabel();
        }

        @Override
        public Color getBackground(Object element) {
            return null;
        }

        @Override
        public Color getForeground(Object element) {
            ProjectRecord projectRecord = (ProjectRecord) element;
            if (projectRecord.hasConflicts) {
                return getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);
            }
            return null;
        }
    }

    /**
     * Class declared public only for test suite.
     *
     */
    public class ProjectRecord {

        Extension extension;

        String projectName;

        boolean hasConflicts;

        IProject project;

        /**
         * Create a record for a project based on the info in the file.
         *
         * @param file
         */
        ProjectRecord(Extension ext) {
            extension = ext;
            projectName = ext.getName();
        }

        /**
         * Get the name of the project
         *
         * @return String
         */
        public String getProjectName() {
            return projectName;
        }

        /**
         * Returns whether the given project description file was invalid
         *
         * @return boolean
         */
        public boolean isInvalidProject() {
            return false;
        }

        /**
         * Gets the label to be used when rendering this project record in the
         * UI.
         *
         * @return String the label
         * @since 3.4
         */
        public String getProjectLabel() {
            String path = extension.getFolder().getAbsolutePath();
            return NLS.bind(
                    Messages.WizardProjectsImportPage_projectLabel,
                    projectName, path);
        }

        /**
         * @return Returns the hasConflicts.
         */
        public boolean hasConflicts() {
            return hasConflicts;
        }
    }

    /**
     * A filter to remove conflicting projects
     */
    class ConflictingProjectFilter extends ViewerFilter {

        @Override
        public boolean select(Viewer viewer, Object parentElement,
                Object element) {
            return !((ProjectRecord) element).hasConflicts;
        }

    }

    class CustomExtensionFilter extends ViewerFilter {

        @Override
        public boolean select(Viewer viewer, Object parentElement,
                Object element) {
            return ((ProjectRecord) element).extension.isCustom()
                    || ((ProjectRecord) element).extension.isPlatform()
                    || ((ProjectRecord) element).extension.isConfig();
        }

    }

    class LocalExtensionFilter extends ViewerFilter {

        @Override
        public boolean select(Viewer viewer, Object parentElement,
                Object element) {
            return !((ProjectRecord) element).extension.isPlatformExt();
        }

    }

    // dialog store id constants
    private final static String STORE_DIRECTORIES = "WizardProjectsImportPage.STORE_DIRECTORIES";//$NON-NLS-1$

    private Combo directoryPathField;

    private CheckboxTreeViewer projectsListCheckbox;

    private ProjectRecord[] selectedProjects = new ProjectRecord[0];

    // private RadioGroupFieldEditor extensionFilter;

    // Keep track of the directory that we browsed to last time
    // the wizard was invoked.
    private static String previouslyBrowsedDirectory = ""; //$NON-NLS-1$

    private Label projectFromDirectoryRadio;

    private Button browseDirectoriesButton;

    // The last selected path; to minimize searches
    private String lastPath;

    // The initial path to set
    private String initialPath;

    private IStructuredSelection currentSelection;

    private Button createWorkingSets;
    private Button hideConflictingProjects;

    private ViewerFilter conflictingProjectsFilter = new ConflictingProjectFilter();
    private ViewerFilter customExtensionFilter = new CustomExtensionFilter();
    private ViewerFilter localExtensionFilter = new LocalExtensionFilter();

    private File platformHome;

    private Button customRadioButton;

    private Button notPlatformRadioButton;

    private Button allRadioButton;

    private boolean createWorkingSetsFlag = true;

    /**
     * Creates a new project creation wizard page.
     *
     * @param string
     * @param workbench
     *
     */
    public ImportPlatformPage(IWorkbench workbench) {
        this("wizardExternalProjectsPage", workbench, null, null); //$NON-NLS-1$
    }

    /**
     * More (many more) parameters.
     *
     * @param pageName
     * @param initialPath
     * @param currentSelection
     * @since 3.5
     */
    public ImportPlatformPage(String pageName, IWorkbench workbench, String initialPath,
            IStructuredSelection currentSelection) {
        super(pageName);
        this.workbench = workbench;
        this.initialPath = initialPath;
        this.currentSelection = currentSelection;
        setPageComplete(false);
        setTitle(Messages.WizardProjectsImportPage_ImportProjectsTitle);
        setDescription(Messages.WizardProjectsImportPage_ImportProjectsDescription);
    }

    @Override
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        Composite workArea = new Composite(parent, SWT.NONE);
        setControl(workArea);

        workArea.setLayout(new GridLayout());
        workArea.setLayoutData(new GridData(GridData.FILL_BOTH
                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        createProjectsRoot(workArea);
        createProjectsList(workArea);
        createOptionsGroup(workArea);
        // createWorkingSetGroup(workArea);
        restoreWidgetValues();
        Dialog.applyDialogFont(workArea);

    }

    @Override
    protected void createOptionsGroupButtons(Group optionsGroup) {

        customRadioButton = EclipseUtils.createRadio(optionsGroup, Messages.Show_extensions_presented_in_custom_folder_only,
                (b, e) -> {
                    if (b.getSelection()) {
                        projectsListCheckbox.setFilters(customExtensionFilter);
                        ImportOption.currentOption = ImportOption.CUSTOM_ONLY;
                        createWorkingSets.setEnabled(false);
                    }
                });
        customRadioButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        notPlatformRadioButton = EclipseUtils.createRadio(optionsGroup, Messages.Show_all_except_for_platform_ext_extensions,
                (b, e) -> {
                    if (b.getSelection()) {
                        projectsListCheckbox.setFilters(localExtensionFilter);
                        ImportOption.currentOption = ImportOption.BIN_NOT_PLATFORM;
                        createWorkingSets.setEnabled(true);
                    }
                });

        notPlatformRadioButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        allRadioButton = EclipseUtils.createRadio(optionsGroup, Messages.Show_all_requied_extensions, (b, e) -> {
            if (b.getSelection()) {
                projectsListCheckbox.setFilters();
                ImportOption.currentOption = ImportOption.ALL;
                createWorkingSets.setEnabled(true);
            }
        });
        allRadioButton.setSelection(true);
        allRadioButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createWorkingSets = EclipseUtils.createCheckbox(optionsGroup, "Create working sets",
                (b, e) -> {
                    createWorkingSetsFlag = b.getSelection();
                });
        createWorkingSets.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createWorkingSets.setSelection(true);

        hideConflictingProjects = EclipseUtils.createCheckbox(optionsGroup, Messages.WizardProjectsImportPage_hideExistingProjects,
                (b, e) -> {
                    Display.getDefault().asyncExec(() -> {
                        projectsListCheckbox.removeFilter(conflictingProjectsFilter);
                        if (b.getSelection()) {
                            projectsListCheckbox.addFilter(conflictingProjectsFilter);
                        }
                    });
                });
        hideConflictingProjects.setSelection(true);
        hideConflictingProjects.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Dialog.applyDialogFont(optionsGroup);
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
        projectsListCheckbox.addCheckStateListener(event -> {
            ProjectRecord element = (ProjectRecord) event.getElement();
            if (element.hasConflicts) {
                projectsListCheckbox.setChecked(element, false);
            }
            setPageComplete(projectsListCheckbox.getCheckedElements().length > 0);
        });

        projectsListCheckbox.setInput(this);
        projectsListCheckbox.setComparator(new ViewerComparator());
        createSelectionButtons(listComposite);
    }

    /**
     * Create the selection buttons in the listComposite.
     *
     * @param listComposite
     */
    private void createSelectionButtons(Composite listComposite) {
        Composite buttonsComposite = new Composite(listComposite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        buttonsComposite.setLayout(layout);

        buttonsComposite.setLayoutData(new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING));

        Button selectAll = new Button(buttonsComposite, SWT.PUSH);
        selectAll.setText(Messages.DataTransfer_selectAll);
        selectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (ProjectRecord selectedProject : selectedProjects) {
                    if (selectedProject.hasConflicts) {
                        projectsListCheckbox.setChecked(selectedProject, false);
                    } else {
                        projectsListCheckbox.setChecked(selectedProject, true);
                    }
                }
                setPageComplete(projectsListCheckbox.getCheckedElements().length > 0);
            }
        });
        Dialog.applyDialogFont(selectAll);
        setButtonLayoutData(selectAll);

        Button deselectAll = new Button(buttonsComposite, SWT.PUSH);
        deselectAll.setText(Messages.DataTransfer_deselectAll);
        deselectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                projectsListCheckbox.setCheckedElements(new Object[0]);
                setPageComplete(false);
            }
        });
        Dialog.applyDialogFont(deselectAll);
        setButtonLayoutData(deselectAll);

        Button refresh = new Button(buttonsComposite, SWT.PUSH);
        refresh.setText(Messages.DataTransfer_refresh);
        refresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateProjectsList(directoryPathField.getText().trim(), true);
            }
        });
        Dialog.applyDialogFont(refresh);
        setButtonLayoutData(refresh);
    }

    /**
     * Create the area where you select the root directory for the projects.
     *
     * @param workArea
     *            Composite
     */
    private void createProjectsRoot(Composite workArea) {

        // project specification group
        Composite projectGroup = new Composite(workArea, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        projectGroup.setLayout(layout);
        projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // new project from directory radio button
        projectFromDirectoryRadio = new Label(projectGroup, SWT.NONE);
        projectFromDirectoryRadio
                .setText("Hybris platform directory:");

        // project location entry combo
        this.directoryPathField = new Combo(projectGroup, SWT.BORDER);

        GridData directoryPathData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
        directoryPathData.widthHint = new PixelConverter(directoryPathField).convertWidthInCharsToPixels(25);
        directoryPathField.setLayoutData(directoryPathData);

        // browse button
        browseDirectoriesButton = new Button(projectGroup, SWT.PUSH);
        browseDirectoriesButton
                .setText(Messages.DataTransfer_browse);
        setButtonLayoutData(browseDirectoriesButton);

        browseDirectoriesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleLocationDirectoryButtonPressed();
            }

        });

        directoryPathField.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_RETURN) {
                e.doit = false;
                updateProjectsList(directoryPathField.getText().trim());
            }
        });

        directoryPathField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(org.eclipse.swt.events.FocusEvent e) {
                updateProjectsList(directoryPathField.getText().trim());
            }

        });

        directoryPathField.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateProjectsList(directoryPathField.getText().trim());
            }
        });

    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.directoryPathField.setFocus();
    }

    /**
     * Update the list of projects based on path. Method declared public only
     * for test suite.
     *
     * @param path
     */
    public void updateProjectsList(final String path) {
        updateProjectsList(path, false);
    }

    private void updateProjectsList(final String hybrisPlatformPath, boolean forceUpdate) {
        // on an empty path empty selectedProjects
        if (hybrisPlatformPath == null || hybrisPlatformPath.length() == 0) {
            setMessage(Messages.WizardProjectsImportPage_ImportProjectsDescription);
            selectedProjects = new ProjectRecord[0];
            projectsListCheckbox.refresh(true);
            projectsListCheckbox.setCheckedElements(selectedProjects);
            setPageComplete(projectsListCheckbox.getCheckedElements().length > 0);
            lastPath = hybrisPlatformPath;
            return;
        }

        platformHome = new File(hybrisPlatformPath);
        if (hybrisPlatformPath.equals(lastPath) && !forceUpdate) {
            // unchanged; lastPath is updated here and in the refresh
            return;
        }

        // We can't access the radio button from the inner class so get the
        // status beforehand
        try {

            getContainer().run(true, true, monitor -> {

                monitor.beginTask(
                        Messages.WizardProjectsImportPage_SearchingMessage,
                        3);
                selectedProjects = new ProjectRecord[0];

                final Set<String> visitedExtensions = new HashSet<>();
                final List<ProjectRecord> records = new ArrayList<>();

                final Holder<IProgressMonitor> monitorHolder = new Holder<>();
                monitorHolder.set(monitor);

                new LocalExtensionVisitor(platformHome).visit(ext -> {
                    if (visitedExtensions.add(ext.getName())) {
                        records.add(new ProjectRecord(ext));
                        monitorHolder.get().worked(1);
                        if (ext.getRequiredExtensions().size() > 0) {
                            IProgressMonitor monitor0 = SubMonitor.convert(monitor, ext.getRequiredExtensions().size());
                            monitorHolder.set(monitor0);
                        }
                    }
                });
                if (visitedExtensions.add("platform")) {
                    records.add(new ProjectRecord(Extension.create(platformHome, platformHome)));
                    monitor.worked(1);
                }
                if (visitedExtensions.add("config")) {
                    File config = new File(platformHome.getParentFile().getParentFile(), "config");
                    records.add(new ProjectRecord(Extension.create(config, platformHome)));
                    monitor.worked(1);
                }

                selectedProjects = records.toArray(new ProjectRecord[records.size()]);

                monitor.done();
            });
        } catch (InvocationTargetException e) {
            IDEWorkbenchPlugin.log(e.getMessage(), e);
        } catch (InterruptedException e) {
            // Nothing to do if the user interrupts.
        }

        lastPath = hybrisPlatformPath;
        updateProjectsStatus();
    }

    private void updateProjectsStatus() {
        projectsListCheckbox.refresh(true);
        ProjectRecord[] projects = getProjectRecords();

        boolean displayConflictWarning = false;
        boolean displayInvalidWarning = false;

        for (ProjectRecord project : projects) {
            if (project.hasConflicts) {
                projectsListCheckbox.setGrayed(project, true);
                displayConflictWarning |= project.hasConflicts;
            } else {
                projectsListCheckbox.setChecked(project, true);
            }
        }

        if (displayConflictWarning && displayInvalidWarning) {
            setMessage(Messages.WizardProjectsImportPage_projectsInWorkspaceAndInvalid, WARNING);
        } else if (displayConflictWarning) {
            setMessage(Messages.WizardProjectsImportPage_projectsInWorkspace, WARNING);
        } else if (displayInvalidWarning) {
            setMessage(Messages.WizardProjectsImportPage_projectsInvalid, WARNING);
        } else {
            setMessage(Messages.WizardProjectsImportPage_ImportProjectsDescription);
        }
        setPageComplete(projectsListCheckbox.getCheckedElements().length > 0);
        if (selectedProjects.length == 0) {
            setMessage(
                    Messages.WizardProjectsImportPage_noProjectsToImport,
                    WARNING);
        }
    }

    /**
     * Collect the list of .project files that are under directory into files.
     *
     * @param files
     * @param directory
     * @param directoriesVisited
     *            Set of canonical paths of directories, used as recursion guard
     * @param nestedProjects
     *            whether to look for nested projects
     * @param monitor
     *            The monitor to report to
     * @return boolean <code>true</code> if the operation was completed.
     */
    static boolean collectProjectFilesFromDirectory(Collection<File> files, File directory,
            Set<String> directoriesVisited, boolean nestedProjects, IProgressMonitor monitor) {

        if (monitor.isCanceled()) {
            return false;
        }
        monitor.subTask(NLS.bind(Messages.WizardProjectsImportPage_CheckingMessage, directory.getPath()));
        File[] contents = directory.listFiles();
        if (contents == null) {
            return false;
        }

        // Initialize recursion guard for recursive symbolic links
        if (directoriesVisited == null) {
            directoriesVisited = new HashSet<>();
            try {
                directoriesVisited.add(directory.getCanonicalPath());
            } catch (IOException exception) {
                StatusManager.getManager().handle(
                        StatusUtil.newStatus(IStatus.ERROR, exception
                                .getLocalizedMessage(), exception));
            }
        }

        // first look for project description files
        final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
        List<File> directories = new ArrayList<>();
        for (File file : contents) {
            if (file.isDirectory()) {
                directories.add(file);
            } else if (file.getName().equals(dotProject) && file.isFile()) {
                files.add(file);
                if (!nestedProjects) {
                    // don't search sub-directories since we can't have nested
                    // projects
                    return true;
                }
            }
        }
        // no project description found or search for nested projects enabled,
        // so recurse into sub-directories
        for (File dir : directories) {
            if (!dir.getName().equals(METADATA_FOLDER)) {
                try {
                    String canonicalPath = dir.getCanonicalPath();
                    if (!directoriesVisited.add(canonicalPath)) {
                        // already been here --> do not recurse
                        continue;
                    }
                } catch (IOException exception) {
                    StatusManager.getManager().handle(
                            StatusUtil.newStatus(IStatus.ERROR, exception
                                    .getLocalizedMessage(), exception));

                }
                collectProjectFilesFromDirectory(files, dir,
                        directoriesVisited, nestedProjects, monitor);
            }
        }
        return true;
    }

    /**
     * The browse button has been selected. Select the location.
     */
    protected void handleLocationDirectoryButtonPressed() {

        DirectoryDialog dialog = new DirectoryDialog(directoryPathField
                .getShell(), SWT.SHEET);
        dialog
                .setMessage(Messages.WizardProjectsImportPage_SelectDialogTitle);

        String dirName = directoryPathField.getText().trim();
        if (dirName.length() == 0) {
            dirName = previouslyBrowsedDirectory;
        }

        if (dirName.length() == 0) {
            dialog.setFilterPath(ResourcesPlugin.getWorkspace()
                    .getRoot().getLocation().toOSString());
        } else {
            File path = new File(dirName);
            if (path.exists()) {
                dialog.setFilterPath(new Path(dirName).toOSString());
            }
        }

        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
            previouslyBrowsedDirectory = selectedDirectory;
            directoryPathField.setText(previouslyBrowsedDirectory);
            updateProjectsList(selectedDirectory);
        }

    }

    /**
     * Create the selected projects
     *
     * @return boolean <code>true</code> if all project creations were
     *         successful.
     */
    public boolean createProjects() {
        saveWidgetValues();

        final Object[] selected = projectsListCheckbox.getCheckedElements();
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            @Override
            protected void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                SubMonitor subMonitor = SubMonitor.convert(monitor, selected.length);
                // Import as many projects as we can; accumulate errors to
                // report to the user
                MultiStatus status = new MultiStatus(IDEWorkbenchPlugin.IDE_WORKBENCH, 1,
                        Messages.WizardProjectsImportPage_projectsInWorkspaceAndInvalid, null);

                ExtensionResolver.CACHE.clear();
                for (Object element : selected) {
                    try {
                        status.add(createExistingProject((ProjectRecord) element, subMonitor.split(1)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }

                if (!status.isOK()) {
                    throw new InvocationTargetException(new CoreException(status));
                }
            }
        };
        // run the new project creation operation
        try {
            getContainer().run(true, true, op);

        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            // one of the steps resulted in a core exception
            Throwable t = e.getTargetException();
            String message = Messages.WizardExternalProjectImportPage_errorMessage;
            IStatus status;
            if (t instanceof CoreException) {
                status = ((CoreException) t).getStatus();
            } else {
                status = new Status(IStatus.ERROR,
                        IDEWorkbenchPlugin.IDE_WORKBENCH, 1, message, t);
            }
            // Update the visible status on error so the user can see what's
            // been imported
            updateProjectsStatus();
            ErrorDialog.openError(getShell(), message, null, status);
            return false;
        } finally {
            ArchiveFileManipulations.closeStructureProvider(structureProvider, getShell());
        }
        return true;
    }

    /**
     * Performs clean-up if the user cancels the wizard without doing anything
     */
    public void performCancel() {
        ArchiveFileManipulations.closeStructureProvider(structureProvider,
                getShell());
    }

    /**
     * Create the project described in record.
     *
     * @param record
     * @return status of the creation
     * @throws InterruptedException
     * @throws CoreException
     */
    private IStatus createExistingProject(final ProjectRecord record, IProgressMonitor mon)
            throws InterruptedException {
        try {
            SubMonitor subMonitor = SubMonitor.convert(mon, 4);
            String projectName = record.getProjectName();

            new ExtensionFixer(record.extension).fix(SubMonitor.convert(mon));

            subMonitor.worked(1);
            IPath locationPath = new Path(record.extension.getProject().getAbsolutePath());
            subMonitor.worked(1);
            record.project = EclipseUtils.createProject(locationPath, projectName, subMonitor);

            subMonitor.worked(1);

            if (createWorkingSetsFlag) {
                SubMonitor subMonitor0 = SubMonitor.convert(mon, 3);
                subMonitor0.worked(1);

                IAdaptable adaptable = null != record.project.getAdapter(IJavaProject.class)
                        ? record.project.getAdapter(IJavaProject.class)
                        : record.project;

                if (record.extension.isCustom()) {
                    EclipseUtils.addProjectToWorkingSet(workbench, Messages.WorkingSet_Custom, adaptable);
                    subMonitor0.worked(1);
                } else {
                    EclipseUtils.addProjectToWorkingSet(workbench, Messages.WorkingSet_Platform, adaptable);
                    subMonitor0.worked(1);
                }
            }

            subMonitor.done();
        } catch (CoreException e) {
            return e.getStatus();
        }
        return Status.OK_STATUS;
    }

    /**
     * Method used for test suite.
     *
     * @return Button the Import from Directory RadioButton
     */
    public Label getProjectFromDirectoryRadio() {
        return projectFromDirectoryRadio;
    }

    /**
     * Method used for test suite.
     *
     * @return CheckboxTreeViewer the viewer containing all the projects found
     */
    public CheckboxTreeViewer getProjectsList() {
        return projectsListCheckbox;
    }

    /**
     * Retrieve all the projects in the current workspace.
     *
     * @return IProject[] array of IProject in the current workspace
     */
    private IProject[] getProjectsInWorkspace() {
        return ResourcesPlugin.getWorkspace().getRoot()
                .getProjects();
    }

    /**
     * Get the array of project records that can be imported from the source
     * workspace or archive, selected by the user. If a project with the same
     * name exists in both the source workspace and the current workspace, then
     * the hasConflicts flag would be set on that project record.
     *
     * Method declared public for test suite.
     *
     * @return ProjectRecord[] array of projects that can be imported into the
     *         workspace
     */
    public ProjectRecord[] getProjectRecords() {
        List<ProjectRecord> projectRecords = new ArrayList<>();
        for (int i = 0; i < selectedProjects.length; i++) {
            String projectName = selectedProjects[i].getProjectName();
            selectedProjects[i].hasConflicts = isProjectInWorkspace(projectName);
            projectRecords.add(selectedProjects[i]);
        }
        return projectRecords
                .toArray(new ProjectRecord[projectRecords.size()]);
    }

    /**
     * Determine if there is a directory with the project name in the workspace
     * path.
     *
     * @param projectName
     *            the name of the project
     * @return true if there is a directory with the same name of the imported
     *         project
     */
    private boolean isProjectInWorkspacePath(String projectName) {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath wsPath = workspace.getRoot().getLocation();
        IPath localProjectPath = wsPath.append(projectName);
        return localProjectPath.toFile().exists();
    }

    /**
     * Determine if the project with the given name is in the current workspace.
     *
     * @param projectName
     *            String the project name to check
     * @return boolean true if the project with the given name is in this
     *         workspace
     */
    private boolean isProjectInWorkspace(String projectName) {
        if (projectName == null) {
            return false;
        }
        IProject[] workspaceProjects = getProjectsInWorkspace();
        for (IProject workspaceProject : workspaceProjects) {
            if (projectName.equals(workspaceProject.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Use the dialog store to restore widget values to the values that they
     * held last time this wizard was used to completion, or alternatively, if
     * an initial path is specified, use it to select values.
     *
     * Method declared public only for use of tests.
     */
    @Override
    public void restoreWidgetValues() {

        // First, check to see if we have resore settings, and
        // take care of the checkbox
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            restoreFromHistory(settings, STORE_DIRECTORIES, directoryPathField);
        }

        // Third, if we do have an initial path, set the proper
        // path and radio buttons to the initial value. Move
        // cursor to the end of the path so user can see the
        // most relevant part (directory / archive name)
        else if (initialPath != null) {
            directoryPathField.setText(initialPath);
            directoryPathField.setSelection(new Point(initialPath.length(), initialPath.length()));
        }
    }

    private void restoreFromHistory(IDialogSettings settings, String key, Combo combo) {
        String[] sourceNames = settings.getArray(key);
        if (sourceNames == null) {
            return; // ie.- no values stored, so stop
        }

        for (String sourceName : sourceNames) {
            combo.add(sourceName);
        }
    }

    /**
     * Since Finish was pressed, write widget values to the dialog store so that
     * they will persist into the next invocation of this wizard page.
     *
     * Method declared public only for use of tests.
     */
    @Override
    public void saveWidgetValues() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            saveInHistory(settings, STORE_DIRECTORIES, directoryPathField.getText());
        }
    }

    private void saveInHistory(IDialogSettings settings, String key, String value) {
        String[] sourceNames = settings.getArray(key);
        if (sourceNames == null) {
            sourceNames = new String[0];
        }
        sourceNames = addToHistory(sourceNames, value);
        settings.put(key, sourceNames);
    }

    @Override
    public void handleEvent(Event event) {
    }

    @Override
    protected boolean allowNewContainerName() {
        return true;
    }

}
