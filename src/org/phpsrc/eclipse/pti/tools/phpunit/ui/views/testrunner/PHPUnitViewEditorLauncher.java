/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - bug fixes
 *     Brock Janiczak <brockj@tpg.com.au> - [JUnit] Add context menu action to import junit test results from package explorer - https://bugs.eclipse.org/bugs/show_bug.cgi?id=213786
 *******************************************************************************/
package org.phpsrc.eclipse.pti.tools.phpunit.ui.views.testrunner;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.php.internal.ui.util.ExceptionHandler;
import org.eclipse.ui.IEditorLauncher;
import org.phpsrc.eclipse.pti.tools.phpunit.core.model.PHPUnitModel;

public class PHPUnitViewEditorLauncher implements IEditorLauncher {

	public void open(IPath file) {
		try {
			TestRunnerViewPart.showTestResultsView();
			PHPUnitModel.importTestRunSession(file.toFile());
		} catch (CoreException e) {
			ExceptionHandler.handle(e, PHPUnitMessages.JUnitViewEditorLauncher_dialog_title,
					PHPUnitMessages.JUnitViewEditorLauncher_error_occurred);
		}
	}

}