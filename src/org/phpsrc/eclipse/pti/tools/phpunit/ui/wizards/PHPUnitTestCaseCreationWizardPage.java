/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.ui.wizards;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

@SuppressWarnings("restriction")
public class PHPUnitTestCaseCreationWizardPage extends WizardPage {

	protected Text fClassPath;
	protected Text fClassName;
	protected IFile fClassFile;
	protected Text fTestClassName;
	protected Text fContainer;
	protected Text fFile;
	protected Text fSuperClass;
	private IStructuredSelection selection;
	protected IProject project;

	protected static final String UTF_8 = "UTF 8"; //$NON-NLS-1$
	protected static final String NO_TEMPLATE = "-- none -- "; //$NON-NLS-1$
	protected Label targetResourceLabel;

	protected PHPUnitPreferences preferences;
	protected boolean testFileExists = false;

	private interface IClassSearchListener {
		public void handleSearchMatch(PHPSearchMatch match);
	};

	public PHPUnitTestCaseCreationWizardPage(
			final IStructuredSelection selection) {
		super("wizardPage"); //$NON-NLS-1$
		setTitle("New PHPUnit Test Case");
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(final Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		// ###### Source ######

		Group classGroup = new Group(container, SWT.RESIZE);
		classGroup.setText("Source");

		GridLayout classLayout = new GridLayout();
		classLayout.numColumns = 3;
		classLayout.verticalSpacing = 10;
		classGroup.setLayout(classLayout);
		classGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// ### Source File ###

		Label classFolderLabel = new Label(classGroup, SWT.NULL);
		classFolderLabel.setText("Source File:");

		fClassPath = new Text(classGroup, SWT.BORDER | SWT.SINGLE);
		fClassPath.setEditable(false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fClassPath.setLayoutData(gd);

		// ### Class to test ###

		Label classLabel = new Label(classGroup, SWT.NULL);
		classLabel.setText("Class to test:");

		fClassName = new Text(classGroup, SWT.BORDER | SWT.SINGLE);
		fClassName.setEditable(false);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fClassName.setLayoutData(gd);
		fClassName.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				dialogChanged();
			}
		});

