package hybristools.utils;

import java.util.function.BiConsumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;

import hybris.messages.Messages;

public class EclipseUtils {

    public static Button createRadio(Composite parent, String label, BiConsumer<Button, SelectionEvent> handler) {
        return createButton(SWT.RADIO, parent, label, handler);
    }

    public static Button createCheckbox(Composite parent, String label, BiConsumer<Button, SelectionEvent> handler) {
        return createButton(SWT.CHECK, parent, label, handler);
    }

    public static Button createButton(Composite parent, String label, BiConsumer<Button, SelectionEvent> handler) {
        return createButton(SWT.BUTTON1, parent, label, handler);
    }

    public static Button createButton(int type, Composite parent, String label, BiConsumer<Button, SelectionEvent> handler) {
        final Button button = new Button(parent, type);
        button.setText(label);
        button.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handler.accept(button, e);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        return button;
    }

    public static Label createLeftLabel(Composite parent, String string) {
        Label label2 = new Label(parent, SWT.LEFT);
        label2.setText(string);
        return label2;
    }

    public static Text createLeftText(Composite workArea, final BiConsumer<Text, KeyEvent> handler) {
        Text text = new Text(workArea, SWT.LEFT | SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (null != handler) {
            text.addKeyListener(new KeyListener() {

                @Override
                public void keyReleased(KeyEvent e) {
                    handler.accept(text, e);
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    handler.accept(text, e);
                }
            });
        }
        return text;
    }

    public static IProject createProject(IPath locationPath, String projectName, SubMonitor monitor)
            throws OperationCanceledException, CoreException {

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProject project = workspace.getRoot().getProject(projectName);
        IProjectDescription description = null;
        if (project.exists()) {
            description = workspace.loadProjectDescription(locationPath);
        } else {
            description = workspace.newProjectDescription(projectName);
            description.setLocation(locationPath);
        }

        SubMonitor subTask = monitor.split(1).setWorkRemaining(100);
        subTask.setTaskName(Messages.WizardProjectsImportPage_CreateProjectsTask);
        project.create(description, monitor.split(30));
        project.open(IResource.BACKGROUND_REFRESH, subTask.split(70));
        return project;
    }

    public static void addProjectToWorkingSet(IWorkbench workbench, String workingSet, IAdaptable project) {
        IWorkingSetManager wsManager = workbench.getWorkingSetManager();
        IWorkingSet customWS = wsManager.getWorkingSet(workingSet);
        if (null == customWS) {
            customWS = wsManager.createWorkingSet(workingSet, new IAdaptable[0]);
            wsManager.addWorkingSet(customWS);
        }
        wsManager.addToWorkingSets(project, new IWorkingSet[] { customWS });
    }
}
