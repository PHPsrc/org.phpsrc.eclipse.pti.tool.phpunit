/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Julien Ruaux: jruaux@octo.com see bug 25324 Ability to know when tests are finished [junit]
 *     Vincent Massol: vmassol@octo.com 25324 Ability to know when tests are finished [junit]
 *     Sebastian Davids: sdavids@gmx.de 35762 JUnit View wasting a lot of screen space [JUnit]
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *******************************************************************************/
package org.phpsrc.eclipse.pti.tools.phpunit.ui.views.testrunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.core.ElementChangedEvent;
import org.eclipse.dltk.core.IElementChangedListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.php.internal.ui.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.PageSwitcher;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;
import org.phpsrc.eclipse.pti.core.Messages;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;
import org.phpsrc.eclipse.pti.tools.phpunit.core.launcher.ITestKind;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.ITestElement.Result;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.ITestRunSessionListener;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.ITestSessionListener;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.PHPUnitModel;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.TestCaseElement;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.TestElement;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.TestRunSession;
import org.phpsrc.eclipse.pti.tools.phpunit.ui.actions.RunTestCaseAction;
import org.phpsrc.eclipse.pti.ui.viewsupport.BasicElementLabels;
import org.phpsrc.eclipse.pti.ui.viewsupport.ViewHistory;

/**
 * A ViewPart that shows the results of a test run.
 */
public class TestRunnerViewPart extends ViewPart {

	public static final String NAME = "org.phpsrc.eclipse.pti.tools.phpunit.ui.views.testrunner"; //$NON-NLS-1$

	private static final String RERUN_LAST_COMMAND = "org.eclipse.pti.tools.phpunit.phpunitShortcut.rerunLast"; //$NON-NLS-1$
	private static final String RERUN_FAILED_FIRST_COMMAND = "org.eclipse.pti.tools.phpunit.phpunitShortcut.rerunFailedFirst"; //$NON-NLS-1$

	static final int REFRESH_INTERVAL = 200;

	static final int LAYOUT_FLAT = 0;
	static final int LAYOUT_HIERARCHICAL = 1;

	/**
	 * Whether the output scrolls and reveals tests as they are executed.
	 */
	protected boolean fAutoScroll = true;
	/**
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>, or
	 * <code>VIEW_ORIENTATION_AUTOMATIC</code>.
	 */
	private int fOrientation = VIEW_ORIENTATION_AUTOMATIC;
	/**
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>.
	 */
	private int fCurrentOrientation;
	/**
	 * The current layout mode (LAYOUT_FLAT or LAYOUT_HIERARCHICAL).
	 */
	private int fLayout = LAYOUT_HIERARCHICAL;

	// private boolean fTestIsRunning= false;

	protected PHPUnitProgressBar fProgressBar;
	protected ProgressImages fProgressImages;
	protected Image fViewImage;
	protected CounterPanel fCounterPanel;
	protected boolean fShowOnErrorOnly = false;
	protected Clipboard fClipboard;
	protected volatile String fInfoMessage;

	private FailureTrace fFailureTrace;

	private TestViewer fTestViewer;
	/**
	 * Is the UI disposed?
	 */
	private boolean fIsDisposed = false;

	/**
	 * Actions
	 */
	private Action fNextAction;
	private Action fPreviousAction;

	private StopAction fStopAction;
	private PHPUnitCopyAction fCopyAction;

	private Action fRerunLastTestAction;
	private IHandlerActivation fRerunLastActivation;
	private Action fRerunFailedFirstAction;
	private IHandlerActivation fRerunFailedFirstActivation;

	private Action fFailuresOnlyFilterAction;
	private ScrollLockAction fScrollLockAction;
	private ToggleOrientationAction[] fToggleOrientationActions;
	private ShowTestHierarchyAction fShowTestHierarchyAction;
	private ShowTimeAction fShowTimeAction;
	private ActivateOnErrorAction fActivateOnErrorAction;
	private IMenuListener fViewMenuListener;

	private TestRunSession fTestRunSession;
	private TestSessionListener fTestSessionListener;

	private RunnerViewHistory fViewHistory;
	private TestRunSessionListener fTestRunSessionListener;

	final Image fStackViewIcon;
	final Image fTestRunOKIcon;
	final Image fTestRunFailIcon;
	final Image fTestRunOKDirtyIcon;
	final Image fTestRunFailDirtyIcon;

	final Image fTestIcon;
	final Image fTestOkIcon;
	final Image fTestErrorIcon;
	final Image fTestFailIcon;
	final Image fTestRunningIcon;
	final Image fTestIgnoredIcon;

