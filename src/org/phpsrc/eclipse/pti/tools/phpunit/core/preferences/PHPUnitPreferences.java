/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.core.preferences;

import org.phpsrc.eclipse.pti.library.pear.core.preferences.AbstractPEARPHPToolPreferences;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnit;

public class PHPUnitPreferences extends AbstractPEARPHPToolPreferences {
	protected final String bootstrap;
	protected final String testFilePatternFolder;
	protected final String sourceFilePatternFolder;
	protected final String testFilePatternFile;
	protected final String testFileSuperClass;
	protected final boolean generateCodeCoverage;
	protected final boolean noNamespaceCheck;

	public PHPUnitPreferences(String phpExecutable, boolean printOutput,
			String pearLibraryName, String bootstrap,
			String testFilePatternFolder, String sourceFilePatternFolder,
			String testFilePatternFile, String testFileSuperClass,
			boolean generateCodeCoverage, boolean noNamespaceCheck) {
		super(phpExecutable, printOutput, pearLibraryName);
		this.bootstrap = bootstrap;
		this.testFilePatternFolder = testFilePatternFolder;
		this.sourceFilePatternFolder = sourceFilePatternFolder;
		this.testFilePatternFile = testFilePatternFile;
		if (testFileSuperClass == null || "".equals(testFileSuperClass))
			this.testFileSuperClass = testFileSuperClass;
		else
			this.testFileSuperClass = PHPUnit.PHPUNIT_TEST_CASE_CLASS;
		this.generateCodeCoverage = generateCodeCoverage;
		this.noNamespaceCheck = noNamespaceCheck;
	}

	public String getBootstrap() {
		return bootstrap;
	}

	public String getTestFilePatternFolder() {
		return testFilePatternFolder;
	}

	public String getSourceFilePatternFolder() {
		return sourceFilePatternFolder;
	}

	public String getTestFilePatternFile() {
		return testFilePatternFile;
	}

	public String getTestFileSuperClass() {
		return testFileSuperClass;
	}

	public boolean generateCodeCoverage() {
		return generateCodeCoverage;
	}

	public boolean noNamespaceCheck() {
		return noNamespaceCheck;
	}
}