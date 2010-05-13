/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.ui.wizards;

import java.io.InvalidClassException;
import java.io.InvalidObjectException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.php.internal.ui.PHPUiConstants;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.FileEditorInput;
import org.phpsrc.eclipse.pti.core.PHPToolCorePlugin;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnit;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnitException;
import org.phpsrc.eclipse.pti.ui.Logger;

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
			PHPUnit phpunit = PHPUnit.getInstance();
			try {
				try {
					phpunit.createPHPClassSkeleton(sourceClassPage.getSourceClassName(), sourceClassPage
							.getSourceClassFile(), sourceClassPage.getTestClassName(), sourceClassPage
							.getPHPClassFilePath(), sourceClassPage.getPHPClassSuperClass());

					Path path = new Path(sourceClassPage.getPHPClassFilePath());
					IFile testFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
					IEditorInput editorInput = new FileEditorInput(testFile);
					IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
					IWorkbenchPage page = window.getActivePage();
					page.openEditor(editorInput, PHPUiConstants.PHP_EDITOR_ID);

					return true;
				} catch (PHPUnitException e) {
					MessageDialog.openError(PHPToolCorePlugin.getActiveWorkbenchShell(), "Failed creating PHP Class", e
							.getMessage());
				}
			} catch (InvalidObjectException e) {
				Logger.logException(e);
			} catch (CoreException e) {
				Logger.logException(e);
			} catch (InvalidClassException e) {
				Logger.logException(e);
			}
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