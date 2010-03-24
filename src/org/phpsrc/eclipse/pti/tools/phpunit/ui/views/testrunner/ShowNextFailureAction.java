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

import org.eclipse.jface.action.Action;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;

class ShowNextFailureAction extends Action {

	private TestRunnerViewPart fPart;

	public ShowNextFailureAction(TestRunnerViewPart part) {
		super(PHPUnitMessages.ShowNextFailureAction_label);
		setDisabledImageDescriptor(PHPUnitPlugin.getImageDescriptor("dlcl16/select_next.gif")); //$NON-NLS-1$
		setHoverImageDescriptor(PHPUnitPlugin.getImageDescriptor("elcl16/select_next.gif")); //$NON-NLS-1$
		setImageDescriptor(PHPUnitPlugin.getImageDescriptor("elcl16/select_next.gif")); //$NON-NLS-1$
		setToolTipText(PHPUnitMessages.ShowNextFailureAction_tooltip);
		fPart = part;
	}

	public void run() {
		fPart.selectNextFailure();
	}
}
