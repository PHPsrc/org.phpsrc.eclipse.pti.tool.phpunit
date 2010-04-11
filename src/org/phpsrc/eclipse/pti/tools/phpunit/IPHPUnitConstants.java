/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit;

public interface IPHPUnitConstants {
	public static final String PLUGIN_ID = PHPUnitPlugin.PLUGIN_ID;

	public static final String PREFERENCE_PAGE_ID = PLUGIN_ID + ".preferences.PHPUnitPreferencePage"; //$NON-NLS-1$
	public static final String PROJECT_PAGE_ID = PLUGIN_ID + ".properties.PHPUnitPreferencePage"; //$NON-NLS-1$

	public static final String VALIDATOR_PHPUNIT_MARKER = "org.phpsrc.eclipse.pti.tools.phpunit.validator.phpToolPHPUnitMarker"; //$NON-NLS-1$

	public static final String TEST_FILE_PATTERN_PLACEHOLDER_PROJECT = "%p";
	public static final String TEST_FILE_PATTERN_PLACEHOLDER_DIR = "%d";
	public static final String TEST_FILE_PATTERN_PLACEHOLDER_FILENAME = "%f";
	public static final String TEST_FILE_PATTERN_PLACEHOLDER_FILE_EXTENSION = "%e";
}
