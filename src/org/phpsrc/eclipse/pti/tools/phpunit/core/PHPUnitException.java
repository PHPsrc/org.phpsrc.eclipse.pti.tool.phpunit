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
