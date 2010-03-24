package org.phpsrc.eclipse.pti.tools.phpunit.core.model;

public interface ITestDebugProcessListener {
	public void startProcess();

	public void stopProcess();

	public void appendOutput(String text);
}
