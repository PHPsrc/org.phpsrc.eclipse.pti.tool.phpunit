package org.phpsrc.eclipse.pti.tools.phpunit.core.model;

public class SimpleTestRunnerClient extends AbstractTestRunnerClient {

	protected void parseOutput(String text) {

		if (text.length() == 1) {
			switch (text.charAt(0)) {
			case '.':
				for (ITestRunListener listener : fListeners) {
					listener.testStarted("Uknown", "Uknown");
					listener.testEnded("Uknown", "Uknown");
				}
				break;
			case 'F':
				for (ITestRunListener listener : fListeners) {
					listener.testStarted("Uknown", "Uknown");
					// public void testFailed(int status, String testId, String
					// testName, String trace, String expected, String actual);
					listener.testFailed(ITestRunListener.STATUS_FAILURE, "Uknown", "Uknown", null, null, null);
				}
				break;

			}
		}
	}
}
