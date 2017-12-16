package com.xl0e.hybris.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStreamMonitor;
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

public abstract class AbstractCommand {
    private static Map<String, ILaunch> LAUNCH_MAP = new HashMap<>();
    private static final Map<String, Integer> ESTIMATION_MAP = new HashMap<>();
    private static ProcessConsoleManager CONSOLE_MANAGER = new ProcessConsoleManager();

    protected final IPath platformPath;
    protected final List<String> arguments;
    protected final String[] attrNameAndValues;

    public AbstractCommand(IPath platformPath, List<String> arguments, String[] attrNameAndValues) {
        this.platformPath = platformPath;
        this.arguments = arguments;
        this.attrNameAndValues = attrNameAndValues;
    }

    protected abstract String getGroupName();

    protected abstract String getApplicationPath();

    protected abstract ILaunch createLaunch(String string);

    private ILaunch getOrCreateLaunch() {
        return LAUNCH_MAP.computeIfAbsent(getGroupName(), this::createLaunch);
    }

    @SuppressWarnings({ "restriction", "unused" })
    public int accept(File ext, IProgressMonitor monitor) {
        try {
            final List<String> processCommands = new ArrayList<>();
            processCommands.add(getApplicationPath());
            processCommands.addAll(arguments);
            final String commandName = String.join(" ", processCommands);
            final int estimated = ESTIMATION_MAP.getOrDefault(commandName, 100);
            final SubMonitor subMonitor = SubMonitor.convert(monitor, "Running " + commandName + " ...", estimated);
            final int[] elapsedTime = new int[] { 0 };
            final List<String> envsList = new ArrayList<>();
            System.getenv().forEach((k, v) -> {
                envsList.add(k + '=' + v);
            });

            final Process antProccess = Runtime.getRuntime().exec(processCommands.toArray(new String[processCommands.size()]),
                    envsList.toArray(new String[envsList.size()]),
                    ext);

            final Map<String, String> attributeMap = new HashMap<>();
            for (int i = 0; i < attrNameAndValues.length; i = i + 2) {
                attributeMap.put(attrNameAndValues[i], attrNameAndValues[i + 1]);
            }
            attributeMap.putIfAbsent(IProcess.ATTR_PROCESS_TYPE, IJavaLaunchConfigurationConstants.ID_JAVA_PROCESS_TYPE);
            final ILaunch launch = getOrCreateLaunch();
            RuntimeProcess process = null;
            int result = -1;
            try {
                process = new RuntimeProcess(launch, antProccess, commandName, attributeMap);
                attachStreamListener(process, subMonitor);
                CONSOLE_MANAGER.launchChanged(launch);
                result = antProccess.waitFor();
                subMonitor.done();
                ESTIMATION_MAP.put(commandName, elapsedTime[0]);
            } finally {
                Optional.ofNullable(process).ifPresent(p -> launch.removeProcess(p));
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
        return -1;
    }

    private static void attachStreamListener(final IProcess process, final SubMonitor subMonitor) {
        final IStreamListener listener = new IStreamListener() {
            @Override
            public void streamAppended(String text, IStreamMonitor monitor) {
                if (subMonitor.isCanceled() && process.canTerminate()) {
                    monitor.removeListener(this);
                    try {
                        process.terminate();
                    } catch (DebugException e) {
                        e.printStackTrace();
                    }
                    subMonitor.done();
                    return;
                }
                subMonitor.worked(1);
                subMonitor.setTaskName(trim(text, 100));
            }
        };
        process.getStreamsProxy().getOutputStreamMonitor().addListener(listener);
    }

    private static String trim(String string, int toLength) {
        if (null != string && string.length() > toLength) {
            return string.substring(0, toLength) + "...";
        }
        return string;
    }

    protected static ISourceLocator getSourceLocator() {
        ArrayList<IProjectNature> tempList = new ArrayList<>();
        StringBuffer traceBuffer = new StringBuffer();
        try {
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
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String getScriptExtension() {
        return null == System.getenv("ComSpec") ? "sh" : "bat";
    }
}
