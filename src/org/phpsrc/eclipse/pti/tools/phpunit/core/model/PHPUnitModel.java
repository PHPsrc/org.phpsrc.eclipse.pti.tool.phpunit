/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.core.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.phpsrc.eclipse.pti.core.Messages;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.TestElement.Status;
import org.phpsrc.eclipse.pti.ui.viewsupport.BasicElementLabels;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Central registry for JUnit test runs.
 */
public final class PHPUnitModel {

	private static final class LegacyTestRunSessionListener implements ITestRunSessionListener {
		private TestRunSession fActiveTestRunSession;
		private ITestSessionListener fTestSessionListener;

		public void sessionAdded(TestRunSession testRunSession) {
			// Only serve one legacy ITestRunListener at a time, since they
			// cannot distinguish between different concurrent test sessions:
			if (fActiveTestRunSession != null)
				return;

			fActiveTestRunSession = testRunSession;

			fTestSessionListener = new ITestSessionListener() {
				public void testAdded(TestElement testElement) {
				}

				public void sessionStarted() {
					ITestRunListener[] testRunListeners = PHPUnitPlugin.getDefault().getTestRunListeners();
					for (int i = 0; i < testRunListeners.length; i++) {
						testRunListeners[i].testRunStarted(fActiveTestRunSession.getTotalCount());
					}
				}

				public void sessionTerminated() {
					ITestRunListener[] testRunListeners = PHPUnitPlugin.getDefault().getTestRunListeners();
					for (int i = 0; i < testRunListeners.length; i++) {
						testRunListeners[i].testRunTerminated();
					}
					sessionRemoved(fActiveTestRunSession);
				}

				public void sessionStopped(long elapsedTime) {
					ITestRunListener[] testRunListeners = PHPUnitPlugin.getDefault().getTestRunListeners();
					for (int i = 0; i < testRunListeners.length; i++) {
						testRunListeners[i].testRunStopped(elapsedTime);
					}
					sessionRemoved(fActiveTestRunSession);
				}

				public void sessionEnded(long elapsedTime) {
					ITestRunListener[] testRunListeners = PHPUnitPlugin.getDefault().getTestRunListeners();
					for (int i = 0; i < testRunListeners.length; i++) {
						testRunListeners[i].testRunEnded(elapsedTime);
					}
					sessionRemoved(fActiveTestRunSession);
				}

				public void runningBegins() {
					// ignore
				}

				public void testStarted(TestCaseElement testCaseElement) {
					ITestRunListener[] testRunListeners = PHPUnitPlugin.getDefault().getTestRunListeners();
					for (int i = 0; i < testRunListeners.length; i++) {
						testRunListeners[i].testStarted(testCaseElement.getId(), testCaseElement.getTestName());
					}
				}

				public void testFailed(TestElement testElement, Status status, String trace, String expected,
						String actual) {
					ITestRunListener[] testRunListeners = PHPUnitPlugin.getDefault().getTestRunListeners();
					for (int i = 0; i < testRunListeners.length; i++) {
						testRunListeners[i].testFailed(status.getOldCode(), testElement.getId(), testElement
								.getTestName(), trace, expected, actual);
					}
				}

				public void testEnded(TestCaseElement testCaseElement) {
					ITestRunListener[] testRunListeners = PHPUnitPlugin.getDefault().getTestRunListeners();
					for (int i = 0; i < testRunListeners.length; i++) {
						testRunListeners[i].testEnded(testCaseElement.getId(), testCaseElement.getTestName());
					}
				}

				public void testReran(TestCaseElement testCaseElement, Status status, String trace,
						String expectedResult, String actualResult) {
					ITestRunListener[] testRunListeners = PHPUnitPlugin.getDefault().getTestRunListeners();
					for (int i = 0; i < testRunListeners.length; i++) {
						testRunListeners[i].testReran(testCaseElement.getId(), testCaseElement.getClassName(),
								testCaseElement.getTestMethodName(), status.getOldCode(), trace, expectedResult,
								actualResult);
					}
				}

				public boolean acceptsSwapToDisk() {
					return true;
				}
			};
			fActiveTestRunSession.addTestSessionListener(fTestSessionListener);
		}

