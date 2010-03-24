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
package org.phpsrc.eclipse.pti.tools.phpunit.ui.views.testrunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.TestElement;

/**
 * Copies a test failure stack trace to the clipboard.
 */
public class PHPUnitCopyAction extends SelectionListenerAction {
	private FailureTrace fView;

	private final Clipboard fClipboard;

	private TestElement fTestElement;

	public PHPUnitCopyAction(FailureTrace view, Clipboard clipboard) {
		super(PHPUnitMessages.CopyTrace_action_label);
		Assert.isNotNull(clipboard);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IPHPUnitHelpContextIds.COPYTRACE_ACTION);
		fView = view;
		fClipboard = clipboard;
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		String trace = fView.getTrace();
		String source = null;
		if (trace != null) {
			source = convertLineTerminators(trace);
		} else if (fTestElement != null) {
			source = fTestElement.getTestName();
		}
		if (source == null || source.length() == 0)
			return;

		TextTransfer plainTextTransfer = TextTransfer.getInstance();
		try {
			fClipboard.setContents(new String[] { convertLineTerminators(source) },
					new Transfer[] { plainTextTransfer });
		} catch (SWTError e) {
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
				throw e;
			if (MessageDialog.openQuestion(fView.getComposite().getShell(), PHPUnitMessages.CopyTraceAction_problem,
					PHPUnitMessages.CopyTraceAction_clipboard_busy))
				run();
		}
	}

	public void handleTestSelected(TestElement test) {
		fTestElement = test;
	}

	private String convertLineTerminators(String in) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		StringReader stringReader = new StringReader(in);
		BufferedReader bufferedReader = new BufferedReader(stringReader);
		String line;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				printWriter.println(line);
			}
		} catch (IOException e) {
			return in; // return the trace unfiltered
		}
		return stringWriter.toString();
	}
}
