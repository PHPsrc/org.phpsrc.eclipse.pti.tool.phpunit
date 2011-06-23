/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.core;

public class PHPUnitException extends Exception {
	private static final long serialVersionUID = 1953727915695228180L;

	public PHPUnitException() {
		super();
	}

	public PHPUnitException(String message, Throwable cause) {
		super(message, cause);
	}

	public PHPUnitException(String message) {
		super(message);
	}

	public PHPUnitException(Throwable cause) {
		super(cause);
	}
}
