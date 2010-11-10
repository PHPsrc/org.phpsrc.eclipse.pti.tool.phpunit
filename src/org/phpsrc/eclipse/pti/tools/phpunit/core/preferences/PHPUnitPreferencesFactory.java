/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.core.preferences;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;
import org.phpsrc.eclipse.pti.tools.phpunit.ui.preferences.PHPUnitPreferenceNames;

public class PHPUnitPreferencesFactory {
	public static PHPUnitPreferences factory(IFile file) {
		return factory(file.getProject());
	}

	public static PHPUnitPreferences factory(IResource resource) {
		return factory(resource.getProject());
	}

	public static PHPUnitPreferences factoryGlobal() {
		return factory((IProject) null);
	}

	public static PHPUnitPreferences factory(IProject project) {
		Preferences prefs = PHPUnitPlugin.getDefault().getPluginPreferences();

		String phpExe = prefs
				.getString(PHPUnitPreferenceNames.PREF_PHP_EXECUTABLE);
		boolean printOutput = prefs
				.getBoolean(PHPUnitPreferenceNames.PREF_DEBUG_PRINT_OUTPUT);
		String bootstrap = prefs
				.getString(PHPUnitPreferenceNames.PREF_BOOTSTRAP);
		String testFilePatternFolder = prefs
				.getString(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FOLDER);
		String testFilePatternFile = prefs
				.getString(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FILE);
		String pearLibraryName = prefs
				.getString(PHPUnitPreferenceNames.PREF_PEAR_LIBRARY);
		String testFileSuperClass = prefs
				.getString(PHPUnitPreferenceNames.PREF_TEST_FILE_SUPER_CLASS);
		boolean generateCodeCoverage = prefs
				.getBoolean(PHPUnitPreferenceNames.PREF_GENERATE_CODE_COVERAGE);

		boolean noNamespaceCheck = prefs
				.getBoolean(PHPUnitPreferenceNames.PREF_NO_NAMESPACE_CHECK);

		if (project != null) {
			IScopeContext[] preferenceScopes = createPreferenceScopes(project);
			if (preferenceScopes[0] instanceof ProjectScope) {
				IEclipsePreferences node = preferenceScopes[0]
						.getNode(PHPUnitPlugin.PLUGIN_ID);
				if (node != null) {
					phpExe = node.get(
							PHPUnitPreferenceNames.PREF_PHP_EXECUTABLE, phpExe);
					printOutput = node.getBoolean(
							PHPUnitPreferenceNames.PREF_DEBUG_PRINT_OUTPUT,
							printOutput);
					bootstrap = node.get(PHPUnitPreferenceNames.PREF_BOOTSTRAP,
							bootstrap);
					testFilePatternFolder = node
							.get(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FOLDER,
									testFilePatternFolder);
					testFilePatternFile = node.get(
							PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FILE,
							testFilePatternFile);
					pearLibraryName = node.get(
							PHPUnitPreferenceNames.PREF_PEAR_LIBRARY,
							pearLibraryName);
					testFileSuperClass = node.get(
							PHPUnitPreferenceNames.PREF_TEST_FILE_SUPER_CLASS,
							testFileSuperClass);
					generateCodeCoverage = node.getBoolean(
							PHPUnitPreferenceNames.PREF_GENERATE_CODE_COVERAGE,
							generateCodeCoverage);
					noNamespaceCheck = node.getBoolean(
							PHPUnitPreferenceNames.PREF_NO_NAMESPACE_CHECK,
							noNamespaceCheck);
				}
			}
		}

		return new PHPUnitPreferences(phpExe, printOutput, pearLibraryName,
				bootstrap, testFilePatternFolder, testFilePatternFile,
				testFileSuperClass, generateCodeCoverage, noNamespaceCheck);
	}

	protected static IScopeContext[] createPreferenceScopes(IProject project) {
		if (project != null) {
			return new IScopeContext[] { new ProjectScope(project),
					new InstanceScope(), new DefaultScope() };
		}
		return new IScopeContext[] { new InstanceScope(), new DefaultScope() };
	}
}
