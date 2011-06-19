/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.ui.preferences;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.php.internal.ui.preferences.IStatusChangeListener;
import org.eclipse.php.internal.ui.preferences.util.Key;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.phpsrc.eclipse.pti.core.search.PHPSearchMatch;
import org.phpsrc.eclipse.pti.core.search.ui.dialogs.FilteredPHPClassSelectionDialog;
import org.phpsrc.eclipse.pti.library.pear.ui.preferences.AbstractPEARPHPToolConfigurationBlock;
import org.phpsrc.eclipse.pti.tools.phpunit.IPHPUnitConstants;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnit;

@SuppressWarnings("restriction")
public class PHPUnitConfigurationBlock extends
		AbstractPEARPHPToolConfigurationBlock {

	private static final Key PREF_PHP_EXECUTABLE = getPHPUnitKey(PHPUnitPreferenceNames.PREF_PHP_EXECUTABLE);
	private static final Key PREF_PEAR_LIBRARY = getPHPUnitKey(PHPUnitPreferenceNames.PREF_PEAR_LIBRARY);
	private static final Key PREF_DEBUG_PRINT_OUTPUT = getPHPUnitKey(PHPUnitPreferenceNames.PREF_DEBUG_PRINT_OUTPUT);
	private static final Key PREF_BOOSTRAP = getPHPUnitKey(PHPUnitPreferenceNames.PREF_BOOTSTRAP);
	private static final Key PREF_TEST_FILE_PATTERN_FOLDER = getPHPUnitKey(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FOLDER);
	private static final Key PREF_SOURCE_FILE_PATTERN_FOLDER = getPHPUnitKey(PHPUnitPreferenceNames.PREF_SOURCE_FILE_PATTERN_FOLDER);
	private static final Key PREF_TEST_FILE_PATTERN_FILE = getPHPUnitKey(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FILE);
	private static final Key PREF_TEST_FILE_SUPER_CLASS = getPHPUnitKey(PHPUnitPreferenceNames.PREF_TEST_FILE_SUPER_CLASS);
	private static final Key PREF_GENERATE_CODE_COVERAGE = getPHPUnitKey(PHPUnitPreferenceNames.PREF_GENERATE_CODE_COVERAGE);
	private static final Key PREF_NO_NAMESPACE_CHECK = getPHPUnitKey(PHPUnitPreferenceNames.PREF_NO_NAMESPACE_CHECK);

	public static final String TEST_FILE_PATTERN_FOLDER_DEFAULT = File.separatorChar
			+ IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_PROJECT
			+ File.separatorChar
			+ "tests"
			+ File.separatorChar
			+ IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_DIR;
	public static final String TEST_FILE_PATTERN_FILE_DEFAULT = IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILENAME
			+ "Test."
			+ IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILE_EXTENSION;

	protected Text fBootstrap;
	protected Button fFileButton;
	protected Text fTestFilePatternFolder;
	protected Text fSourceFilePatternFolder;
	protected Text fTestFilePatternFile;
	protected Text fTestFileSuperClass;
	protected Button fGenerateCodeCoverageCheckbox;
	protected Button fNoNamespaceCheckCheckbox;

	public PHPUnitConfigurationBlock(IStatusChangeListener context,
			IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getKeys(), container);
	}

	private static Key[] getKeys() {
		return new Key[] { PREF_PHP_EXECUTABLE, PREF_PEAR_LIBRARY,
				PREF_DEBUG_PRINT_OUTPUT, PREF_BOOSTRAP,
				PREF_TEST_FILE_PATTERN_FOLDER, PREF_TEST_FILE_PATTERN_FILE,
				PREF_TEST_FILE_SUPER_CLASS, PREF_GENERATE_CODE_COVERAGE,
				PREF_NO_NAMESPACE_CHECK, PREF_SOURCE_FILE_PATTERN_FOLDER };
	}

	protected Composite createToolContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 10;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		composite.setLayout(layout);

		Group patternGroup = createTestFilePatternGroup(composite);
		patternGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Group optionGroup = createPHPOptionsGroup(composite);
		optionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		unpackTestFilePattern();
		unpackBootstrap();

		validateSettings(null, null, null);

		return composite;
	}

	private Group createTestFilePatternGroup(Composite folder) {
		final Group testFilePatternGroup = new Group(folder, SWT.RESIZE);
		testFilePatternGroup.setText("Test Case");

		final GridLayout testFilePatternLayout = new GridLayout();
		testFilePatternLayout.numColumns = 3;
		testFilePatternLayout.verticalSpacing = 9;
		testFilePatternGroup.setLayout(testFilePatternLayout);

		Label testFileSuperClassLabel = new Label(testFilePatternGroup,
				SWT.NULL);
		testFileSuperClassLabel.setText("SuperClass:");

		fTestFileSuperClass = new Text(testFilePatternGroup, SWT.BORDER
				| SWT.SINGLE);
		fTestFileSuperClass
				.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button fTestFileSuperClassButton = new Button(testFilePatternGroup,
				SWT.PUSH);
		fTestFileSuperClassButton.setText("Search...");
		fTestFileSuperClassButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				FilteredPHPClassSelectionDialog dialog = new FilteredPHPClassSelectionDialog(
						getShell(), false);
				if (dialog.open() == Window.OK) {
					PHPSearchMatch result = (PHPSearchMatch) dialog
							.getFirstResult();
					if (result != null && result.getElement() != null)
						fTestFileSuperClass.setText(result.getElement()
								.getElementName());
				}
			}
		});

		Label testFilePatternFolderLabel = new Label(testFilePatternGroup,
				SWT.NULL);
		testFilePatternFolderLabel.setText("Test Folder Pattern:");

		fTestFilePatternFolder = new Text(testFilePatternGroup, SWT.BORDER
				| SWT.SINGLE);
		fTestFilePatternFolder.setLayoutData(new GridData(
				GridData.FILL_HORIZONTAL));
		fTestFilePatternFolder.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateSettings(PREF_TEST_FILE_PATTERN_FOLDER, null,
						((Text) e.widget).getText());
			}
		});

		Button fTestFilePatternFolderDefaultButton = new Button(
				testFilePatternGroup, SWT.PUSH);
		fTestFilePatternFolderDefaultButton.setText("Default");
		fTestFilePatternFolderDefaultButton
				.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						fTestFilePatternFolder
								.setText(TEST_FILE_PATTERN_FOLDER_DEFAULT);
					}
				});

		addInfoLabel(
				testFilePatternGroup,
				"The test folder pattern is used for automatic generate the target folder for new test case files",
				3);

		Label sourceFilePatternFolderLabel = new Label(testFilePatternGroup,
				SWT.NULL);
		sourceFilePatternFolderLabel.setText("Source Folder Pattern:");

		fSourceFilePatternFolder = new Text(testFilePatternGroup, SWT.BORDER
				| SWT.SINGLE);
		fSourceFilePatternFolder.setLayoutData(new GridData(
				GridData.FILL_HORIZONTAL));
		fSourceFilePatternFolder.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateSettings(PREF_SOURCE_FILE_PATTERN_FOLDER, null,
						((Text) e.widget).getText());
			}
		});

		addInfoLabel(
				testFilePatternGroup,
				"The source folder pattern is used for automatic generate the target folder for new php class files",
				3);

		addInfoLabel(
				testFilePatternGroup,
				"For both folder pattern use placeholder %p for project and %d[{start,end}] for directory."
						+ "\nExamples for file /library/myframework/subfolder1/subfolder2/filename.php"
						+ "\n%d = /library/myframework/subfolder1/subfolder2"
						+ "\n%d{3} = subfolder1"
						+ "\n%d{2,3} = myframework/subfolder1"
						+ "\n%d{,3} = library/myframework/subfolder1"
						+ "\n%d{2,} = myframework/subfolder1/subfolder2", 3);

		Label testFilePatternFileLabel = new Label(testFilePatternGroup,
				SWT.NULL);
		testFilePatternFileLabel.setText("File Name Pattern:");

		fTestFilePatternFile = new Text(testFilePatternGroup, SWT.BORDER
				| SWT.SINGLE);
		fTestFilePatternFile.setLayoutData(new GridData(
				GridData.FILL_HORIZONTAL));

		Button fTestFilePatternFileDefaultButton = new Button(
				testFilePatternGroup, SWT.PUSH);
		fTestFilePatternFileDefaultButton.setText("Default");
		fTestFilePatternFileDefaultButton
				.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						fTestFilePatternFile
								.setText(TEST_FILE_PATTERN_FILE_DEFAULT);
					}
				});

		addInfoLabel(
				testFilePatternGroup,
				"Use placeholder %f for short filename or %ff for long filename without extension and %e for file extension."
						+ "\nExamples for part1.part2.part3.php"
						+ "\n%f = part1"
						+ "\n%ff = part1.part2.part3"
						+ "\n%e = php", 3);

		return testFilePatternGroup;
	}

	private Group createPHPOptionsGroup(Composite folder) {
		final Group phpUnitOptionsGroup = new Group(folder, SWT.RESIZE);
		phpUnitOptionsGroup.setText("PHPUnit Options");

		final GridLayout phpUnitOptionsLayout = new GridLayout();
		phpUnitOptionsLayout.numColumns = 3;
		phpUnitOptionsLayout.verticalSpacing = 9;
		phpUnitOptionsGroup.setLayout(phpUnitOptionsLayout);

		Label fileLabel = new Label(phpUnitOptionsGroup, SWT.NULL);
		fileLabel.setText("Bootstrap file:");

		fBootstrap = new Text(phpUnitOptionsGroup, SWT.BORDER | SWT.SINGLE);
		fBootstrap.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fFileButton = new Button(phpUnitOptionsGroup, SWT.PUSH);
		fFileButton.setText("Browse...");
		fFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				handleBrowse();
			}
		});

		fGenerateCodeCoverageCheckbox = new Button(phpUnitOptionsGroup,
				SWT.CHECK);
		fGenerateCodeCoverageCheckbox
				.setText("Generate code coverage (need Xdebug PHP extension)");
		fGenerateCodeCoverageCheckbox
				.setSelection(getBooleanValue(PREF_GENERATE_CODE_COVERAGE));
		fGenerateCodeCoverageCheckbox
				.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						boolean selection = fGenerateCodeCoverageCheckbox
								.getSelection();
						setValue(PREF_GENERATE_CODE_COVERAGE, selection);
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});

		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		fGenerateCodeCoverageCheckbox.setLayoutData(data);

		fNoNamespaceCheckCheckbox = new Button(phpUnitOptionsGroup, SWT.CHECK);
		fNoNamespaceCheckCheckbox
				.setText("Do not check for equal namespaces while searching for php/test case classes");
		fNoNamespaceCheckCheckbox
				.setSelection(getBooleanValue(PREF_NO_NAMESPACE_CHECK));
		fNoNamespaceCheckCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean selection = fNoNamespaceCheckCheckbox.getSelection();
				setValue(PREF_NO_NAMESPACE_CHECK, selection);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		fNoNamespaceCheckCheckbox.setLayoutData(data);

		return phpUnitOptionsGroup;
	}

	private void handleBrowse() {

		final ResourceSelectionDialog dialog = new ResourceSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(),
				"Select Bootstrap File");

		if (dialog.open() == Window.OK) {
			final Object[] result = dialog.getResult();
			if (result.length > 0) {
				fBootstrap.setText(((IFile) result[0]).getFullPath()
						.toOSString());
			}
		}
	}

	protected void validateSettings(Key changedKey, String oldValue,
			String newValue) {
		if (fTestFilePatternFolder.getText().indexOf('{') != -1
				&& "".equals(fSourceFilePatternFolder.getText())) {
			String message = "Since you want to use not the whole part for the test folder pattern you have to configure the source folder pattern.";
			Status status = new Status(IStatus.ERROR, PHPUnitPlugin.PLUGIN_ID,
					message);
			getStatusChangeListener().statusChanged(status);
		} else {
			getStatusChangeListener().statusChanged(Status.OK_STATUS);
		}
	}

	protected boolean processChanges(IWorkbenchPreferenceContainer container) {
		clearProjectLauncherCache(PHPUnit.QUALIFIED_NAME);

		setValue(PREF_BOOSTRAP, fBootstrap.getText());
		setValue(PREF_TEST_FILE_PATTERN_FOLDER,
				fTestFilePatternFolder.getText());
		setValue(PREF_TEST_FILE_PATTERN_FILE, fTestFilePatternFile.getText());
		setValue(PREF_TEST_FILE_SUPER_CLASS, fTestFileSuperClass.getText());
		setValue(PREF_SOURCE_FILE_PATTERN_FOLDER,
				fSourceFilePatternFolder.getText());

		return super.processChanges(container);
	}

	public void useProjectSpecificSettings(boolean enable) {
		super.useProjectSpecificSettings(enable);
		fBootstrap.setEnabled(enable);
		fFileButton.setEnabled(enable);
	}

	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null;
	}

	protected final static Key getPHPUnitKey(String key) {
		return getKey(PHPUnitPlugin.PLUGIN_ID, key);
	}

	protected Key getPHPExecutableKey() {
		return PREF_PHP_EXECUTABLE;
	}

	protected Key getDebugPrintOutputKey() {
		return PREF_DEBUG_PRINT_OUTPUT;
	}

	protected Key getPEARLibraryKey() {
		return PREF_PEAR_LIBRARY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#
	 * updateControls()
	 */

	protected void updateControls() {
		super.updateControls();
		unpackBootstrap();
		unpackTestFilePattern();
	}

	private void unpackBootstrap() {
		String bootstrap = getValue(PREF_BOOSTRAP);
		if (bootstrap != null)
			fBootstrap.setText(bootstrap);
	}

	private void unpackTestFilePattern() {
		String folder = getValue(PREF_TEST_FILE_PATTERN_FOLDER);
		if (folder == null)
			folder = TEST_FILE_PATTERN_FOLDER_DEFAULT;
		fTestFilePatternFolder.setText(folder);

		String file = getValue(PREF_TEST_FILE_PATTERN_FILE);
		if (file == null)
			file = TEST_FILE_PATTERN_FILE_DEFAULT;
		fTestFilePatternFile.setText(file);

		String superClass = getValue(PREF_TEST_FILE_SUPER_CLASS);
		if (superClass == null || "".equals(superClass))
			superClass = PHPUnit.PHPUNIT_TEST_CASE_CLASS;
		fTestFileSuperClass.setText(superClass);

		folder = getValue(PREF_SOURCE_FILE_PATTERN_FOLDER);
		if (folder == null)
			folder = "";
		fSourceFilePatternFolder.setText(folder);
	}
}