	final ImageDescriptor fSuiteIconDescriptor = PHPUnitPlugin
			.getImageDescriptor("obj16/tsuite.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteOkIconDescriptor = PHPUnitPlugin
			.getImageDescriptor("obj16/tsuiteok.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteErrorIconDescriptor = PHPUnitPlugin
			.getImageDescriptor("obj16/tsuiteerror.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteFailIconDescriptor = PHPUnitPlugin
			.getImageDescriptor("obj16/tsuitefail.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteRunningIconDescriptor = PHPUnitPlugin
			.getImageDescriptor("obj16/tsuiterun.gif"); //$NON-NLS-1$

	final Image fSuiteIcon;
	final Image fSuiteOkIcon;
	final Image fSuiteErrorIcon;
	final Image fSuiteFailIcon;
	final Image fSuiteRunningIcon;

	final List fImagesToDispose;

	// Persistence tags.
	static final String TAG_PAGE = "page"; //$NON-NLS-1$
	static final String TAG_RATIO = "ratio"; //$NON-NLS-1$
	static final String TAG_TRACEFILTER = "tracefilter"; //$NON-NLS-1$
	static final String TAG_ORIENTATION = "orientation"; //$NON-NLS-1$
	static final String TAG_SCROLL = "scroll"; //$NON-NLS-1$
	static final String TAG_LAYOUT = "layout"; //$NON-NLS-1$
	static final String TAG_FAILURES_ONLY = "failuresOnly"; //$NON-NLS-1$
	static final String TAG_SHOW_TIME = "time"; //$NON-NLS-1$

	static final String PREF_LAST_PATH = "lastImportExportPath"; //$NON-NLS-1$

	// orientations
	static final int VIEW_ORIENTATION_VERTICAL = 0;
	static final int VIEW_ORIENTATION_HORIZONTAL = 1;
	static final int VIEW_ORIENTATION_AUTOMATIC = 2;

	private IMemento fMemento;

	Image fOriginalViewImage;
	IElementChangedListener fDirtyListener;

	// private CTabFolder fTabFolder;
	private SashForm fSashForm;

	private Composite fCounterComposite;
	private Composite fParent;

	/**
	 * A Job that periodically updates view description, counters, and progress
	 * bar.
	 */
	private UpdateUIJob fUpdateJob;

	/**
	 * A Job that runs as long as a test run is running. It is used to show
	 * busyness for running jobs in the view (title in italics).
	 */
	private JUnitIsRunningJob fJUnitIsRunningJob;
	private ILock fJUnitIsRunningLock;
	public static final Object FAMILY_PHPUNIT_RUN = new Object();

	private IPartListener2 fPartListener = new IPartListener2() {
		public void partActivated(IWorkbenchPartReference ref) {
		}

		public void partBroughtToTop(IWorkbenchPartReference ref) {
		}

		public void partInputChanged(IWorkbenchPartReference ref) {
		}

		public void partClosed(IWorkbenchPartReference ref) {
		}

		public void partDeactivated(IWorkbenchPartReference ref) {
		}

		public void partOpened(IWorkbenchPartReference ref) {
		}

		public void partVisible(IWorkbenchPartReference ref) {
			if (getSite().getId().equals(ref.getId())) {
				fPartIsVisible = true;
			}
		}

		public void partHidden(IWorkbenchPartReference ref) {
			if (getSite().getId().equals(ref.getId())) {
				fPartIsVisible = false;
			}
		}
	};

	protected boolean fPartIsVisible = false;

	private class RunnerViewHistory extends ViewHistory {

		public void configureHistoryListAction(IAction action) {
			action.setText(PHPUnitMessages.TestRunnerViewPart_history);
		}

		public void configureHistoryDropDownAction(IAction action) {
			action.setToolTipText(PHPUnitMessages.TestRunnerViewPart_test_run_history);
			PHPUnitPlugin.setLocalImageDescriptors(action, "history_list.gif"); //$NON-NLS-1$
		}

		public Action getClearAction() {
			return new ClearAction();
		}

		public String getHistoryListDialogTitle() {
			return PHPUnitMessages.TestRunnerViewPart_test_runs;
		}

		public String getHistoryListDialogMessage() {
			return PHPUnitMessages.TestRunnerViewPart_select_test_run;
		}

		public Shell getShell() {
			return fParent.getShell();
		}

		public List getHistoryEntries() {
			return PHPUnitPlugin.getModel().getTestRunSessions();
		}

		public Object getCurrentEntry() {
			return fTestRunSession;
		}

		public void setActiveEntry(Object entry) {
			TestRunSession deactivatedSession = setActiveTestRunSession((TestRunSession) entry);
			if (deactivatedSession != null)
				deactivatedSession.swapOut();
		}

		public void setHistoryEntries(List remainingEntries, Object activeEntry) {
			setActiveTestRunSession((TestRunSession) activeEntry);

			List testRunSessions = PHPUnitPlugin.getModel()
					.getTestRunSessions();
			testRunSessions.removeAll(remainingEntries);
			for (Iterator iter = testRunSessions.iterator(); iter.hasNext();) {
				PHPUnitPlugin.getModel().removeTestRunSession(
						(TestRunSession) iter.next());
			}
			for (Iterator iter = remainingEntries.iterator(); iter.hasNext();) {
				TestRunSession remaining = (TestRunSession) iter.next();
				remaining.swapOut();
			}
		}

		public ImageDescriptor getImageDescriptor(Object element) {
			TestRunSession session = (TestRunSession) element;
			if (session.isStopped())
				return fSuiteIconDescriptor;

			if (session.isRunning())
				return fSuiteRunningIconDescriptor;

			Result result = session.getTestResult(true);
			if (result == Result.OK)
				return fSuiteOkIconDescriptor;
			else if (result == Result.ERROR)
				return fSuiteErrorIconDescriptor;
			else if (result == Result.FAILURE)
				return fSuiteFailIconDescriptor;
			else
				return fSuiteIconDescriptor;
		}

		public String getText(Object element) {
			TestRunSession session = (TestRunSession) element;
			String testRunLabel = BasicElementLabels.getPHPElementName(session
					.getTestRunName());
			if (session.getStartTime() == 0) {
				return testRunLabel;
			} else {
				String startTime = DateFormat.getDateTimeInstance().format(
						new Date(session.getStartTime()));
				return Messages.format(
						PHPUnitMessages.TestRunnerViewPart_testName_startTime,
						new Object[] { testRunLabel, startTime });
			}
		}

		public void addMenuEntries(MenuManager manager) {
			manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS,
					new ImportTestRunSessionAction(fParent.getShell()));
			if (fTestRunSession != null)
				manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS,
						new ExportTestRunSessionAction(fParent.getShell(),
								fTestRunSession));
		}

		public String getMaxEntriesMessage() {
			return PHPUnitMessages.TestRunnerViewPart_max_remembered;
		}

		public int getMaxEntries() {
			IPreferenceStore store = PHPUnitPlugin.getDefault()
					.getPreferenceStore();
			return store.getInt(PHPUnitPreferencesConstants.MAX_TEST_RUNS);
		}

		public void setMaxEntries(int maxEntries) {
			IPreferenceStore store = PHPUnitPlugin.getDefault()
					.getPreferenceStore();
			store.setValue(PHPUnitPreferencesConstants.MAX_TEST_RUNS,
					maxEntries);
		}
	}

	private static class ImportTestRunSessionAction extends Action {
		private final Shell fShell;

		public ImportTestRunSessionAction(Shell shell) {
			super(
					PHPUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_name);
			fShell = shell;
		}

		public void run() {
			FileDialog importDialog = new FileDialog(fShell, SWT.OPEN);
			importDialog
					.setText(PHPUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_title);
			IDialogSettings dialogSettings = PHPUnitPlugin.getDefault()
					.getDialogSettings();
			String lastPath = dialogSettings.get(PREF_LAST_PATH);
			if (lastPath != null) {
				importDialog.setFilterPath(lastPath);
			}
			importDialog.setFilterExtensions(new String[] { "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
			String path = importDialog.open();
			if (path == null)
				return;

			// TODO: MULTI: getFileNames()
			File file = new File(path);

			try {
				PHPUnitModel.importTestRunSession(file);
			} catch (CoreException e) {
				Logger.logException(e);
				ErrorDialog
						.openError(
								fShell,
								PHPUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_error_title,
								e.getStatus().getMessage(), e.getStatus());
			}
		}
	}

	private static class ExportTestRunSessionAction extends Action {
		private final TestRunSession fTestRunSession;
		private final Shell fShell;

		public ExportTestRunSessionAction(Shell shell,
				TestRunSession testRunSession) {
			super(
					PHPUnitMessages.TestRunnerViewPart_ExportTestRunSessionAction_name);
			fShell = shell;
			fTestRunSession = testRunSession;
		}

		public void run() {
			FileDialog exportDialog = new FileDialog(fShell, SWT.SAVE);
			exportDialog
					.setText(PHPUnitMessages.TestRunnerViewPart_ExportTestRunSessionAction_title);
			IDialogSettings dialogSettings = PHPUnitPlugin.getDefault()
					.getDialogSettings();
			String lastPath = dialogSettings.get(PREF_LAST_PATH);
			if (lastPath != null) {
				exportDialog.setFilterPath(lastPath);
			}
			exportDialog.setFileName(getFileName());
			exportDialog.setFilterExtensions(new String[] { "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
			String path = exportDialog.open();
			if (path == null)
				return;

			// TODO: MULTI: getFileNames()
			File file = new File(path);

			try {
				PHPUnitModel.exportTestRunSession(fTestRunSession, file);
			} catch (CoreException e) {
				Logger.logException(e);
				ErrorDialog
						.openError(
								fShell,
								PHPUnitMessages.TestRunnerViewPart_ExportTestRunSessionAction_error_title,
								e.getStatus().getMessage(), e.getStatus());
			}
		}

		private String getFileName() {
			String testRunName = fTestRunSession.getTestRunName();
			long startTime = fTestRunSession.getStartTime();
			if (startTime == 0)
				return testRunName;

			String isoTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(startTime)); //$NON-NLS-1$
			return testRunName + " " + isoTime + ".xml"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private class TestRunSessionListener implements ITestRunSessionListener {
		public void sessionAdded(TestRunSession testRunSession) {
			TestRunSession deactivatedSession = setActiveTestRunSession(testRunSession);
			if (deactivatedSession != null)
				deactivatedSession.swapOut();
			String testRunLabel = BasicElementLabels
					.getPHPElementName(fTestRunSession.getTestRunName());
			String msg;
			if (testRunSession.getLaunch() != null) {
				msg = Messages.format(
						PHPUnitMessages.TestRunnerViewPart_Launching,
						new Object[] { testRunLabel });
			} else {
				msg = testRunLabel;
			}
			setContentDescription(msg);
		}

		public void sessionRemoved(TestRunSession testRunSession) {
			if (testRunSession.equals(fTestRunSession)) {
				List testRunSessions = PHPUnitPlugin.getModel()
						.getTestRunSessions();
				TestRunSession deactivatedSession;
				if (!testRunSessions.isEmpty()) {
					deactivatedSession = setActiveTestRunSession((TestRunSession) testRunSessions
							.get(0));
				} else {
					deactivatedSession = setActiveTestRunSession(null);
				}
				if (deactivatedSession != null)
					deactivatedSession.swapOut();
			}
		}
	}

	private class TestSessionListener implements ITestSessionListener {
		public void sessionStarted() {
			fTestViewer.registerViewersRefresh();
			fShowOnErrorOnly = getShowOnErrorOnly();

			startUpdateJobs();

			fStopAction.setEnabled(true);
			fRerunLastTestAction.setEnabled(true);
		}

		public void sessionEnded(long elapsedTime) {
			fTestViewer.registerAutoScrollTarget(null);

			String[] keys = { elapsedTimeAsString(elapsedTime) };
			String msg = Messages.format(
					PHPUnitMessages.TestRunnerViewPart_message_finish, keys);
			registerInfoMessage(msg);

			postSyncRunnable(new Runnable() {
				public void run() {
					if (isDisposed())
						return;
					fStopAction.setEnabled(lastLaunchIsKeptAlive());
					processChangesInUI();
					if (hasErrorsOrFailures()) {
						selectFirstFailure();
					}
					if (fDirtyListener == null) {
						fDirtyListener = new DirtyListener();
						// JavaCore.addElementChangedListener(fDirtyListener);
					}
					warnOfContentChange();
				}
			});
			stopUpdateJobs();
		}

		public void sessionStopped(final long elapsedTime) {
			fTestViewer.registerAutoScrollTarget(null);

			registerInfoMessage(PHPUnitMessages.TestRunnerViewPart_message_stopped);
			handleStopped();
		}

		public void sessionTerminated() {
			fTestViewer.registerAutoScrollTarget(null);

			registerInfoMessage(PHPUnitMessages.TestRunnerViewPart_message_terminated);
			handleStopped();
		}

		public void runningBegins() {
			if (!fShowOnErrorOnly)
				postShowTestResultsView();
		}

		public void testStarted(TestCaseElement testCaseElement) {
			fTestViewer.registerAutoScrollTarget(testCaseElement);
			fTestViewer.registerViewerUpdate(testCaseElement);

			String className = BasicElementLabels
					.getPHPElementName(testCaseElement.getClassName());
			String method = BasicElementLabels
					.getPHPElementName(testCaseElement.getTestMethodName());
			String status = Messages.format(
					PHPUnitMessages.TestRunnerViewPart_message_started,
					new String[] { className, method });
			registerInfoMessage(status);
		}

		public void testFailed(TestElement testElement,
				TestElement.Status status, String trace, String expected,
				String actual) {
			if (isAutoScroll()) {
				fTestViewer.registerFailedForAutoScroll(testElement);
			}
			fTestViewer.registerViewerUpdate(testElement);

			// show the view on the first error only
			if (fShowOnErrorOnly && (getErrorsPlusFailures() == 1))
				postShowTestResultsView();

			// TODO:
			// [Bug 35590] JUnit window doesn't report errors from
			// junit.extensions.TestSetup [JUnit]
			// when a failure occurs in test setup then no test is running
			// to update the views we artificially signal the end of a test run
			// if (!fTestIsRunning) {
			// fTestIsRunning= false;
			// testEnded(testCaseElement);
			// }
		}

		public void testEnded(TestCaseElement testCaseElement) {
			fTestViewer.registerViewerUpdate(testCaseElement);
		}

		public void testReran(TestCaseElement testCaseElement,
				TestElement.Status status, String trace, String expectedResult,
				String actualResult) {
			fTestViewer.registerViewerUpdate(testCaseElement); // TODO:
			// autoExpand?
			postSyncProcessChanges();
			showFailure(testCaseElement);
		}

		public void testAdded(TestElement testElement) {
			fTestViewer.registerTestAdded(testElement);
		}

		public boolean acceptsSwapToDisk() {
			return false;
		}
	}

	private class UpdateUIJob extends UIJob {
		private boolean fRunning = true;

		public UpdateUIJob(String name) {
			super(name);
			setSystem(true);
		}

		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (!isDisposed()) {
				processChangesInUI();
			}
			schedule(REFRESH_INTERVAL);
			return Status.OK_STATUS;
		}

		public void stop() {
			fRunning = false;
		}

		public boolean shouldSchedule() {
			return fRunning;
		}
	}

	private class JUnitIsRunningJob extends Job {
		public JUnitIsRunningJob(String name) {
			super(name);
			setSystem(true);
		}

		public IStatus run(IProgressMonitor monitor) {
			// wait until the test run terminates
			fJUnitIsRunningLock.acquire();
			return Status.OK_STATUS;
		}

		public boolean belongsTo(Object family) {
			return family == TestRunnerViewPart.FAMILY_PHPUNIT_RUN;
		}
	}

	private class ClearAction extends Action {
		public ClearAction() {
			setText(PHPUnitMessages.TestRunnerViewPart_clear_history_label);

			boolean enabled = false;
			List testRunSessions = PHPUnitPlugin.getModel()
					.getTestRunSessions();
			for (Iterator iter = testRunSessions.iterator(); iter.hasNext();) {
				TestRunSession testRunSession = (TestRunSession) iter.next();
				if (!testRunSession.isRunning() && !testRunSession.isStarting()) {
					enabled = true;
					break;
				}
			}
			setEnabled(enabled);
		}

		public void run() {
			List testRunSessions = getRunningSessions();
			Object first = testRunSessions.isEmpty() ? null : testRunSessions
					.get(0);
			fViewHistory.setHistoryEntries(testRunSessions, first);
		}

		private List getRunningSessions() {
			List testRunSessions = PHPUnitPlugin.getModel()
					.getTestRunSessions();
			for (Iterator iter = testRunSessions.iterator(); iter.hasNext();) {
				TestRunSession testRunSession = (TestRunSession) iter.next();
				if (!testRunSession.isRunning() && !testRunSession.isStarting()) {
					iter.remove();
				}
			}
			return testRunSessions;
		}
	}

	private class StopAction extends Action {
		public StopAction() {
			setText(PHPUnitMessages.TestRunnerViewPart_stopaction_text);
			setToolTipText(PHPUnitMessages.TestRunnerViewPart_stopaction_tooltip);
			PHPUnitPlugin.setLocalImageDescriptors(this, "stop.gif"); //$NON-NLS-1$
		}

		public void run() {
			stopTest();
			setEnabled(false);
		}
	}

	private class RerunLastAction extends Action {
		public RerunLastAction() {
			setText(PHPUnitMessages.TestRunnerViewPart_rerunaction_label);
			setToolTipText(PHPUnitMessages.TestRunnerViewPart_rerunaction_tooltip);
			PHPUnitPlugin.setLocalImageDescriptors(this, "relaunch.gif"); //$NON-NLS-1$
			setEnabled(false);
			setActionDefinitionId(RERUN_LAST_COMMAND);
		}

		public void run() {
			rerunTestRun();
		}
	}

	private class RerunLastFailedFirstAction extends Action {
		public RerunLastFailedFirstAction() {
			setText(PHPUnitMessages.TestRunnerViewPart_rerunfailuresaction_label);
			setToolTipText(PHPUnitMessages.TestRunnerViewPart_rerunfailuresaction_tooltip);
			PHPUnitPlugin.setLocalImageDescriptors(this, "relaunchf.gif"); //$NON-NLS-1$
			setEnabled(false);
			setActionDefinitionId(RERUN_FAILED_FIRST_COMMAND);
		}

		public void run() {
			rerunTestFailedFirst();
		}
	}

	private class ToggleOrientationAction extends Action {
		private final int fActionOrientation;

		public ToggleOrientationAction(int orientation) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_HORIZONTAL) {
				setText(PHPUnitMessages.TestRunnerViewPart_toggle_horizontal_label);
				setImageDescriptor(PHPUnitPlugin
						.getImageDescriptor("elcl16/th_horizontal.gif")); //$NON-NLS-1$
			} else if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_VERTICAL) {
				setText(PHPUnitMessages.TestRunnerViewPart_toggle_vertical_label);
				setImageDescriptor(PHPUnitPlugin
						.getImageDescriptor("elcl16/th_vertical.gif")); //$NON-NLS-1$
			} else if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_AUTOMATIC) {
				setText(PHPUnitMessages.TestRunnerViewPart_toggle_automatic_label);
				setImageDescriptor(PHPUnitPlugin
						.getImageDescriptor("elcl16/th_automatic.gif")); //$NON-NLS-1$
			}
			fActionOrientation = orientation;
			PlatformUI
					.getWorkbench()
					.getHelpSystem()
					.setHelp(
							this,
							IPHPUnitHelpContextIds.RESULTS_VIEW_TOGGLE_ORIENTATION_ACTION);
		}

		public int getOrientation() {
			return fActionOrientation;
		}

		public void run() {
			if (isChecked()) {
				fOrientation = fActionOrientation;
				computeOrientation();
			}
		}
	}

	/**
	 * Listen for for modifications to Java elements
	 */
	private class DirtyListener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent event) {

		}
	}