		public void sessionRemoved(TestRunSession testRunSession) {
			if (fActiveTestRunSession == testRunSession) {
				fActiveTestRunSession.removeTestSessionListener(fTestSessionListener);
				fTestSessionListener = null;
				fActiveTestRunSession = null;
			}
		}
	}

	private final ListenerList fTestRunSessionListeners = new ListenerList();
	/**
	 * Active test run sessions, youngest first.
	 */
	private final LinkedList/* <TestRunSession> */fTestRunSessions = new LinkedList();

	/**
	 * Starts the model (called by the {@link JUnitPlugin} on startup).
	 */
	public void start() {
		PHPUnitDebugEventHandler.getDefault().start();

		/*
		 * TODO: restore on restart: - only import headers! - only import last n
		 * sessions; remove all other files in historyDirectory
		 */
		File historyDirectory = PHPUnitPlugin.getHistoryDirectory();
		File[] swapFiles = historyDirectory.listFiles();
		if (swapFiles != null) {
			Arrays.sort(swapFiles, new Comparator() {
				public int compare(Object o1, Object o2) {
					String name1 = ((File) o1).getName();
					String name2 = ((File) o2).getName();
					return name1.compareTo(name2);
				}
			});
			for (int i = 0; i < swapFiles.length; i++) {
				final File file = swapFiles[i];
				SafeRunner.run(new ISafeRunnable() {
					public void run() throws Exception {
						importTestRunSession(file);
					}

					public void handleException(Throwable exception) {
						PHPUnitPlugin.log(exception);
					}
				});
			}
		}

		addTestRunSessionListener(new LegacyTestRunSessionListener());
	}

	/**
	 * Stops the model (called by the {@link JUnitPlugin} on shutdown).
	 */
	public void stop() {
		PHPUnitDebugEventHandler.getDefault().stop();

		File historyDirectory = PHPUnitPlugin.getHistoryDirectory();
		File[] swapFiles = historyDirectory.listFiles();
		if (swapFiles != null) {
			for (int i = 0; i < swapFiles.length; i++) {
				swapFiles[i].delete();
			}
		}

		// for (Iterator iter= fTestRunSessions.iterator(); iter.hasNext();) {
		// final TestRunSession session= (TestRunSession) iter.next();
		// SafeRunner.run(new ISafeRunnable() {
		// public void run() throws Exception {
		// session.swapOut();
		// }
		// public void handleException(Throwable exception) {
		// JUnitPlugin.log(exception);
		// }
		// });
		// }
	}

	public void addTestRunSessionListener(ITestRunSessionListener listener) {
		fTestRunSessionListeners.add(listener);
	}

	public void removeTestRunSessionListener(ITestRunSessionListener listener) {
		fTestRunSessionListeners.remove(listener);
	}

	/**
	 * @return a list of active {@link TestRunSession}s. The list is a copy of
	 *         the internal data structure and modifications do not affect the
	 *         global list of active sessions. The list is sorted by age,
	 *         youngest first.
	 */
	public List getTestRunSessions() {
		return new ArrayList(fTestRunSessions);
	}

	/**
	 * Adds the given {@link TestRunSession} and notifies all registered
	 * {@link ITestRunSessionListener}s.
	 * <p>
	 * <b>To be called in the UI thread only!</b>
	 * </p>
	 * 
	 * @param testRunSession
	 *            the session to add
	 */
	public void addTestRunSession(TestRunSession testRunSession) {
		Assert.isNotNull(testRunSession);
		Assert.isLegal(!fTestRunSessions.contains(testRunSession));
		fTestRunSessions.addFirst(testRunSession);
		notifyTestRunSessionAdded(testRunSession);
	}

