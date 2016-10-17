package hybris.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.debug.core.sourcelookup.containers.DefaultSourceContainer;
import org.eclipse.debug.internal.ui.views.console.ProcessConsoleManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;

import hybris.messages.Messages;

public class RunServerHandler extends AbstractHandler {

    private static ProcessConsoleManager consoleManager = new ProcessConsoleManager();

    private String getExt() {
        return null == System.getenv("ComSpec") ? "sh" : "bat";
    }

    protected List<String> getCommands(IPath command) {
        return Arrays.asList(command.toOSString());
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();

        final IProject platformProject = workspace.getRoot().getProject("platform");

        if (platformProject.exists()) {

            Job job = new Job(Messages.WizardProjectsImportPage_runningAntAll) {
                @Override
                public IStatus run(IProgressMonitor monitor) {
                    try {

                        IPath path = platformProject.getLocation().makeAbsolute().append("/hybrisserver." + getExt());
                        List<String> commands = getCommands(path);
                        Process proccess = new ProcessBuilder(commands).directory(platformProject.getLocation().toFile())
                                .redirectErrorStream(true).start();

                        ILaunch launch = new Launch(null, ILaunchManager.RUN_MODE, getSourceLocator());

                        IProcess procc = new RuntimeProcess(launch, proccess, "Hybris server", null);
                        procc.setAttribute(IProcess.ATTR_PROCESS_TYPE, IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION);
                        consoleManager.launchAdded(launch);
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

        }
        return null;
    }

    private static ISourceLocator getSourceLocator() throws CoreException {
        ArrayList<IProjectNature> tempList = new ArrayList<>();
        StringBuffer traceBuffer = new StringBuffer();

        traceBuffer.append("Projects in source path :\n");
        for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            traceBuffer.append("Project " + project.getName());
            if ((project.isOpen()) && project.hasNature(JavaCore.NATURE_ID)) {
                tempList.add(project.getNature(JavaCore.NATURE_ID));
                traceBuffer.append(" added to tempList\n");
            }
        }

        ISourceLookupDirector sourceLocator = new JavaSourceLookupDirector();
        ISourcePathComputer computer = DebugPlugin.getDefault().getLaunchManager()
                .getSourcePathComputer("org.eclipse.jdt.launching.sourceLookup.javaSourcePathComputer");
        sourceLocator.setSourcePathComputer(computer); // $NON-NLS-1$

        List<ISourceContainer> sourceContainers = new ArrayList<>();

        if (!tempList.isEmpty()) {
            IJavaProject[] javaProjects = tempList.toArray(new IJavaProject[1]);
            // sourceLocator = new JavaSourceLocator(javaProjects, true);

            // Eclipse stops looking for source if it finds a jar containing the
            // source code
            // despite this jar as no attached source (the user will have to use
            // 'Attach source' button).
            // So we have to enforce that sources in project are searched before
            // jar files,
            // To do so we add source containers in this orders :
            // - First project source containers.
            // - second packageFragmentRoot container (jar files in projects
            // build path will be added to source path)
            // - third DefaultSourceContainer (jar files added to classpath will
            // be added to source path)

            // First add all projects source containers
            for (int i = 0; i < javaProjects.length; i++) {
                IJavaProject project = javaProjects[i];
                traceBuffer.append("  -> Add JavaProjectSourceContainer for " + project.getProject().getName() + "\n");
                sourceContainers.add(new JavaProjectSourceContainer(project));
            }

            // Adding packageFragmentRoot source containers, so classes in jar
            // files associated to a project will be seen
            Set<IPath> external = new HashSet<>();

            for (int i = 0; i < javaProjects.length; i++) {
                IJavaProject project = javaProjects[i];
                traceBuffer.append("  -> Compute SourceContainers for " + project.getProject().getName() + " :\n");

                IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
                for (int ri = 0; ri < roots.length; ri++) {
                    IPackageFragmentRoot root = roots[ri];
                    if (root.isExternal()) {
                        IPath location = root.getPath();
                        if (external.contains(location)) {
                            continue;
                        }
                        external.add(location);
                    }
                    sourceContainers.add(new PackageFragmentRootSourceContainer(root));
                    traceBuffer.append("     RootSourceContainer created for : " + root.getPath().toPortableString() + "\n");
                }
            }
        }

        // Last add DefaultSourceContainer, classes in jar files added to
        // classpath will be visible
        sourceContainers.add(new DefaultSourceContainer());

        sourceLocator.setSourceContainers(sourceContainers.toArray(new ISourceContainer[sourceContainers.size()]));
        sourceLocator.initializeParticipants();

        System.out.println(traceBuffer.toString());
        return sourceLocator;
    }

}
