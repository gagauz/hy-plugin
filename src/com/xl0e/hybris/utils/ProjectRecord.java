package com.xl0e.hybris.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;

import com.xl0e.hybris.extension.Extension;
import com.xl0e.hybris.messages.Messages;

/**
 * Class declared public only for test suite.
 *
 */
public class ProjectRecord implements Comparable<ProjectRecord> {

    private Extension extension;

    private String projectName;

    private boolean hasConflicts;

    private IProject project;

    /**
     * Create a record for a project based on the info in the file.
     *
     * @param file
     */
    public ProjectRecord(Extension ext) {
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
     * Gets the label to be used when rendering this project record in the UI.
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

    public Extension getExtension() {
        return extension;
    }

    public void setExtension(Extension extension) {
        this.extension = extension;
    }

    public boolean isHasConflicts() {
        return hasConflicts;
    }

    public void setHasConflicts(boolean hasConflicts) {
        this.hasConflicts = hasConflicts;
    }

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public int compareTo(ProjectRecord o) {
        return getProjectName().compareTo(o.getProjectName());
    }
}
