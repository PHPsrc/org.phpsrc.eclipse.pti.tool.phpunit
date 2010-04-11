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
import org.eclipse.jface.window.Window;
import org.eclipse.php.internal.ui.preferences.IStatusChangeListener;
import org.eclipse.php.internal.ui.preferences.util.Key;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

public class PHPUnitConfigurationBlock extends AbstractPEARPHPToolConfigurationBlock {

	private static final Key PREF_PHP_EXECUTABLE = getPHPUnitKey(PHPUnitPreferenceNames.PREF_PHP_EXECUTABLE);
	private static final Key PREF_PEAR_LIBRARY = getPHPUnitKey(PHPUnitPreferenceNames.PREF_PEAR_LIBRARY);
	private static final Key PREF_DEBUG_PRINT_OUTPUT = getPHPUnitKey(PHPUnitPreferenceNames.PREF_DEBUG_PRINT_OUTPUT);
	private static final Key PREF_BOOSTRAP = getPHPUnitKey(PHPUnitPreferenceNames.PREF_BOOTSTRAP);
	private static final Key PREF_TEST_FILE_PATTERN_FOLDER = getPHPUnitKey(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FOLDER);
	private static final Key PREF_TEST_FILE_PATTERN_FILE = getPHPUnitKey(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FILE);
	private static final Key PREF_TEST_FILE_SUPER_CLASS = getPHPUnitKey(PHPUnitPreferenceNames.PREF_TEST_FILE_SUPER_CLASS);

	public static final String TEST_FILE_PATTERN_FOLDER_DEFAULT = File.separatorChar
			+ IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_PROJECT + File.separatorChar + "tests"
			+ File.separatorChar + IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_DIR;
	public static final String TEST_FILE_PATTERN_FILE_DEFAULT = IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILENAME
			+ "Test." + IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILE_EXTENSION;

	protected Text fBootstrap;
	protected Button fFileButton;
	protected Text fTestFilePatternFolder;
	protected Text fTestFilePatternFile;
	protected Text fTestFileSuperClass;

	public PHPUnitConfigurationBlock(IStatusChangeListener context, IProject project,
			IWorkbenchPreferenceContainer container) {
		super(context, project, getKeys(), container);
	}

	private static Key[] getKeys() {
		return new Key[] { PREF_PHP_EXECUTABLE, PREF_PEAR_LIBRARY, PREF_DEBUG_PRINT_OUTPUT, PREF_BOOSTRAP,
				PREF_TEST_FILE_PATTERN_FOLDER, PREF_TEST_FILE_PATTERN_FILE, PREF_TEST_FILE_SUPER_CLASS };
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

		Label testFileSuperClassLabel = new Label(testFilePatternGroup, SWT.NULL);
		testFileSuperClassLabel.setText("SuperClass:");

		fTestFileSuperClass = new Text(testFilePatternGroup, SWT.BORDER | SWT.SINGLE);
		fTestFileSuperClass.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button fTestFileSuperClassButton = new Button(testFilePatternGroup, SWT.PUSH);
		fTestFileSuperClassButton.setText("Search...");
		fTestFileSuperClassButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				FilteredPHPClassSelectionDialog dialog = new FilteredPHPClassSelectionDialog(getShell(), false);
				if (dialog.open() == Window.OK) {
					PHPSearchMatch result = (PHPSearchMatch) dialog.getFirstResult();
					if (result != null && result.getElement() != null)
						fTestFileSuperClass.setText(result.getElement().getElementName());
				}
			}
		});

		Label testFilePatternFolderLabel = new Label(testFilePatternGroup, SWT.NULL);
		testFilePatternFolderLabel.setText("Source Folder Pattern:");

		fTestFilePatternFolder = new Text(testFilePatternGroup, SWT.BORDER | SWT.SINGLE);
		fTestFilePatternFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button fTestFilePatternFolderDefaultButton = new Button(testFilePatternGroup, SWT.PUSH);
		fTestFilePatternFolderDefaultButton.setText("Default");
		fTestFilePatternFolderDefaultButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				fTestFilePatternFolder.setText(TEST_FILE_PATTERN_FOLDER_DEFAULT);
			}
		});

		Label testFilePatternFolderInfoLabel = new Label(testFilePatternGroup, SWT.NONE);
		testFilePatternFolderInfoLabel.setText("Use placeholder %p for project and %d for directory.");
		GridData folderInfoData = new GridData(GridData.FILL_HORIZONTAL);
		folderInfoData.horizontalSpan = 3;
		testFilePatternFolderInfoLabel.setLayoutData(folderInfoData);
		makeFontItalic(testFilePatternFolderInfoLabel);

		Label testFilePatternFileLabel = new Label(testFilePatternGroup, SWT.NULL);
		testFilePatternFileLabel.setText("File Name Pattern:");

		fTestFilePatternFile = new Text(testFilePatternGroup, SWT.BORDER | SWT.SINGLE);
		fTestFilePatternFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button fTestFilePatternFileDefaultButton = new Button(testFilePatternGroup, SWT.PUSH);
		fTestFilePatternFileDefaultButton.setText("Default");
		fTestFilePatternFileDefaultButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				fTestFilePatternFile.setText(TEST_FILE_PATTERN_FILE_DEFAULT);
			}
		});

		Label testFilePatternFileInfoLabel = new Label(testFilePatternGroup, SWT.NONE);
		testFilePatternFileInfoLabel
				.setText("Use placeholder %f for filename without extension and %e for file extension.");
		GridData fileInfoData = new GridData(GridData.FILL_HORIZONTAL);
		fileInfoData.horizontalSpan = 3;
		testFilePatternFileInfoLabel.setLayoutData(fileInfoData);
		makeFontItalic(testFilePatternFileInfoLabel);

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

		return phpUnitOptionsGroup;
	}

	private void handleBrowse() {

		final ResourceSelectionDialog dialog = new ResourceSelectionDialog(getShell(), ResourcesPlugin.getWorkspace()
				.getRoot(), "Select Bootstrap File");

		if (dialog.open() == Window.OK) {
			final Object[] result = dialog.getResult();
			if (result.length > 0) {
				fBootstrap.setText(((IFile) result[0]).getFullPath().toOSString());
			}
		}
	}

	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		// TODO Auto-generated method stub
	}

	protected boolean processChanges(IWorkbenchPreferenceContainer container) {
		clearProjectLauncherCache(PHPUnit.QUALIFIED_NAME);

		setValue(PREF_BOOSTRAP, fBootstrap.getText());
		setValue(PREF_TEST_FILE_PATTERN_FOLDER, fTestFilePatternFolder.getText());
		setValue(PREF_TEST_FILE_PATTERN_FILE, fTestFilePatternFile.getText());
		setValue(PREF_TEST_FILE_SUPER_CLASS, fTestFileSuperClass.getText());

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
	}
}