package hybris.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import hybris.ant.ImportOption;
import hybris.extension.Extension;
import hybris.extension.ExtensionFixer;
import hybris.extension.ExtensionResolver;
import hybris.messages.Messages;

public abstract class AbstractClasspathFixerHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProject platformProject = workspace.getRoot().getProject("platform");
        final List<IClasspathEntry> classpathEntries = new ArrayList<>(100);
        try {
            final IJavaProject javaProject = (IJavaProject) platformProject.getNature(JavaCore.NATURE_ID);
            classpathEntries.addAll(Arrays.asList(javaProject.getRawClasspath()));
        } catch (CoreException e1) {
            e1.printStackTrace();
        }
        if (platformProject.exists()) {
            ImportOption.currentOption = null;

            final IProject[] projects = workspace.getRoot().getProjects();
            Job job = new Job(Messages.WizardProjectsImportPage_fixClasspath) {

                @Override
                public IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask(Messages.WizardProjectsImportPage_runningAntAll, projects.length);
                        ExtensionResolver.CACHE.clear();
                        for (IProject project : projects) {
                            if (!monitor.isCanceled()) {
                                new ExtensionFixer(Extension.create(project.getLocation().toFile(), platformProject.getLocation().toFile()))
                                        .fix(SubMonitor.convert(monitor));
                                monitor.worked(1);
                            } else {
                                return Status.CANCEL_STATUS;
                            }
                        }

                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return new Status(IStatus.ERROR, "hybris", e.getMessage(), e);
                    } finally {
                        monitor.done();
                    }
                }

            };
            job.setRule(ResourcesPlugin.getWorkspace().getRoot());
            job.schedule();

            Job job2 = new Job(Messages.WizardExternalProjectImportPage_refreshingWorkspace) {
                @Override
                public IStatus run(IProgressMonitor monitor) {
                    try {
                        workspace.getRoot().refreshLocal(2, monitor);
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return new Status(IStatus.ERROR, "hybris", e.getMessage(), e);
                    }
                }
            };

            job2.setRule(ResourcesPlugin.getWorkspace().getRoot());
            job2.schedule();

        }
        return null;

    }

    protected abstract boolean isExcludeTestClasses();

    protected abstract boolean isDisableAntBuilder();
}
