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
	protected String bootstrap;
	protected String testFilePatternFolder;
	protected String testFilePatternFile;
	protected String testFileSuperClass;
	protected boolean generateCodeCoverage;
	protected boolean noNamespaceCheck;

	public PHPUnitPreferences(String phpExecutable, boolean printOutput,
			String pearLibraryName, String bootstrap,
			String testFilePatternFolder, String testFilePatternFile,
			String testFileSuperClass, boolean generateCodeCoverage,
			boolean noNamespaceCheck) {
		super(phpExecutable, printOutput, pearLibraryName);
		this.bootstrap = bootstrap;
		this.testFilePatternFolder = testFilePatternFolder;
		this.testFilePatternFile = testFilePatternFile;
		this.testFileSuperClass = testFileSuperClass;
		if (this.testFileSuperClass == null
				|| "".equals(this.testFileSuperClass))
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