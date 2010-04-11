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

		String phpExe = prefs.getString(PHPUnitPreferenceNames.PREF_PHP_EXECUTABLE);
		boolean printOutput = prefs.getBoolean(PHPUnitPreferenceNames.PREF_DEBUG_PRINT_OUTPUT);
		String bootstrap = prefs.getString(PHPUnitPreferenceNames.PREF_BOOTSTRAP);
		String testFilePatternFolder = prefs.getString(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FOLDER);
		String testFilePatternFile = prefs.getString(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FILE);
		String pearLibraryName = prefs.getString(PHPUnitPreferenceNames.PREF_PEAR_LIBRARY);

		if (project != null) {
			IScopeContext[] preferenceScopes = createPreferenceScopes(project);
			if (preferenceScopes[0] instanceof ProjectScope) {
				IEclipsePreferences node = preferenceScopes[0].getNode(PHPUnitPlugin.PLUGIN_ID);
				if (node != null) {
					phpExe = node.get(PHPUnitPreferenceNames.PREF_PHP_EXECUTABLE, phpExe);
					printOutput = node.getBoolean(PHPUnitPreferenceNames.PREF_DEBUG_PRINT_OUTPUT, printOutput);
					bootstrap = node.get(PHPUnitPreferenceNames.PREF_BOOTSTRAP, bootstrap);
					testFilePatternFolder = node.get(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FOLDER,
							testFilePatternFolder);
					testFilePatternFile = node.get(PHPUnitPreferenceNames.PREF_TEST_FILE_PATTERN_FILE,
							testFilePatternFile);
					pearLibraryName = node.get(PHPUnitPreferenceNames.PREF_PEAR_LIBRARY, pearLibraryName);
				}
			}
		}

		return new PHPUnitPreferences(phpExe, printOutput, pearLibraryName, bootstrap, testFilePatternFolder,
				testFilePatternFile);
	}

	protected static IScopeContext[] createPreferenceScopes(IProject project) {
		if (project != null) {
			return new IScopeContext[] { new ProjectScope(project), new InstanceScope(), new DefaultScope() };
		}
		return new IScopeContext[] { new InstanceScope(), new DefaultScope() };
	}
}
