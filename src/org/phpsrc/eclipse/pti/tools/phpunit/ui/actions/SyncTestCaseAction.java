/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.ui.actions;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.phpsrc.eclipse.pti.core.PHPToolCorePlugin;
import org.phpsrc.eclipse.pti.core.PHPToolkitUtil;
import org.phpsrc.eclipse.pti.core.jobs.MutexRule;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnit;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnitUtil;
import org.phpsrc.eclipse.pti.tools.phpunit.core.preferences.PHPUnitPreferences;
import org.phpsrc.eclipse.pti.tools.phpunit.core.preferences.PHPUnitPreferencesFactory;
import org.phpsrc.eclipse.pti.ui.actions.ResourceAction;

public class SyncTestCaseAction extends ResourceAction {

	final MutexRule rule = new MutexRule();

	public void run(IAction action) {
		final IResource[] resources = getSelectedResources();
		if (resources.length > 0 && resources[0] instanceof IFile) {
			IFile file = (IFile) resources[0];

			try {
				if (PHPUnit.isTestCase(file)) {
					IFile testCase = file;
					File tmpFile = PHPUnitUtil
							.generateProjectRelativePHPClassFile(file);
					if (tmpFile == null) {
						throw new Exception(
								"Can not create PHP Class file path. Please check your PHPUnit configuration.");
					}

					IFile phpClass = getWorspaceFile(tmpFile);
					boolean doSync = true;
					if (!phpClass.exists()) {
						doSync = MessageDialog.openConfirm(
								PHPToolCorePlugin.getActiveWorkbenchShell(),
								"Info", "PHP Class file " + tmpFile.toString()
										+ " not exists. Create?");
					}

					if (doSync) {
						String testClassName = PHPToolkitUtil
								.getClassNameWithNamespace(testCase);
						PHPUnitUtil
								.syncTestCaseToPHPClass(
										testClassName,
										testCase,
										PHPUnitUtil
												.generatePHPClassName(testClassName),
										phpClass.getFullPath().toOSString(),
										null);
					}

				} else {
					IFile phpClass = file;
					File tmpFile = PHPUnitUtil
							.generateProjectRelativeTestCaseFile(file);
					if (tmpFile == null) {
						throw new Exception(
								"Can not create Test Case file path. Please check your PHPUnit configuration.");
					}
					IFile testCase = getWorspaceFile(tmpFile);
					boolean doSync = true;
					if (!testCase.exists()) {
						doSync = MessageDialog.openConfirm(
								PHPToolCorePlugin.getActiveWorkbenchShell(),
								"Info", "Test Case file " + tmpFile.toString()
										+ " not exists. Create?");
					}

					if (doSync) {
						PHPUnitPreferences prefs = PHPUnitPreferencesFactory
								.factory(phpClass);
						String sourceClassName = PHPToolkitUtil
								.getClassNameWithNamespace(phpClass);
						PHPUnitUtil
								.syncPHPClassToTestCase(
										sourceClassName,
										phpClass,
										PHPUnitUtil
												.generateTestCaseClassName(sourceClassName),
										testCase.getFullPath().toOSString(),
										prefs.getTestFileSuperClass());
					}
				}
			} catch (Exception e) {
				MessageDialog.openError(
						PHPToolCorePlugin.getActiveWorkbenchShell(), "Error",
						e.getMessage());
			}

		}
	}

	private IFile getWorspaceFile(File relativeFile) throws Exception {
		File test = new File(ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toOSString(), relativeFile.toString());
		IFile[] files = ResourcesPlugin.getWorkspace().getRoot()
				.findFilesForLocationURI(test.toURI());
		if (files == null || files.length == 0)
			throw new Exception("Can not determine workspace file "
					+ relativeFile.toString());

		return files[0];
	}

}