	private class FailuresOnlyFilterAction extends Action {
		public FailuresOnlyFilterAction() {
			super(PHPUnitMessages.TestRunnerViewPart_show_failures_only,
					AS_CHECK_BOX);
			setToolTipText(PHPUnitMessages.TestRunnerViewPart_show_failures_only);
			setImageDescriptor(PHPUnitPlugin
					.getImageDescriptor("obj16/failures.gif")); //$NON-NLS-1$
		}

		public void run() {
			setShowFailuresOnly(isChecked());
		}
	}

	private class ShowTimeAction extends Action {

		public ShowTimeAction() {
			super(PHPUnitMessages.TestRunnerViewPart_show_execution_time,
					IAction.AS_CHECK_BOX);
		}

		public void run() {
			setShowExecutionTime(isChecked());
		}
	}

	private class ShowTestHierarchyAction extends Action {

		public ShowTestHierarchyAction() {
			super(PHPUnitMessages.TestRunnerViewPart_hierarchical_layout,
					IAction.AS_CHECK_BOX);
			setImageDescriptor(PHPUnitPlugin
					.getImageDescriptor("elcl16/hierarchicalLayout.gif")); //$NON-NLS-1$
		}

		public void run() {
			int mode = isChecked() ? LAYOUT_HIERARCHICAL : LAYOUT_FLAT;
			setLayoutMode(mode);
		}
	}

