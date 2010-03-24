/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Davids <sdavids@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.phpsrc.eclipse.pti.tools.phpunit.ui.views.testrunner;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;

/**
 * Default preference value initialization for the
 * <code>org.eclipse.jdt.junit</code> plug-in.
 */
public class PHPunitPreferenceInitializer extends AbstractPreferenceInitializer {

	/** {@inheritDoc} */
	public void initializeDefaultPreferences() {
		IPreferenceStore prefs = PHPUnitPlugin.getDefault().getPreferenceStore();

		prefs.setDefault(PHPUnitPreferencesConstants.DO_FILTER_STACK, true);

		prefs.setDefault(PHPUnitPreferencesConstants.SHOW_ON_ERROR_ONLY, false);
		prefs.setDefault(PHPUnitPreferencesConstants.ENABLE_ASSERTIONS, false);

		prefs.setDefault(PHPUnitPreferencesConstants.MAX_TEST_RUNS, 10);
	}
}
