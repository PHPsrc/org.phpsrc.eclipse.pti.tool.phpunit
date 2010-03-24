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
