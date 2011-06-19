/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnitUtil;

@SuppressWarnings("restriction")
public class CreatePHPClassWizard extends Wizard implements INewWizard {

	private IWorkbench workbench;
	private IStructuredSelection selection;
	private PHPClassCreationWizardPage sourceClassPage;

	public CreatePHPClassWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	public boolean performFinish() {
		if (sourceClassPage.finish()) {
			return PHPUnitUtil.syncTestCaseToPHPClass(
					sourceClassPage.getSourceClassName(),
					sourceClassPage.getSourceClassFile(),
					sourceClassPage.getPHPClassName(),
					sourceClassPage.getPHPClassFilePath(),
					sourceClassPage.getPHPClassSuperClass());
		}
		return false;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	public void addPages() {
		sourceClassPage = new PHPClassCreationWizardPage(selection);
		addPage(sourceClassPage);
	}

	public boolean setTestCaseClassName(String className) {
		return sourceClassPage.setTestCaseClassName(className);
	}

	public boolean setTestCaseClassName(String className, IDLTKSearchScope scope) {
		return sourceClassPage.setTestCaseClassName(className, scope);
	}

	public boolean setTestCaseClassName(String className, IResource classFile) {
		return sourceClassPage.setTestCaseClassName(className, classFile);
	}
}