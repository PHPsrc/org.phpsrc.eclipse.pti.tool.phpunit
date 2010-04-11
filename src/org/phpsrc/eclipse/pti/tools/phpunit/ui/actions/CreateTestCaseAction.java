/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.dltk.core.IOpenable;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.phpsrc.eclipse.pti.core.PHPToolkitUtil;
import org.phpsrc.eclipse.pti.tools.phpunit.ui.wizards.CreatePHPUnitTestCaseWizard;
import org.phpsrc.eclipse.pti.ui.Logger;

public class CreateTestCaseAction implements IObjectActionDelegate, IEditorActionDelegate {
	private IResource[] files;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		ISelection selection = targetPart.getSite().getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			files = new IResource[structuredSelection.size()];

			ArrayList<IResource> resources = new ArrayList<IResource>(structuredSelection.size());

			Iterator<?> iterator = structuredSelection.iterator();
			while (iterator.hasNext()) {
				Object entry = iterator.next();
				try {
					if (entry instanceof ISourceModule) {
						IFile file = (IFile) ((ISourceModule) entry).getCorrespondingResource();

						if (PHPToolkitUtil.isPhpFile(file)) {
							resources.add(((ISourceModule) entry).getCorrespondingResource());
						}
					} else if (entry instanceof IOpenable) {
						resources.add(((IOpenable) entry).getCorrespondingResource());
					}
				} catch (ModelException e) {
					Logger.logException(e);
				}
			}

			files = resources.toArray(new IResource[0]);
		}
	}

	public void run(IAction action) {
		if (files != null) {
			for (IResource file : files) {
				if (file instanceof IFile) {
					createTestCase((IFile) file);
				}
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void setActiveEditor(IAction action, IEditorPart targetPart) {
		if (targetPart != null) {
			IEditorInput iei = targetPart.getEditorInput();
			if (iei instanceof IFileEditorInput) {
				IFileEditorInput ifei = (IFileEditorInput) iei;
				files = new IResource[] { ifei.getFile() };
			}
		}
	}

	protected void createTestCase(IFile file) {
		CreatePHPUnitTestCaseWizard wizard = new CreatePHPUnitTestCaseWizard();
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());

		ISourceModule module = PHPToolkitUtil.getSourceModule(file);

		try {
			IType[] types = module.getAllTypes();
			for (IType type : types) {
				WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						wizard);
				dialog.create();
				wizard.setSourceClassName(type.getElementName(), file);
				dialog.open();
			}
		} catch (ModelException e) {
			Logger.logException(e);
		}

	}
}
