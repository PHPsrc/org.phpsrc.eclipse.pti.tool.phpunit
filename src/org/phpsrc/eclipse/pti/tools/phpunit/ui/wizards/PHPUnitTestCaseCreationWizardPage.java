/*******************************************************************************
 * Copyright (c) 2010, Sven Kiera
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Organisation nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.ui.wizards;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.core.search.SearchMatch;
import org.eclipse.dltk.internal.core.SourceType;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.phpsrc.eclipse.pti.core.IPHPCoreConstants;
import org.phpsrc.eclipse.pti.core.PHPToolkitUtil;
import org.phpsrc.eclipse.pti.core.search.PHPSearchEngine;
import org.phpsrc.eclipse.pti.core.search.PHPSearchMatch;
import org.phpsrc.eclipse.pti.core.search.ui.dialogs.FilteredPHPClassSelectionDialog;
import org.phpsrc.eclipse.pti.tools.phpunit.IPHPUnitConstants;
import org.phpsrc.eclipse.pti.tools.phpunit.core.preferences.PHPUnitPreferences;
import org.phpsrc.eclipse.pti.tools.phpunit.core.preferences.PHPUnitPreferencesFactory;
import org.phpsrc.eclipse.pti.tools.phpunit.ui.preferences.PHPUnitConfigurationBlock;
import org.phpsrc.eclipse.pti.ui.Logger;

public class PHPUnitTestCaseCreationWizardPage extends WizardPage {

	protected Text classText;
	protected IFile classFile;
	protected Text containerText;
	protected Text fileText;
	private IStructuredSelection selection;
	protected IProject project;

	protected static final String UTF_8 = "UTF 8"; //$NON-NLS-1$
	protected static final String NO_TEMPLATE = "-- none -- "; //$NON-NLS-1$
	protected Label targetResourceLabel;

	protected boolean testFileExists = false;

	public PHPUnitTestCaseCreationWizardPage(final IStructuredSelection selection) {
		super("wizardPage"); //$NON-NLS-1$
		setTitle("New PHPUnit Test Case");
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		final Group classGroup = new Group(container, SWT.RESIZE);
		classGroup.setText("Source");

		final GridLayout classLayout = new GridLayout();
		classLayout.numColumns = 3;
		classLayout.verticalSpacing = 10;
		classGroup.setLayout(classLayout);
		classGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label classLabel = new Label(classGroup, SWT.NULL);
		classLabel.setText("Class Name");

		classText = new Text(classGroup, SWT.BORDER | SWT.SINGLE);
		classText.setEditable(false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		classText.setLayoutData(gd);
		classText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				dialogChanged();
			}
		});

		final Button sourceButton = new Button(classGroup, SWT.PUSH);
		sourceButton.setText("Search...");
		sourceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				handleClassSearch();
			}
		});

		final Group fileGroup = new Group(container, SWT.RESIZE);
		fileGroup.setText("Test");

		final GridLayout fileLayout = new GridLayout();
		fileLayout.numColumns = 3;
		fileLayout.verticalSpacing = 10;
		fileGroup.setLayout(fileLayout);
		fileGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label fileLabel = new Label(fileGroup, SWT.NULL);
		fileLabel.setText("Source Folder");

		containerText = new Text(fileGroup, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				dialogChanged();
			}
		});

		final Button containerButton = new Button(fileGroup, SWT.PUSH);
		containerButton.setText("Browse...");
		containerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				handleContainerBrowse();
			}
		});

		targetResourceLabel = new Label(fileGroup, SWT.NULL);
		targetResourceLabel.setText("File Name");

		fileText = new Text(fileGroup, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fileText.setLayoutData(gd);
		fileText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				dialogChanged();
			}
		});

		initialize();
		dialogChanged();
		setControl(container);

		if (this.selection != null) {
			Object element = this.selection.getFirstElement();
			ISourceModule module = PHPToolkitUtil.getSourceModule(element);
			if (module != null) {
				IType[] types;
				try {
					types = module.getAllTypes();
					if (types != null && types.length > 0) {
						this.setSourceClassName(types[0].getElementName(), types[0].getResource());
					}
				} catch (ModelException e1) {
					Logger.logException(e1);
				}
			}
		}
	}

	private void setSourceClass(SearchMatch match) {
		setSourceClass((SourceType) match.getElement(), match.getResource());
	}

	private void setSourceClass(PHPSearchMatch match) {
		setSourceClass(match.getElement(), match.getResource());
	}

	private void setSourceClass(SourceType type, IResource resource) {
		classText.setText(type.getElementName());
		classFile = (IFile) resource;

		String patternFolder = null;
		String patternFile = null;

		PHPUnitPreferences prefs = PHPUnitPreferencesFactory.factory(classFile);
		if (prefs != null) {
			patternFolder = prefs.getTestFilePatternFolder();
			patternFile = prefs.getTestFilePatternFile();
		}

		if (patternFolder == null)
			patternFolder = PHPUnitConfigurationBlock.TEST_FILE_PATTERN_FOLDER_DEFAULT;

		String patternProject = "";
		String patternPath = "";

		String path = type.getPath().toOSString();
		if (path.indexOf(File.separatorChar, 1) >= 0) {
			patternProject = path.substring(1, path.indexOf(File.separatorChar, 1));
			patternPath = path.substring(path.indexOf(File.separatorChar, 1) + 1, path.lastIndexOf(File.separatorChar));
		} else {
			patternProject = path;
		}

		patternFolder = patternFolder.replace(IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_PROJECT, patternProject);
		patternFolder = patternFolder.replace(IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_DIR, patternPath);
		containerText.setText(patternFolder);

		if (patternFile == null)
			patternFile = PHPUnitConfigurationBlock.TEST_FILE_PATTERN_FILE_DEFAULT;

		String fileName = resource.getName();
		int firstDotPos = fileName.indexOf(".");
		int lastDotPos = fileName.lastIndexOf(".");
		patternFile = patternFile.replace(IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILENAME, fileName.substring(
				0, firstDotPos));
		patternFile = patternFile.replace(IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILE_EXTENSION, fileName
				.substring(lastDotPos + 1));
		fileText.setText(patternFile);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */
	private void initialize() {
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
			final IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1) {
				return;
			}

			Object obj = ssel.getFirstElement();
			if (obj instanceof IAdaptable) {
				obj = ((IAdaptable) obj).getAdapter(IResource.class);
			}

			IContainer container = null;
			if (obj instanceof IResource) {
				if (obj instanceof IContainer) {
					container = (IContainer) obj;
				} else {
					container = ((IResource) obj).getParent();
				}
			}

			if (container != null) {
				containerText.setText(container.getFullPath().toString());
				this.project = container.getProject();
			}
		}
	}

	protected void setInitialFileName(final String fileName) {
		fileText.setText(fileName);
		// fixed bug 157145 - highlight the newfile word in the file name input
		fileText.setSelection(0, 7);
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */

	private void handleContainerBrowse() {
		final ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace()
				.getRoot(), false, "Select Test File Folder");
		dialog.showClosedProjects(false);
		if (dialog.open() == Window.OK) {
			final Object[] result = dialog.getResult();
			if (result.length == 1)
				containerText.setText(((Path) result[0]).toOSString());
		}
	}

	private void handleClassSearch() {
		FilteredPHPClassSelectionDialog dialog = new FilteredPHPClassSelectionDialog(getShell(), false);
		if (dialog.open() == Window.OK) {
			PHPSearchMatch result = (PHPSearchMatch) dialog.getFirstResult();
			this.setSourceClass(result);
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */
	protected void dialogChanged() {
		testFileExists = false;

		final String container = getContainerName();
		final String fileName = getFileName();

		if (container.length() == 0) {
			updateStatus("Folder must be specified");
			return;
		}

		final IContainer containerFolder = getContainer(container);
		if (containerFolder == null || !containerFolder.exists()) {
			setMessage("Selected folder does not exist and will be created", WizardPage.INFORMATION);
		} else {
			if (!containerFolder.getProject().isOpen()) {
				updateStatus("Selected folder is in a closed project");
				return;
			}
			if (fileName != null && !fileName.equals("") && containerFolder.getFile(new Path(fileName)).exists()) { //$NON-NLS-1$
				setMessage("File exists and will be combined", WizardPage.INFORMATION);
				testFileExists = true;
			}
		}

		this.project = null;
		if (container != null && container.length() > 0 && container.indexOf(java.io.File.separatorChar) >= 0) {
			IContainer projectContainer = getContainer(container.substring(0, container.indexOf(
					java.io.File.separatorChar, 1)));
			if (projectContainer != null)
				this.project = projectContainer.getProject();
		}

		if (this.project == null) {
			updateStatus("Project does not exist");
			return;
		}

		int dotIndex = fileName.lastIndexOf('.');
		if (fileName.length() == 0 || dotIndex == 0) {
			updateStatus("File name must be specified");
			return;
		}

		if (dotIndex != -1) {
			String fileNameWithoutExtention = fileName.substring(0, dotIndex);
			for (int i = 0; i < fileNameWithoutExtention.length(); i++) {
				char ch = fileNameWithoutExtention.charAt(i);
				if (!(Character.isJavaIdentifierPart(ch) || ch == '.' || ch == '-')) {
					updateStatus("File name contains illegal characters");
					return;
				}
			}
		}

		final IContentType contentType = Platform.getContentTypeManager().getContentType(
				IPHPCoreConstants.ContentTypeID_PHP);
		if (!contentType.isAssociatedWith(fileName)) {
			// fixed bug 195274
			// get the extensions from content type
			final String[] fileExtensions = contentType.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
			StringBuffer buffer = new StringBuffer("The file name must end in one of the following extensions [");
			buffer.append(fileExtensions[0]);
			for (String extension : fileExtensions) {
				buffer.append(", ").append(extension); //$NON-NLS-1$
			}
			buffer.append("]"); //$NON-NLS-1$
			updateStatus(buffer.toString());
			return;
		}

		updateStatus(null);
	}

	protected IContainer getContainer(final String text) {
		final Path path = new Path(text);

		final IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		return resource instanceof IContainer ? (IContainer) resource : null;

	}

	protected void updateStatus(final String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public boolean setSourceClassName(String className) {
		return setSourceClassName(className, PHPSearchEngine.createWorkspaceScope(), null);
	}

	public boolean setSourceClassName(String className, IResource classFile) {
		return setSourceClassName(className, PHPSearchEngine.createProjectScope(classFile.getProject()), classFile);
	}

	public boolean setSourceClassName(String className, IDLTKSearchScope scope) {
		return setSourceClassName(className, scope, null);
	}

	protected boolean setSourceClassName(String className, IDLTKSearchScope scope, IResource classFile) {
		SearchMatch[] matches = PHPSearchEngine.findClass(className, scope);

		if (matches.length > 0) {
			for (SearchMatch match : matches) {
				if (classFile != null || match.getResource().equals(classFile)) {
					setSourceClass(match);
					return true;
				}
			}

			// no file found, so use first match
			setSourceClass(matches[0]);
			return true;
		}

		return false;
	}

	public String getSourceClassName() {
		return classText.getText();
	}

	public IFile getSourceClassFile() {
		return classFile;
	}

	public String getContainerName() {
		return containerText.getText();
	}

	public String getFileName() {
		return fileText.getText();
	}

	public String getTestClassFilePath() {
		return getContainerName() + "\\" + getFileName();
	}

	public IProject getProject() {
		return project;
	}

	public boolean finish() {
		return true;
	}
}