		Button sourceButton = new Button(classGroup, SWT.PUSH);
		sourceButton.setText("Search...");
		sourceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				handleClassSearch(new IClassSearchListener() {
					public void handleSearchMatch(PHPSearchMatch match) {
						setSourceClass(match);
					}
				});
			}
		});

		// ###### Test ######

		Group fileGroup = new Group(container, SWT.RESIZE);
		fileGroup.setText("Test Case");

		GridLayout fileLayout = new GridLayout();
		fileLayout.numColumns = 3;
		fileLayout.verticalSpacing = 10;
		fileGroup.setLayout(fileLayout);
		fileGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// ### Test Class ###

		Label testClassNameLabel = new Label(fileGroup, SWT.NULL);
		testClassNameLabel.setText("Class Name:");

		fTestClassName = new Text(fileGroup, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fTestClassName.setLayoutData(gd);

		// ### SuperClass ###

		Label superClassLabel = new Label(fileGroup, SWT.NULL);
		superClassLabel.setText("SuperClass:");

		fSuperClass = new Text(fileGroup, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fSuperClass.setLayoutData(gd);

		Button superClassButton = new Button(fileGroup, SWT.PUSH);
		superClassButton.setText("Search...");
		superClassButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				handleClassSearch(new IClassSearchListener() {
					public void handleSearchMatch(PHPSearchMatch match) {
						fSuperClass
								.setText(match.getElement().getElementName());
					}
				});
			}
		});

		// ### Target Folder ###

		Label fileLabel = new Label(fileGroup, SWT.NULL);
		fileLabel.setText("Target Folder:");
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.verticalIndent = 4;
		fileLabel.setLayoutData(gd);

		Composite folderLabelGroup = new Composite(fileGroup, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		folderLabelGroup.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalIndent = 2;
		folderLabelGroup.setLayoutData(gd);

		fContainer = new Text(folderLabelGroup, SWT.BORDER | SWT.SINGLE);
		fContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fContainer.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				dialogChanged();
			}
		});

		Label folderInfoLabel = new Label(folderLabelGroup, SWT.NULL);
		folderInfoLabel.setText("Path must start with a project like "
				+ File.separatorChar + "<project>" + File.separatorChar
				+ "<folder 1>" + File.separatorChar + "<folder 2>");
		folderInfoLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		makeFontItalic(folderInfoLabel);

		Button containerButton = new Button(fileGroup, SWT.PUSH);
		containerButton.setText("Browse...");
		containerButton.setLayoutData(new GridData(
				GridData.VERTICAL_ALIGN_BEGINNING));
		containerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				handleContainerBrowse();
			}
		});

		// ### File Name ###

		targetResourceLabel = new Label(fileGroup, SWT.NULL);
		targetResourceLabel.setText("File Name:");

		fFile = new Text(fileGroup, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fFile.setLayoutData(gd);
		fFile.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				dialogChanged();
			}
		});

		initialize();
		dialogChanged();
		setControl(container);
	}

	private void setSourceClass(SearchMatch match) {
		setSourceClass((SourceType) match.getElement(), match.getResource());
	}

	private void setSourceClass(PHPSearchMatch match) {
		setSourceClass(match.getElement(), match.getResource());
	}

	private void setSourceClass(SourceType type, IResource resource) {
		Assert.isNotNull(type);
		Assert.isNotNull(resource);

		fClassName.setText(type.getElementName());
		fClassPath.setText(resource.getFullPath().toOSString());
		fClassFile = (IFile) resource;
		fTestClassName.setText(type.getElementName() + "Test");

		String patternFolder = null;
		String patternFile = null;

		preferences = PHPUnitPreferencesFactory.factory(fClassFile);
		if (preferences != null) {
			patternFolder = preferences.getTestFilePatternFolder();
			patternFile = preferences.getTestFilePatternFile();
		}

		if (patternFolder == null)
			patternFolder = PHPUnitConfigurationBlock.TEST_FILE_PATTERN_FOLDER_DEFAULT;

		String patternProject = "";
		String patternPath = "";

		String path = type.getPath().toOSString();
		int firstSeparator = path.indexOf(File.separatorChar, 1);
		if (firstSeparator > 0) {
			patternProject = path.substring(1, firstSeparator);
			int lastSeparator = path.lastIndexOf(File.separatorChar);
			if (firstSeparator + 1 < lastSeparator)
				patternPath = path.substring(firstSeparator + 1, lastSeparator);
		} else {
			patternProject = path;
		}

		System.out.println(patternProject);
		System.out.println(patternPath);

		Pattern pDirPlaceholder = Pattern.compile(Pattern
				.quote(IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_DIR)
				+ "(\\{([0-9]*)(,)?([0-9]*)\\})?");

		String[] folderParts = patternPath.split(Pattern.quote(""
				+ File.separatorChar));

		Matcher mDirPlaceholder = pDirPlaceholder.matcher(patternFolder);
		while (mDirPlaceholder.find()) {
			if (mDirPlaceholder.group(1) != null
					&& !"".equals(mDirPlaceholder.group(1))) {
				int start = 1;
				int end = 0;
				try {
					start = Integer.parseInt(mDirPlaceholder.group(2));
				} catch (Exception e) {
				}
				try {
					end = Integer.parseInt(mDirPlaceholder.group(4));
				} catch (Exception e) {
				}

				if (end == 0) {
					if (",".equals(mDirPlaceholder.group(3))) {
						end = folderParts.length;
					} else {
						end = start;
					}
				} else if (end > folderParts.length) {
					end = folderParts.length;
				}

				String folderSubstring = "";
				for (int i = start; i <= end; ++i) {
					if (folderSubstring.length() > 0) {
						folderSubstring += File.separatorChar;
					}
					folderSubstring += folderParts[i - 1];
				}

				patternFolder = patternFolder.replace(mDirPlaceholder.group(),
						folderSubstring);
			} else {
				patternFolder = patternFolder
						.replaceFirst(
								Pattern.quote(IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_DIR),
								patternPath);
			}
		}

		patternFolder = patternFolder.replace(
				IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_PROJECT,
				patternProject);
		fContainer.setText(patternFolder);

		if (patternFile == null)
			patternFile = PHPUnitConfigurationBlock.TEST_FILE_PATTERN_FILE_DEFAULT;

		String fileName = resource.getName();
		int firstDotPos = fileName.indexOf(".");
		int lastDotPos = fileName.lastIndexOf(".");
		patternFile = patternFile.replace(
				IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILENAME,
				fileName.substring(0, firstDotPos));
		patternFile = patternFile.replace(
				IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILE_EXTENSION,
				fileName.substring(lastDotPos + 1));
		fFile.setText(patternFile);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */
	private void initialize() {
		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
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
				fContainer.setText(container.getFullPath().toOSString());
				this.project = container.getProject();
			}
		} else {
			preferences = PHPUnitPreferencesFactory.factoryGlobal();
		}

		if (selection != null) {
			Object element = selection.getFirstElement();
			ISourceModule module = PHPToolkitUtil.getSourceModule(element);
			if (module != null) {
				IType[] types;
				try {
					types = module.getAllTypes();
					if (types != null && types.length > 0) {
						this.setSourceClassName(types[0].getElementName(),
								types[0].getResource());
					}
				} catch (ModelException e1) {
					Logger.logException(e1);
				}
			}
		}

		fSuperClass.setText(preferences.getTestFileSuperClass());
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */
	private void handleContainerBrowse() {
		final ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				"Select Test File Folder");
		dialog.showClosedProjects(false);
		if (dialog.open() == Window.OK) {
			final Object[] result = dialog.getResult();
			if (result.length == 1)
				fContainer.setText(((Path) result[0]).toOSString());
		}
	}

	private void handleClassSearch(IClassSearchListener listener) {
		FilteredPHPClassSelectionDialog dialog = new FilteredPHPClassSelectionDialog(
				getShell(), false);
		if (dialog.open() == Window.OK) {
			PHPSearchMatch result = (PHPSearchMatch) dialog.getFirstResult();
			if (result != null && result.getElement() != null)
				listener.handleSearchMatch(result);
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */
	protected void dialogChanged() {
		testFileExists = false;

		if (preferences == null || preferences.getPhpExecutable() == null
				|| "".equals(preferences.getPhpExecutable())) {
			updateStatus("No preferences found. Please check your PHPUnit configuration.");
			return;
		}

		if (fClassFile == null) {
			updateStatus("No class to test selected");
			return;
		}

		final String container = getContainerName();
		final String fileName = getFileName();

		if (container.length() == 0) {
			updateStatus("Folder must be specified");
			return;
		}

		final IContainer containerFolder = getContainer(container);
		if (containerFolder == null || !containerFolder.exists()) {
			setMessage("Selected folder does not exist and will be created",
					WizardPage.INFORMATION);
		} else {
			if (containerFolder.getProject() == null) {
				updateStatus("Project must be specified within Target Folder");
				return;
			}

			if (!containerFolder.getProject().isOpen()) {
				updateStatus("Selected folder is in a closed project");
				return;
			}

			if (fileName != null
					&& !fileName.equals("") && containerFolder.getFile(new Path(fileName)).exists()) { //$NON-NLS-1$
				setMessage("File exists and will be combined",
						WizardPage.INFORMATION);
				testFileExists = true;
			}
		}

		this.project = null;
		String projectName = null;
		if (container != null && container.length() > 0) {
			if (container.indexOf(java.io.File.separatorChar, 1) > 0)
				projectName = container.substring(1,
						container.indexOf(java.io.File.separatorChar, 1));
			else
				projectName = container.substring(1);
			IContainer projectContainer = getContainer(java.io.File.separatorChar
					+ projectName);
			if (projectContainer != null)
				this.project = projectContainer.getProject();
		}

		if (this.project == null) {
			if (projectName != null)
				updateStatus("Project \"" + projectName + "\" does not exist");
			else
				updateStatus("Project must be specified within Target Folder");
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

		final IContentType contentType = Platform.getContentTypeManager()
				.getContentType(IPHPCoreConstants.ContentTypeID_PHP);
		if (!contentType.isAssociatedWith(fileName)) {
			// fixed bug 195274
			// get the extensions from content type
			final String[] fileExtensions = contentType
					.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
			StringBuffer buffer = new StringBuffer(
					"The file name must end in one of the following extensions [");
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

		final IResource resource = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(path);
		return resource instanceof IContainer ? (IContainer) resource : null;

	}

	protected void updateStatus(final String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public boolean setSourceClassName(String className) {
		return setSourceClassName(className,
				PHPSearchEngine.createWorkspaceScope(), null);
	}

	public boolean setSourceClassName(String className, IResource classFile) {
		return setSourceClassName(className,
				PHPSearchEngine.createProjectScope(classFile.getProject()),
				classFile);
	}

	public boolean setSourceClassName(String className, IDLTKSearchScope scope) {
		return setSourceClassName(className, scope, null);
	}

	protected boolean setSourceClassName(String className,
			IDLTKSearchScope scope, IResource classFile) {
		SearchMatch[] matches = PHPSearchEngine.findClass(className, scope);

		if (matches.length > 0) {
			for (SearchMatch match : matches) {
				if (classFile == null || match.getResource().equals(classFile)) {
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
		return fClassName.getText();
	}

	public IFile getSourceClassFile() {
		return fClassFile;
	}

	public String getContainerName() {
		return fContainer.getText();
	}

	public String getFileName() {
		return fFile.getText();
	}

	public String getTestClassFilePath() {
		return getContainerName() + File.separatorChar + getFileName();
	}

	public String getTestSuperClass() {
		return fSuperClass.getText();
	}

	public String getTestClassName() {
		return fTestClassName.getText();
	}

	public IProject getProject() {
		return project;
	}

	public boolean finish() {
		return true;
	}

	protected void makeFontItalic(Control label) {
		Font font = label.getFont();
		FontData[] data = font.getFontData();
		if (data.length > 0) {
			data[0].setStyle(data[0].getStyle() | SWT.ITALIC);
		}
		label.setFont(new Font(font.getDevice(), data));
	}
}