	/**
	 * Imports a test run session from the given file.
	 * 
	 * @param file
	 *            a file containing a test run session transcript
	 * @return the imported test run session
	 * @throws CoreException
	 *             if the import failed
	 */
	public static TestRunSession importTestRunSession(File file) throws CoreException {
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			// parserFactory.setValidating(true); // TODO: add DTD and debug
			// flag
			SAXParser parser = parserFactory.newSAXParser();
			TestRunHandler handler = new TestRunHandler();
			parser.parse(file, handler);
			TestRunSession session = handler.getTestRunSession();
			PHPUnitPlugin.getModel().addTestRunSession(session);
			return session;
		} catch (ParserConfigurationException e) {
			throwImportError(file, e);
		} catch (SAXException e) {
			throwImportError(file, e);
		} catch (IOException e) {
			throwImportError(file, e);
		}
		return null; // does not happen
	}

	public static void importIntoTestRunSession(File swapFile, TestRunSession testRunSession) throws CoreException {
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			// parserFactory.setValidating(true); // TODO: add DTD and debug
			// flag
			SAXParser parser = parserFactory.newSAXParser();
			TestRunHandler handler = new TestRunHandler(testRunSession);
			parser.parse(swapFile, handler);
		} catch (ParserConfigurationException e) {
			throwImportError(swapFile, e);
		} catch (SAXException e) {
			throwImportError(swapFile, e);
		} catch (IOException e) {
			throwImportError(swapFile, e);
		}
	}

	/**
	 * Exports the given test run session.
	 * 
	 * @param testRunSession
	 *            the test run session
	 * @param file
	 *            the destination
	 * @throws CoreException
	 *             if an error occurred
	 */
	public static void exportTestRunSession(TestRunSession testRunSession, File file) throws CoreException {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			exportTestRunSession(testRunSession, out);

		} catch (IOException e) {
			throwExportError(file, e);
		} catch (TransformerConfigurationException e) {
			throwExportError(file, e);
		} catch (TransformerException e) {
			throwExportError(file, e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e2) {
					PHPUnitPlugin.log(e2);
				}
			}
		}
	}

	public static void exportTestRunSession(TestRunSession testRunSession, OutputStream out)
			throws TransformerFactoryConfigurationError, TransformerException {

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		InputSource inputSource = new InputSource();
		SAXSource source = new SAXSource(new TestRunSessionSerializer(testRunSession), inputSource);
		StreamResult result = new StreamResult(out);
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		/*
		 * Bug in Xalan: Only indents if proprietary property
		 * org.apache.xalan.templates.OutputProperties.S_KEY_INDENT_AMOUNT is
		 * set.
		 * 
		 * Bug in Xalan as shipped with J2SE 5.0: Does not read the
		 * indent-amount property at all >:-(.
		 */
		try {
			transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IllegalArgumentException e) {
			// no indentation today...
		}
		transformer.transform(source, result);
	}

	private static void throwExportError(File file, Exception e) throws CoreException {
		throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR, PHPUnitPlugin.PLUGIN_ID, Messages
				.format(ModelMessages.JUnitModel_could_not_write, BasicElementLabels.getPathLabel(file)), e));
	}

	private static void throwImportError(File file, Exception e) throws CoreException {
		throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR, PHPUnitPlugin.PLUGIN_ID, Messages
				.format(ModelMessages.JUnitModel_could_not_read, BasicElementLabels.getPathLabel(file)), e));
	}

	/**
	 * Removes the given {@link TestRunSession} and notifies all registered
	 * {@link ITestRunSessionListener}s.
	 * <p>
	 * <b>To be called in the UI thread only!</b>
	 * </p>
	 * 
	 * @param testRunSession
	 *            the session to remove
	 */
	public void removeTestRunSession(TestRunSession testRunSession) {
		boolean existed = fTestRunSessions.remove(testRunSession);
		if (existed) {
			notifyTestRunSessionRemoved(testRunSession);
		}
		testRunSession.removeSwapFile();
	}

	private void notifyTestRunSessionRemoved(TestRunSession testRunSession) {
		testRunSession.stopTestRun();
		ILaunch launch = testRunSession.getLaunch();
		if (launch != null) {
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			launchManager.removeLaunch(launch);
		}

		Object[] listeners = fTestRunSessionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((ITestRunSessionListener) listeners[i]).sessionRemoved(testRunSession);
		}
	}

	private void notifyTestRunSessionAdded(TestRunSession testRunSession) {
		Object[] listeners = fTestRunSessionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((ITestRunSessionListener) listeners[i]).sessionAdded(testRunSession);
		}
	}

}
