/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.core.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.phpsrc.eclipse.pti.ui.Logger;

public class JsonTestRunnerClient extends AbstractTestRunnerClient {
	private static final String STATUS_PASS = "pass"; //$NON-NLS-1$
	private static final String STATUS_FAIL = "fail"; //$NON-NLS-1$
	private static final String KEY_SUITE = "suite"; //$NON-NLS-1$
	private static final String KEY_TEST = "test"; //$NON-NLS-1$
	private static final String KEY_TESTS = "tests"; //$NON-NLS-1$
	private static final String KEY_EVENT = "event"; //$NON-NLS-1$
	private static final String KEY_STATUS = "status"; //$NON-NLS-1$
	private static final String KEY_TRACE = "trace"; //$NON-NLS-1$
	private static final String KEY_MESSAGE = "message"; //$NON-NLS-1$
	private static final String EVENT_SUITESTART = "suiteStart"; //$NON-NLS-1$
	private static final String EVENT_TEST = "test"; //$NON-NLS-1$
	private static final String EVENT_TESTSTART = "testStart"; //$NON-NLS-1$

	public static final String PARAM_SEP = "<,>";

	private StringBuilder outputCache;
	private StringBuilder jsonOutputCache;
	private boolean testRunStarted = false;
	private boolean testStarted = false;
	private String lastTestKey;
	private Pattern errorPattern = Pattern.compile("Fatal error: .*");
	private Pattern failedAssertingPattern = Pattern.compile(
			"Failed asserting that (.*) is equal to (.*)\\.", Pattern.MULTILINE
					| Pattern.DOTALL);
	private int jsonObjectLevel = 0;

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
		outputCache = new StringBuilder();
		super.startListening(listeners);
	}

	protected void parseOutput(String text) {
		for (char c : text.toCharArray()) {
			if (c == '{') {
				if (jsonObjectLevel == 0) {
					jsonOutputCache = new StringBuilder();
				}
				++jsonObjectLevel;
			}

			if (jsonOutputCache != null) {
				jsonOutputCache.append(c);
				if (c == '}') {
					--jsonObjectLevel;
					if (jsonObjectLevel == 0) {
						parseJson(jsonOutputCache.toString());
						jsonOutputCache = null;
					}
				}
			} else {
				outputCache.append(c);
			}
		}
	}

	private void parseJson(String json) {
		try {
			JSONObject jsonObj = new JSONObject(json);
			if (jsonObj.has(KEY_EVENT)) {

				String event = jsonObj.getString(KEY_EVENT);
				if (EVENT_SUITESTART.equals(event)) {
					for (ITestRunListener listener : fListeners) {
						if (!testRunStarted) {
							listener.testRunStarted(jsonObj.getInt(KEY_TESTS));
							testRunStarted = true;
						}

						listener.testTreeEntry(jsonObj.getString(KEY_SUITE)
								+ PARAM_SEP + jsonObj.getString(KEY_SUITE)
								+ PARAM_SEP + "true" + PARAM_SEP
								+ jsonObj.getInt(KEY_TESTS));
					}
				} else if (EVENT_TESTSTART.equals(event)) {
					for (ITestRunListener listener : fListeners) {
						startTest(listener, jsonObj.getString(KEY_TEST));
						testStarted = true;
						lastTestKey = jsonObj.getString(KEY_TEST);
					}
				} else if (EVENT_TEST.equals(event)) {
					for (ITestRunListener listener : fListeners) {
						if (!testStarted)
							startTest(listener, jsonObj.getString(KEY_TEST));
						else
							testStarted = false;

						String status = jsonObj.getString(KEY_STATUS);
						if (STATUS_PASS.equals(status)) {
							listener.testEnded(jsonObj.getString(KEY_TEST),
									jsonObj.getString(KEY_TEST));
						} else {
							int statusCode = STATUS_FAIL.equals(status) ? ITestRunListener.STATUS_FAILURE
									: ITestRunListener.STATUS_ERROR;

							String expected = "";
							String actual = "";

							String msg = jsonObj.getString(KEY_MESSAGE).trim();
							Matcher m = failedAssertingPattern.matcher(msg);
							if (m.matches()) {
								expected = m.group(2);
								actual = m.group(1);
							}

							listener.testFailed(statusCode,
									jsonObj.getString(KEY_TEST),
									jsonObj.getString(KEY_TEST), msg, expected,
									actual);
						}
					}
				}
			}
		} catch (JSONException e) {
			Logger.logException(e);
		}
	}

	private void startTest(ITestRunListener listener, String key) {
		listener.testTreeEntry(key + PARAM_SEP + key + PARAM_SEP + "false"
				+ PARAM_SEP + "0");
		listener.testStarted(key, key);
	}

	protected void notifyTestRunEnded(long elapsedTime) {
		if (testStarted) {
			StringBuilder error = new StringBuilder();
			Matcher m = errorPattern.matcher(outputCache.toString());
			while (m.find()) {
				if (error.length() > 0)
					error.append('\n');
				error.append(m.group().trim());
			}

			for (ITestRunListener listener : fListeners) {
				listener.testFailed(ITestRunListener.STATUS_ERROR, lastTestKey,
						lastTestKey, error.toString(), null, null);
			}
		}

		super.notifyTestRunEnded(elapsedTime);
	}
}
