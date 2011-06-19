package org.phpsrc.eclipse.pti.tools.phpunit.core.model;

import java.util.Date;

public class AbstractTestRunnerClient {

	/**
	 * An array of listeners that are informed about test events.
	 */
	protected ITestRunListener[] fListeners;

	private boolean fIsRunning = false;
	private long fStartTime = -1;

	private ITestDebugProcessListener fDebugProcessListener = new ITestDebugProcessListener() {

		public void startProcess() {
			fIsRunning = true;
			fStartTime = new Date().getTime();
			notifyTestRunStarted();
		}

		public void stopProcess() {
			fIsRunning = false;
			notifyTestRunEnded(new Date().getTime() - fStartTime);
			fStartTime = -1;
			stopListening();
		}

		public void appendOutput(String text) {
			parseOutput(text);
		}
	};

	/**
	 * Start listening to a test run. Start a server connection that the
	 * RemoteTestRunner can connect to.
	 * 
	 * @param listeners
	 *            listeners to inform
	 * @param port
	 *            port on which the server socket will be opened
	 */
	public synchronized void startListening(ITestRunListener[] listeners) {
		fListeners = listeners;
		PHPUnitDebugEventHandler.getDefault()
				.addListener(fDebugProcessListener);
	}

	protected synchronized void stopListening() {
		PHPUnitDebugEventHandler.getDefault().removeListener(
				fDebugProcessListener);
		fListeners = null;
	}

	protected void notifyTestRunStarted() {
		if (fListeners != null) {
			for (ITestRunListener listener : fListeners) {
				listener.testRunStarted(0);
			}
		}
	}

	protected void notifyTestRunEnded(long elapsedTime) {
		if (fListeners != null) {
			for (ITestRunListener listener : fListeners) {
				listener.testRunEnded(elapsedTime);
			}
		}
	}

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
					listener.testFailed(ITestRunListener.STATUS_FAILURE,
							"Uknown", "Uknown", null, null, null);
				}
				break;

			}
		}
	}

	public boolean isRunning() {
		return fIsRunning;
	}

	/**
	 * Requests to stop the remote test run.
	 */
	public synchronized void stopTest() {
		if (isRunning()) {
		}
	}
}
