/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.core.model;

import org.json.JSONException;
import org.json.JSONObject;

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

	private StringBuilder outputCache;
	private boolean testRunStarted = false;
	private boolean testStarted = false;

	protected void parseOutput(String text) {
		for (char c : text.toCharArray()) {
			if (c == '{') {
				outputCache = new StringBuilder();
			}

			if (outputCache != null) {
				outputCache.append(c);
				if (c == '}') {
					parseJson(outputCache.toString());
					outputCache = null;
				}
			}
		}
	}

	private void parseJson(String json) {
		try {
			// System.out.println(json);

			JSONObject jsonObj = new JSONObject(json);

			String event = jsonObj.getString(KEY_EVENT);
			if (EVENT_SUITESTART.equals(event)) {
				for (ITestRunListener listener : fListeners) {
					if (!testRunStarted) {
						listener.testRunStarted(jsonObj.getInt(KEY_TESTS));
						testRunStarted = true;
					}

					listener.testTreeEntry(jsonObj.getString(KEY_SUITE) + "," + jsonObj.getString(KEY_SUITE) + ",true,"
							+ jsonObj.getInt(KEY_TESTS));
				}
			} else if (EVENT_TESTSTART.equals(event)) {
				for (ITestRunListener listener : fListeners) {
					startTest(listener, jsonObj.getString(KEY_TEST));
					testStarted = true;
				}
			} else if (EVENT_TEST.equals(event)) {
				for (ITestRunListener listener : fListeners) {
					if (!testStarted)
						startTest(listener, jsonObj.getString(KEY_TEST));
					else
						testStarted = false;

					String status = jsonObj.getString(KEY_STATUS);
					if (STATUS_PASS.equals(status)) {
						listener.testEnded(jsonObj.getString(KEY_TEST), jsonObj.getString(KEY_TEST));
					} else {
						int statusCode = STATUS_FAIL.equals(status) ? ITestRunListener.STATUS_FAILURE
								: ITestRunListener.STATUS_ERROR;
						listener.testFailed(statusCode, jsonObj.getString(KEY_TEST), jsonObj.getString(KEY_TEST),
								jsonObj.getString(KEY_MESSAGE), "1", "0");
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void startTest(ITestRunListener listener, String key) {
		listener.testTreeEntry(key + "," + key + ",false,0");
		listener.testStarted(key, key);
	}
}