	private class ActivateOnErrorAction extends Action {
		public ActivateOnErrorAction() {
			super(PHPUnitMessages.TestRunnerViewPart_activate_on_failure_only,
					IAction.AS_CHECK_BOX);
			//setImageDescriptor(PHPUnitPlugin.getImageDescriptor("obj16/failures.gif")); //$NON-NLS-1$
			update();
		}

		public void update() {
			setChecked(getShowOnErrorOnly());
		}

		public void run() {
			boolean checked = isChecked();
			fShowOnErrorOnly = checked;
			IPreferenceStore store = PHPUnitPlugin.getDefault()
					.getPreferenceStore();
			store.setValue(PHPUnitPreferencesConstants.SHOW_ON_ERROR_ONLY,
					checked);
		}
	}

	public TestRunnerViewPart() {
		fImagesToDispose = new ArrayList();

		fStackViewIcon = createManagedPHPUnitImage("eview16/stackframe.gif");//$NON-NLS-1$
		fTestRunOKIcon = createManagedImage("eview16/phpunitsucc.gif"); //$NON-NLS-1$
		fTestRunFailIcon = createManagedImage("eview16/phpuniterr.gif"); //$NON-NLS-1$
		fTestRunOKDirtyIcon = createManagedImage("eview16/phpunitsuccq.gif"); //$NON-NLS-1$
		fTestRunFailDirtyIcon = createManagedImage("eview16/phpuniterrq.gif"); //$NON-NLS-1$

		fTestIcon = createManagedPHPUnitImage("obj16/test.gif"); //$NON-NLS-1$
		fTestOkIcon = createManagedPHPUnitImage("obj16/testok.gif"); //$NON-NLS-1$
		fTestErrorIcon = createManagedPHPUnitImage("obj16/testerr.gif"); //$NON-NLS-1$
		fTestFailIcon = createManagedPHPUnitImage("obj16/testfail.gif"); //$NON-NLS-1$
		fTestRunningIcon = createManagedPHPUnitImage("obj16/testrun.gif"); //$NON-NLS-1$
		fTestIgnoredIcon = createManagedPHPUnitImage("obj16/testignored.gif"); //$NON-NLS-1$

		fSuiteIcon = createManagedImage(fSuiteIconDescriptor);
		fSuiteOkIcon = createManagedImage(fSuiteOkIconDescriptor);
		fSuiteErrorIcon = createManagedImage(fSuiteErrorIconDescriptor);
		fSuiteFailIcon = createManagedImage(fSuiteFailIconDescriptor);
		fSuiteRunningIcon = createManagedImage(fSuiteRunningIconDescriptor);
	}

	private Image createManagedImage(String path) {
		return createManagedImage(PHPUnitPlugin.getImageDescriptor(path));
	}

	private Image createManagedPHPUnitImage(String path) {
		return createManagedImage(PHPUnitPlugin.getImageDescriptor(path));
	}

	private Image createManagedImage(ImageDescriptor descriptor) {
		Image image = descriptor.createImage();
		if (image == null) {
			image = ImageDescriptor.getMissingImageDescriptor().createImage();
		}
		fImagesToDispose.add(image);
		return image;
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		fMemento = memento;
		IWorkbenchSiteProgressService progressService = getProgressService();
		if (progressService != null)
			progressService
					.showBusyForFamily(TestRunnerViewPart.FAMILY_PHPUNIT_RUN);
	}

