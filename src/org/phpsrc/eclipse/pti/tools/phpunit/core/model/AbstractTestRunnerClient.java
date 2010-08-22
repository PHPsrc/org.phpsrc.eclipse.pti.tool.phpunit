package org.phpsrc.eclipse.pti.tools.phpunit.core.model;

public abstract class AbstractTestRunnerClient {

	/**
	 * An array of listeners that are informed about test events.
	 */
	protected ITestRunListener[] fListeners;

	private boolean fIsRunning = false;

	private ITestDebugProcessListener fDebugProcessListener = new ITestDebugProcessListener() {

		public void startProcess() {
			fIsRunning = true;
			notifyTestRunStarted();
		}

		public void stopProcess() {
			fIsRunning = false;
			notifyTestRunEnded();
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
		PHPUnitDebugEventHandler.getDefault().addListener(fDebugProcessListener);
	}

	protected synchronized void stopListening() {
		PHPUnitDebugEventHandler.getDefault().removeListener(fDebugProcessListener);
		fListeners = null;
	}

	protected void notifyTestRunStarted() {
		if (fListeners != null) {
			for (ITestRunListener listener : fListeners) {
				listener.testRunStarted(0);
			}
		}
	}

	protected void notifyTestRunEnded() {
		if (fListeners != null) {
			for (ITestRunListener listener : fListeners) {
				listener.testRunEnded(0);
			}
		}
	}

	protected abstract void parseOutput(String text);

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
