package org.phpsrc.eclipse.pti.tools.phpunit.core.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TapTestRunnerClient extends AbstractTestRunnerClient {

	private Pattern fTestCaseStartPattern = Pattern.compile("(not )?ok [0-9]+ - (([^: ]+): )?([^: ]+)::([^: ]+)");

	protected void parseOutput(String text) {
		for (String line : text.split("\n")) {
			Matcher m = fTestCaseStartPattern.matcher(line.trim());
			if (m.matches()) {
				boolean ok = m.group(1) == null;
				int failureStatus = "Failure".equals(m.group(3)) ? ITestRunListener.STATUS_FAILURE
						: ITestRunListener.STATUS_ERROR;
				String testId = m.group(4);
				String testName = m.group(5);

				for (ITestRunListener listener : fListeners) {
					listener.testTreeEntry(testId + "," + testName + ",false,0");

					listener.testStarted(testId, testName);
					if (!ok) {
						listener.testFailed(failureStatus, testId, testName, "", "", "");
					} else {
						listener.testEnded(testId, testName);
					}
				}
			}
		}
	}
}