	private IWorkbenchSiteProgressService getProgressService() {
		Object siteService = getSite().getAdapter(
				IWorkbenchSiteProgressService.class);
		if (siteService != null)
			return (IWorkbenchSiteProgressService) siteService;
		return null;
	}

	public void saveState(IMemento memento) {
		if (fSashForm == null) {
			// part has not been created
			if (fMemento != null) // Keep the old state;
				memento.putMemento(fMemento);
			return;
		}

		// int activePage= fTabFolder.getSelectionIndex();
		// memento.putInteger(TAG_PAGE, activePage);
		memento.putString(TAG_SCROLL,
				fScrollLockAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		int weigths[] = fSashForm.getWeights();
		int ratio = (weigths[0] * 1000) / (weigths[0] + weigths[1]);
		memento.putInteger(TAG_RATIO, ratio);
		memento.putInteger(TAG_ORIENTATION, fOrientation);

		memento.putString(TAG_FAILURES_ONLY,
				fFailuresOnlyFilterAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putInteger(TAG_LAYOUT, fLayout);
		memento.putString(TAG_SHOW_TIME,
				fShowTimeAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void restoreLayoutState(IMemento memento) {
		// Integer page= memento.getInteger(TAG_PAGE);
		// if (page != null) {
		// int p= page.intValue();
		// if (p < fTestRunTabs.size()) { // tab count can decrease if a
		// contributing plug-in is removed
		// fTabFolder.setSelection(p);
		// fActiveRunTab= (TestRunTab)fTestRunTabs.get(p);
		// }
		// }
		Integer ratio = memento.getInteger(TAG_RATIO);
		if (ratio != null)
			fSashForm.setWeights(new int[] { ratio.intValue(),
					1000 - ratio.intValue() });
		Integer orientation = memento.getInteger(TAG_ORIENTATION);
		if (orientation != null)
			fOrientation = orientation.intValue();
		computeOrientation();
		String scrollLock = memento.getString(TAG_SCROLL);
		if (scrollLock != null) {
			fScrollLockAction.setChecked(scrollLock.equals("true")); //$NON-NLS-1$
			setAutoScroll(!fScrollLockAction.isChecked());
		}

		Integer layout = memento.getInteger(TAG_LAYOUT);
		int layoutValue = LAYOUT_HIERARCHICAL;
		if (layout != null)
			layoutValue = layout.intValue();

		String failuresOnly = memento.getString(TAG_FAILURES_ONLY);
		boolean showFailuresOnly = false;
		if (failuresOnly != null)
			showFailuresOnly = failuresOnly.equals("true"); //$NON-NLS-1$

		String time = memento.getString(TAG_SHOW_TIME);
		boolean showTime = true;
		if (time != null)
			showTime = time.equals("true"); //$NON-NLS-1$

		setFilterAndLayout(showFailuresOnly, layoutValue);
		setShowExecutionTime(showTime);
	}

	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void stopTest() {
		if (fTestRunSession != null) {
			if (fTestRunSession.isRunning()) {
				setContentDescription(PHPUnitMessages.TestRunnerViewPart_message_stopping);
			}
			fTestRunSession.stopTestRun();
		}
	}

	private void startUpdateJobs() {
		postSyncProcessChanges();

		if (fUpdateJob != null) {
			return;
		}
		fJUnitIsRunningJob = new JUnitIsRunningJob(
				PHPUnitMessages.TestRunnerViewPart_wrapperJobName);
		fJUnitIsRunningLock = Job.getJobManager().newLock();
		// acquire lock while a test run is running
		// the lock is released when the test run terminates
		// the wrapper job will wait on this lock.
		fJUnitIsRunningLock.acquire();
		getProgressService().schedule(fJUnitIsRunningJob);

		fUpdateJob = new UpdateUIJob(PHPUnitMessages.TestRunnerViewPart_jobName);
		fUpdateJob.schedule(REFRESH_INTERVAL);
	}

	private void stopUpdateJobs() {
		if (fUpdateJob != null) {
			fUpdateJob.stop();
			fUpdateJob = null;
		}
		if (fJUnitIsRunningJob != null && fJUnitIsRunningLock != null) {
			fJUnitIsRunningLock.release();
			fJUnitIsRunningJob = null;
		}
		postSyncProcessChanges();
	}

	private void processChangesInUI() {
		if (fSashForm.isDisposed())
			return;

		doShowInfoMessage();
		refreshCounters();

		updateViewTitleProgress();

		boolean hasErrorsOrFailures = hasErrorsOrFailures();
		fNextAction.setEnabled(hasErrorsOrFailures);
		fPreviousAction.setEnabled(hasErrorsOrFailures);

		fTestViewer.processChangesInUI();
	}

	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void rerunTestRun() {
		if (lastLaunchIsKeptAlive()) {
			// prompt for terminating the existing run
			if (MessageDialog.openQuestion(getSite().getShell(),
					PHPUnitMessages.TestRunnerViewPart_terminate_title,
					PHPUnitMessages.TestRunnerViewPart_terminate_message)) {
				stopTest(); // TODO: wait for termination
			}
		}

		if (fTestRunSession == null)
			return;
		IFile testFile = fTestRunSession.getTestFile();
		if (testFile != null)
			relaunch(testFile);
	}

	public void rerunTestFailedFirst() {
	}

	private void relaunch(IFile testFile) {
		RunTestCaseAction action = new RunTestCaseAction();
		action.setSelectedResources(new IResource[] { testFile });
		action.run(null);
	}

	private String createFailureNamesFile() throws CoreException {
		try {
			File file = File.createTempFile("testFailures", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			file.deleteOnExit();
			TestElement[] failures = fTestRunSession.getAllFailedTestElements();
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter(file));
				for (int i = 0; i < failures.length; i++) {
					TestElement testElement = failures[i];
					bw.write(testElement.getTestName());
					bw.newLine();
				}
			} finally {
				if (bw != null) {
					bw.close();
				}
			}
			return file.getAbsolutePath();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					PHPUnitPlugin.PLUGIN_ID, IStatus.ERROR, "", e)); //$NON-NLS-1$
		}
	}

	public void setAutoScroll(boolean scroll) {
		fAutoScroll = scroll;
	}

	public boolean isAutoScroll() {
		return fAutoScroll;
	}

	public void selectNextFailure() {
		fTestViewer.selectFailure(true);
	}

	public void selectPreviousFailure() {
		fTestViewer.selectFailure(false);
	}

	protected void selectFirstFailure() {
		fTestViewer.selectFirstFailure();
	}

	private boolean hasErrorsOrFailures() {
		return getErrorsPlusFailures() > 0;
	}

	private int getErrorsPlusFailures() {
		if (fTestRunSession == null)
			return 0;
		else
			return fTestRunSession.getErrorCount()
					+ fTestRunSession.getFailureCount();
	}

	private String elapsedTimeAsString(long runTime) {
		return NumberFormat.getInstance().format((double) runTime / 1000);
	}

	private void handleStopped() {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed())
					return;
				resetViewIcon();
				fStopAction.setEnabled(false);
			}
		});
		stopUpdateJobs();
	}

	private void resetViewIcon() {
		fViewImage = fOriginalViewImage;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	private void updateViewIcon() {
		if (fTestRunSession == null || fTestRunSession.isStopped()
				|| fTestRunSession.isRunning()
				|| fTestRunSession.getStartedCount() == 0)
			fViewImage = fOriginalViewImage;
		else if (hasErrorsOrFailures())
			fViewImage = fTestRunFailIcon;
		else
			fViewImage = fTestRunOKIcon;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	private void updateViewTitleProgress() {
		if (fTestRunSession != null) {
			if (fTestRunSession.isRunning()) {
				Image progress = fProgressImages.getImage(
						fTestRunSession.getStartedCount(),
						fTestRunSession.getTotalCount(),
						fTestRunSession.getErrorCount(),
						fTestRunSession.getFailureCount());
				if (progress != fViewImage) {
					fViewImage = progress;
					firePropertyChange(IWorkbenchPart.PROP_TITLE);
				}
			} else {
				updateViewIcon();
			}
		} else {
			resetViewIcon();
		}
	}

	/**
	 * @param testRunSession
	 *            new active test run session
	 * @return deactivated session, or <code>null</code> iff no session got
	 *         deactivated
	 */
	private TestRunSession setActiveTestRunSession(TestRunSession testRunSession) {
		/*
		 * - State: fTestRunSession fTestSessionListener Jobs
		 * fTestViewer.processChangesInUI(); - UI: fCounterPanel fProgressBar
		 * setContentDescription / fInfoMessage setTitleToolTip view icons
		 * statusLine fFailureTrace
		 * 
		 * action enablement
		 */
		if (fTestRunSession == testRunSession)
			return null;

		if (fTestRunSession != null && fTestSessionListener != null) {
			fTestRunSession.removeTestSessionListener(fTestSessionListener);
			fTestSessionListener = null;
		}

		TestRunSession deactivatedSession = fTestRunSession;

		fTestRunSession = testRunSession;
		fTestViewer.registerActiveSession(testRunSession);

		if (fSashForm.isDisposed()) {
			stopUpdateJobs();
			return deactivatedSession;
		}

		if (testRunSession == null) {
			setTitleToolTip(null);
			resetViewIcon();
			clearStatus();
			fFailureTrace.clear();

			registerInfoMessage(" "); //$NON-NLS-1$
			stopUpdateJobs();

			fStopAction.setEnabled(false);
			fRerunFailedFirstAction.setEnabled(false);
			fRerunLastTestAction.setEnabled(false);

		} else {
			fTestSessionListener = new TestSessionListener();
			fTestRunSession.addTestSessionListener(fTestSessionListener);

			setTitleToolTip();

			clearStatus();
			fFailureTrace.clear();
			registerInfoMessage(BasicElementLabels
					.getPHPElementName(fTestRunSession.getTestRunName()));

			fRerunLastTestAction
					.setEnabled(fTestRunSession.getTestFile() != null);

			if (fTestRunSession.isRunning()) {
				startUpdateJobs();

				fStopAction.setEnabled(true);

			} else /* old or fresh session: don't want jobs at this stage */{
				stopUpdateJobs();

				fStopAction.setEnabled(fTestRunSession.isKeptAlive());
				fTestViewer.expandFirstLevel();
			}
		}
		return deactivatedSession;
	}

	/**
	 * @return the display name of the current test run sessions kind, or
	 *         <code>null</code>
	 */
	public String getTestKindDisplayName() {
		ITestKind kind = fTestRunSession.getTestRunnerKind();
		if (!kind.isNull()) {
			return kind.getDisplayName();
		}
		return null;
	}

	private void setTitleToolTip() {
		String testKindDisplayStr = getTestKindDisplayName();

		String testRunLabel = BasicElementLabels
				.getPHPElementName(fTestRunSession.getTestRunName());
		if (testKindDisplayStr != null)
			setTitleToolTip(MessageFormat.format(
					PHPUnitMessages.TestRunnerViewPart_titleToolTip,
					new String[] { testRunLabel, testKindDisplayStr }));
		else
			setTitleToolTip(testRunLabel);
	}

	public synchronized void dispose() {
		fIsDisposed = true;
		if (fTestRunSessionListener != null)
			PHPUnitPlugin.getModel().removeTestRunSessionListener(
					fTestRunSessionListener);

		IHandlerService handlerService = (IHandlerService) getSite()
				.getWorkbenchWindow().getService(IHandlerService.class);
		handlerService.deactivateHandler(fRerunLastActivation);
		handlerService.deactivateHandler(fRerunFailedFirstActivation);
		setActiveTestRunSession(null);

		if (fProgressImages != null)
			fProgressImages.dispose();
		getViewSite().getPage().removePartListener(fPartListener);

		disposeImages();
		if (fClipboard != null)
			fClipboard.dispose();
		if (fViewMenuListener != null) {
			getViewSite().getActionBars().getMenuManager()
					.removeMenuListener(fViewMenuListener);
		}
		if (fDirtyListener != null) {
			// JavaCore.removeElementChangedListener(fDirtyListener);
			fDirtyListener = null;
		}
	}

	private void disposeImages() {
		for (int i = 0; i < fImagesToDispose.size(); i++) {
			((Image) fImagesToDispose.get(i)).dispose();
		}
	}

	private void postSyncRunnable(Runnable r) {
		if (!isDisposed())
			getDisplay().syncExec(r);
	}

	private void refreshCounters() {
		// TODO: Inefficient. Either
		// - keep a boolean fHasTestRun and update only on changes, or
		// - improve components to only redraw on changes (once!).

		int startedCount;
		int ignoredCount;
		int totalCount;
		int errorCount;
		int failureCount;
		boolean hasErrorsOrFailures;
		boolean stopped;

		if (fTestRunSession != null) {
			startedCount = fTestRunSession.getStartedCount();
			ignoredCount = fTestRunSession.getIgnoredCount();
			totalCount = fTestRunSession.getTotalCount();
			errorCount = fTestRunSession.getErrorCount();
			failureCount = fTestRunSession.getFailureCount();
			hasErrorsOrFailures = errorCount + failureCount > 0;
			stopped = fTestRunSession.isStopped();
		} else {
			startedCount = 0;
			ignoredCount = 0;
			totalCount = 0;
			errorCount = 0;
			failureCount = 0;
			hasErrorsOrFailures = false;
			stopped = false;
		}

		fCounterPanel.setTotal(totalCount);
		fCounterPanel.setRunValue(startedCount, ignoredCount);
		fCounterPanel.setErrorValue(errorCount);
		fCounterPanel.setFailureValue(failureCount);

		int ticksDone;
		if (startedCount == 0)
			ticksDone = 0;
		else if (startedCount == totalCount && !fTestRunSession.isRunning())
			ticksDone = totalCount;
		else
			ticksDone = startedCount - 1;

		fProgressBar.reset(hasErrorsOrFailures, stopped, ticksDone, totalCount);
	}

	protected void postShowTestResultsView() {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed())
					return;
				showTestResultsView();
			}
		});
	}

	public static void showTestResultsView() {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			TestRunnerViewPart testRunner = null;

			if (page != null) {
				try { // show the result view
					testRunner = (TestRunnerViewPart) page
							.findView(TestRunnerViewPart.NAME);
					if (testRunner == null) {
						IWorkbenchPart activePart = page.getActivePart();
						testRunner = (TestRunnerViewPart) page
								.showView(TestRunnerViewPart.NAME);
						// restore focus
						page.activate(activePart);
					} else {
						page.bringToTop(testRunner);
					}
				} catch (PartInitException pie) {
					Logger.logException(pie);
				}
			}
		}
	}

	protected void doShowInfoMessage() {
		if (fInfoMessage != null) {
			setContentDescription(fInfoMessage);
			fInfoMessage = null;
		}
	}

	protected void registerInfoMessage(String message) {
		fInfoMessage = message;
	}

	private SashForm createSashForm(Composite parent) {
		fSashForm = new SashForm(parent, SWT.VERTICAL);

		ViewForm top = new ViewForm(fSashForm, SWT.NONE);

		Composite empty = new Composite(top, SWT.NONE);
		empty.setLayout(new Layout() {
			protected Point computeSize(Composite composite, int wHint,
					int hHint, boolean flushCache) {
				return new Point(1, 1); // (0, 0) does not work with
				// super-intelligent ViewForm
			}

			protected void layout(Composite composite, boolean flushCache) {
			}
		});
		top.setTopLeft(empty); // makes ViewForm draw the horizontal separator
		// line ...
		fTestViewer = new TestViewer(top, fClipboard, this);
		top.setContent(fTestViewer.getTestViewerControl());

		ViewForm bottom = new ViewForm(fSashForm, SWT.NONE);

		CLabel label = new CLabel(bottom, SWT.NONE);
		label.setText(PHPUnitMessages.TestRunnerViewPart_label_failure);
		label.setImage(fStackViewIcon);
		bottom.setTopLeft(label);
		ToolBar failureToolBar = new ToolBar(bottom, SWT.FLAT | SWT.WRAP);
		bottom.setTopCenter(failureToolBar);
		fFailureTrace = new FailureTrace(bottom, fClipboard, this,
				failureToolBar);
		bottom.setContent(fFailureTrace.getComposite());

		fSashForm.setWeights(new int[] { 50, 50 });
		return fSashForm;
	}

	private void clearStatus() {
		getStatusLine().setMessage(null);
		getStatusLine().setErrorMessage(null);
	}

	public void setFocus() {
		if (fTestViewer != null)
			fTestViewer.getTestViewerControl().setFocus();
	}

	public void createPartControl(Composite parent) {
		fParent = parent;
		addResizeListener(parent);
		fClipboard = new Clipboard(parent.getDisplay());

		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		parent.setLayout(gridLayout);

		fViewHistory = new RunnerViewHistory();
		configureToolBar();

		fCounterComposite = createProgressCountPanel(parent);
		fCounterComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		SashForm sashForm = createSashForm(parent);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		IActionBars actionBars = getViewSite().getActionBars();
		fCopyAction = new PHPUnitCopyAction(fFailureTrace, fClipboard);
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
				fCopyAction);
		initPageSwitcher();

		fOriginalViewImage = getTitleImage();
		fProgressImages = new ProgressImages();
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(parent, IPHPUnitHelpContextIds.RESULTS_VIEW);

		getViewSite().getPage().addPartListener(fPartListener);

		setFilterAndLayout(false, LAYOUT_HIERARCHICAL);
		setShowExecutionTime(true);
		if (fMemento != null) {
			restoreLayoutState(fMemento);
		}
		fMemento = null;

		fTestRunSessionListener = new TestRunSessionListener();
		PHPUnitPlugin.getModel().addTestRunSessionListener(
				fTestRunSessionListener);
		List sessions = PHPUnitPlugin.getModel().getTestRunSessions();
		if (sessions.size() > 0) {
			Object lastSession = sessions.get(0);
			if (lastSession instanceof TestRunSession)
				fTestRunSessionListener
						.sessionAdded((TestRunSession) lastSession);
		}
	}

	private void initPageSwitcher() {
		new PageSwitcher(this) {
			public Object[] getPages() {
				return fViewHistory.getHistoryEntries().toArray();
			}

			public String getName(Object page) {
				return fViewHistory.getText(page);
			}

			public ImageDescriptor getImageDescriptor(Object page) {
				return fViewHistory.getImageDescriptor(page);
			}

			public void activatePage(Object page) {
				fViewHistory.setActiveEntry(page);
			}

			public int getCurrentPageIndex() {
				return fViewHistory.getHistoryEntries().indexOf(
						fViewHistory.getCurrentEntry());
			}
		};
	}

	private void addResizeListener(Composite parent) {
		parent.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {
			}

			public void controlResized(ControlEvent e) {
				computeOrientation();
			}
		});
	}

	void computeOrientation() {
		if (fOrientation != VIEW_ORIENTATION_AUTOMATIC) {
			fCurrentOrientation = fOrientation;
			setOrientation(fCurrentOrientation);
		} else {
			Point size = fParent.getSize();
			if (size.x != 0 && size.y != 0) {
				if (size.x > size.y)
					setOrientation(VIEW_ORIENTATION_HORIZONTAL);
				else
					setOrientation(VIEW_ORIENTATION_VERTICAL);
			}
		}
	}

	private void configureToolBar() {
		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager toolBar = actionBars.getToolBarManager();
		IMenuManager viewMenu = actionBars.getMenuManager();

		fNextAction = new ShowNextFailureAction(this);
		fNextAction.setEnabled(false);
		actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(),
				fNextAction);

		fPreviousAction = new ShowPreviousFailureAction(this);
		fPreviousAction.setEnabled(false);
		actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(),
				fPreviousAction);

		fStopAction = new StopAction();
		fStopAction.setEnabled(false);

		fRerunLastTestAction = new RerunLastAction();
		IHandlerService handlerService = (IHandlerService) getSite()
				.getWorkbenchWindow().getService(IHandlerService.class);
		IHandler handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event)
					throws ExecutionException {
				fRerunLastTestAction.run();
				return null;
			}

			public boolean isEnabled() {
				return fRerunLastTestAction.isEnabled();
			}
		};
		fRerunLastActivation = handlerService.activateHandler(
				RERUN_LAST_COMMAND, handler);

		fRerunFailedFirstAction = new RerunLastFailedFirstAction();
		handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event)
					throws ExecutionException {
				fRerunFailedFirstAction.run();
				return null;
			}

			public boolean isEnabled() {
				return fRerunFailedFirstAction.isEnabled();
			}
		};
		fRerunFailedFirstActivation = handlerService.activateHandler(
				RERUN_FAILED_FIRST_COMMAND, handler);

		fFailuresOnlyFilterAction = new FailuresOnlyFilterAction();

		fScrollLockAction = new ScrollLockAction(this);
		fScrollLockAction.setChecked(!fAutoScroll);

		fToggleOrientationActions = new ToggleOrientationAction[] {
				new ToggleOrientationAction(VIEW_ORIENTATION_VERTICAL),
				new ToggleOrientationAction(VIEW_ORIENTATION_HORIZONTAL),
				new ToggleOrientationAction(VIEW_ORIENTATION_AUTOMATIC) };

		fShowTestHierarchyAction = new ShowTestHierarchyAction();
		fShowTimeAction = new ShowTimeAction();

		toolBar.add(fNextAction);
		toolBar.add(fPreviousAction);
		toolBar.add(fFailuresOnlyFilterAction);
		toolBar.add(fScrollLockAction);
		toolBar.add(new Separator());
		toolBar.add(fRerunLastTestAction);
		// toolBar.add(fRerunFailedFirstAction);
		// toolBar.add(fStopAction);
		toolBar.add(fViewHistory.createHistoryDropDownAction());

		viewMenu.add(fShowTestHierarchyAction);
		viewMenu.add(fShowTimeAction);
		viewMenu.add(new Separator());

		MenuManager layoutSubMenu = new MenuManager(
				PHPUnitMessages.TestRunnerViewPart_layout_menu);
		for (int i = 0; i < fToggleOrientationActions.length; ++i) {
			layoutSubMenu.add(fToggleOrientationActions[i]);
		}
		viewMenu.add(layoutSubMenu);
		viewMenu.add(new Separator());

		viewMenu.add(fFailuresOnlyFilterAction);

		fActivateOnErrorAction = new ActivateOnErrorAction();
		viewMenu.add(fActivateOnErrorAction);
		fViewMenuListener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fActivateOnErrorAction.update();
			}
		};

		viewMenu.addMenuListener(fViewMenuListener);

		actionBars.updateActionBars();
	}

	private IStatusLineManager getStatusLine() {
		// we want to show messages globally hence we
		// have to go through the active part
		IViewSite site = getViewSite();
		IWorkbenchPage page = site.getPage();
		IWorkbenchPart activePart = page.getActivePart();

		if (activePart instanceof IViewPart) {
			IViewPart activeViewPart = (IViewPart) activePart;
			IViewSite activeViewSite = activeViewPart.getViewSite();
			return activeViewSite.getActionBars().getStatusLineManager();
		}

		if (activePart instanceof IEditorPart) {
			IEditorPart activeEditorPart = (IEditorPart) activePart;
			IEditorActionBarContributor contributor = activeEditorPart
					.getEditorSite().getActionBarContributor();
			if (contributor instanceof EditorActionBarContributor)
				return ((EditorActionBarContributor) contributor)
						.getActionBars().getStatusLineManager();
		}
		// no active part
		return getViewSite().getActionBars().getStatusLineManager();
	}

	protected Composite createProgressCountPanel(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		setCounterColumns(layout);

		fCounterPanel = new CounterPanel(composite);
		fCounterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		fProgressBar = new PHPUnitProgressBar(composite);
		fProgressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		return composite;
	}

	public void handleTestSelected(TestElement test) {
		showFailure(test);
		fCopyAction.handleTestSelected(test);
	}

	private void showFailure(final TestElement test) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed())
					fFailureTrace.showFailure(test);
			}
		});
	}

	/**
	 * @return the Java project, or <code>null</code>
	 */
	public IProject getLaunchedProject() {
		// return fTestRunSession == null ? null :
		// fTestRunSession.getLaunchedProject();
		return null;
	}

	private boolean isDisposed() {
		return fIsDisposed || fCounterPanel.isDisposed();
	}

	private Display getDisplay() {
		return getViewSite().getShell().getDisplay();
	}

	/*
	 * @see IWorkbenchPart#getTitleImage()
	 */
	public Image getTitleImage() {
		if (fOriginalViewImage == null)
			fOriginalViewImage = super.getTitleImage();

		if (fViewImage == null)
			return super.getTitleImage();
		return fViewImage;
	}

	void codeHasChanged() {
		if (fDirtyListener != null) {
			// JavaCore.removeElementChangedListener(fDirtyListener);
			fDirtyListener = null;
		}
		if (fViewImage == fTestRunOKIcon)
			fViewImage = fTestRunOKDirtyIcon;
		else if (fViewImage == fTestRunFailIcon)
			fViewImage = fTestRunFailDirtyIcon;

		Runnable r = new Runnable() {
			public void run() {
				if (isDisposed())
					return;
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		};
		if (!isDisposed())
			getDisplay().asyncExec(r);
	}

	public boolean isCreated() {
		return fCounterPanel != null;
	}

	public void rerunTest(String testId, String className, String testName,
			String launchMode) {
		// try {
		// boolean couldLaunch = fTestRunSession.rerunTest(testId, className,
		// testName, launchMode);
		// if (!couldLaunch) {
		// MessageDialog.openInformation(getSite().getShell(),
		// JUnitMessages.TestRunnerViewPart_cannotrerun_title,
		// JUnitMessages.TestRunnerViewPart_cannotrerurn_message);
		// } else if (fTestRunSession.isKeptAlive()) {
		// TestCaseElement testCaseElement = (TestCaseElement)
		// fTestRunSession.getTestElement(testId);
		// testCaseElement.setStatus(TestElement.Status.RUNNING, null, null,
		// null);
		// fTestViewer.registerViewerUpdate(testCaseElement);
		// postSyncProcessChanges();
		// }
		//
		// } catch (CoreException e) {
		// ErrorDialog.openError(getSite().getShell(),
		// JUnitMessages.TestRunnerViewPart_error_cannotrerun, e
		// .getMessage(), e.getStatus());
		// }
	}

	private void postSyncProcessChanges() {
		postSyncRunnable(new Runnable() {
			public void run() {
				processChangesInUI();
			}
		});
	}

	public void warnOfContentChange() {
		IWorkbenchSiteProgressService service = getProgressService();
		if (service != null)
			service.warnOfContentChange();
	}

	public boolean lastLaunchIsKeptAlive() {
		return fTestRunSession != null && fTestRunSession.isKeptAlive();
	}

	private void setOrientation(int orientation) {
		if ((fSashForm == null) || fSashForm.isDisposed())
			return;
		boolean horizontal = orientation == VIEW_ORIENTATION_HORIZONTAL;
		fSashForm.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
		for (int i = 0; i < fToggleOrientationActions.length; ++i)
			fToggleOrientationActions[i]
					.setChecked(fOrientation == fToggleOrientationActions[i]
							.getOrientation());
		fCurrentOrientation = orientation;
		GridLayout layout = (GridLayout) fCounterComposite.getLayout();
		setCounterColumns(layout);
		fParent.layout();
	}

	private void setCounterColumns(GridLayout layout) {
		if (fCurrentOrientation == VIEW_ORIENTATION_HORIZONTAL)
			layout.numColumns = 2;
		else
			layout.numColumns = 1;
	}

	private static boolean getShowOnErrorOnly() {
		IPreferenceStore store = PHPUnitPlugin.getDefault()
				.getPreferenceStore();
		return store.getBoolean(PHPUnitPreferencesConstants.SHOW_ON_ERROR_ONLY);
	}

	public FailureTrace getFailureTrace() {
		return fFailureTrace;
	}

	void setShowFailuresOnly(boolean failuresOnly) {
		setFilterAndLayout(failuresOnly, fLayout);
	}

	private void setLayoutMode(int mode) {
		setFilterAndLayout(fFailuresOnlyFilterAction.isChecked(), mode);
	}

	private void setFilterAndLayout(boolean failuresOnly, int layoutMode) {
		fShowTestHierarchyAction.setChecked(layoutMode == LAYOUT_HIERARCHICAL);
		fLayout = layoutMode;
		fFailuresOnlyFilterAction.setChecked(failuresOnly);
		fTestViewer.setShowFailuresOnly(failuresOnly, layoutMode);
	}

	private void setShowExecutionTime(boolean showTime) {
		fTestViewer.setShowTime(showTime);
		fShowTimeAction.setChecked(showTime);

	}

	TestElement[] getAllFailures() {
		return fTestRunSession.getAllFailedTestElements();
	}
}
