/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.ui.views.testrunner;

import java.text.NumberFormat;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.phpsrc.eclipse.pti.core.Messages;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.ITestCaseElement;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.ITestElement;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.ITestRunSession;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.ITestSuiteElement;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.TestCaseElement;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.TestElement.Status;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.TestSuiteElement;
import org.phpsrc.eclipse.pti.ui.viewsupport.BasicElementLabels;

public class TestSessionLabelProvider extends LabelProvider implements
		IStyledLabelProvider {

	private final TestRunnerViewPart fTestRunnerPart;
	private final int fLayoutMode;
	private final NumberFormat timeFormat;

	private boolean fShowTime;

	public TestSessionLabelProvider(TestRunnerViewPart testRunnerPart,
			int layoutMode) {
		fTestRunnerPart = testRunnerPart;
		fLayoutMode = layoutMode;
		fShowTime = true;

		timeFormat = NumberFormat.getNumberInstance();
		timeFormat.setGroupingUsed(true);
		timeFormat.setMinimumFractionDigits(3);
		timeFormat.setMaximumFractionDigits(3);
		timeFormat.setMinimumIntegerDigits(1);
	}

	public StyledString getStyledText(Object element) {
		String label = getSimpleLabel(element);
		if (label == null) {
			return new StyledString(element.toString());
		}
		StyledString text = new StyledString(label);

		ITestElement testElement = (ITestElement) element;
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL) {
			if (testElement.getParentContainer() instanceof ITestRunSession) {
				String testKindDisplayName = fTestRunnerPart
						.getTestKindDisplayName();
				if (testKindDisplayName != null) {
					String decorated = Messages
							.format(PHPUnitMessages.TestSessionLabelProvider_testName_JUnitVersion,
									new Object[] { label, testKindDisplayName });
					text = StyledCellLabelProvider.styleDecoratedString(
							decorated, StyledString.QUALIFIER_STYLER, text);
				}
			}

		} else {
			if (element instanceof ITestCaseElement) {
				String className = BasicElementLabels
						.getPHPElementName(((ITestCaseElement) element)
								.getTestClassName());
				String decorated = Messages
						.format(PHPUnitMessages.TestSessionLabelProvider_testMethodName_className,
								new Object[] { label, className });
				text = StyledCellLabelProvider.styleDecoratedString(decorated,
						StyledString.QUALIFIER_STYLER, text);
			}
		}
		return addElapsedTime(text, testElement.getElapsedTimeInSeconds());
	}

	private StyledString addElapsedTime(StyledString styledString, double time) {
		String string = styledString.getString();
		String decorated = addElapsedTime(string, time);
		return StyledCellLabelProvider.styleDecoratedString(decorated,
				StyledString.COUNTER_STYLER, styledString);
	}

	private String addElapsedTime(String string, double time) {
		if (!fShowTime || Double.isNaN(time)) {
			return string;
		}
		String formattedTime = timeFormat.format(time);
		return Messages
				.format(PHPUnitMessages.TestSessionLabelProvider_testName_elapsedTimeInSeconds,
						new String[] { string, formattedTime });
	}

	private String getSimpleLabel(Object element) {
		if (element instanceof ITestSuiteElement) {
			return BasicElementLabels
					.getPHPElementName(((ITestSuiteElement) element)
							.getSuiteTypeName());
		} else if (element instanceof ITestCaseElement) {
			String label = ((ITestCaseElement) element).getTestMethodName();
			String testData = ((ITestCaseElement) element).getTestData();
			if (testData != null)
				label += " " + testData;

			return BasicElementLabels.getPHPElementName(label);
		}
		return null;
	}

	public String getText(Object element) {
		String label = getSimpleLabel(element);
		if (label == null) {
			return element.toString();
		}
		ITestElement testElement = (ITestElement) element;
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL) {
			if (testElement.getParentContainer() instanceof ITestRunSession) {
				String testKindDisplayName = fTestRunnerPart
						.getTestKindDisplayName();
				if (testKindDisplayName != null) {
					label = Messages
							.format(PHPUnitMessages.TestSessionLabelProvider_testName_JUnitVersion,
									new Object[] { label, testKindDisplayName });
				}
			}
		} else {
			if (element instanceof ITestCaseElement) {
				String className = BasicElementLabels
						.getPHPElementName(((ITestCaseElement) element)
								.getTestClassName());
				label = Messages
						.format(PHPUnitMessages.TestSessionLabelProvider_testMethodName_className,
								new Object[] { label, className });
			}
		}
		return addElapsedTime(label, testElement.getElapsedTimeInSeconds());
	}

	public Image getImage(Object element) {
		if (element instanceof TestCaseElement) {
			TestCaseElement testCaseElement = ((TestCaseElement) element);
			if (testCaseElement.isIgnored())
				return fTestRunnerPart.fTestIgnoredIcon;

			Status status = testCaseElement.getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fTestIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fTestRunningIcon;
			else if (status.isError())
				return fTestRunnerPart.fTestErrorIcon;
			else if (status.isFailure())
				return fTestRunnerPart.fTestFailIcon;
			else if (status.isOK())
				return fTestRunnerPart.fTestOkIcon;
			else
				throw new IllegalStateException(element.toString());

		} else if (element instanceof TestSuiteElement) {
			Status status = ((TestSuiteElement) element).getStatus();
			if (status.isNotRun())
				return fTestRunnerPart.fSuiteIcon;
			else if (status.isRunning())
				return fTestRunnerPart.fSuiteRunningIcon;
			else if (status.isError())
				return fTestRunnerPart.fSuiteErrorIcon;
			else if (status.isFailure())
				return fTestRunnerPart.fSuiteFailIcon;
			else if (status.isOK())
				return fTestRunnerPart.fSuiteOkIcon;
			else
				throw new IllegalStateException(element.toString());

		} else {
			throw new IllegalArgumentException(String.valueOf(element));
		}
	}

	public void setShowTime(boolean showTime) {
		fShowTime = showTime;
		fireLabelProviderChanged(new LabelProviderChangedEvent(this));
	}

}
