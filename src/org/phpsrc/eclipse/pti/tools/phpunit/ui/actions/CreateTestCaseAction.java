/*******************************************************************************
 * Copyright (c) 2010, Sven Kiera
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Organisation nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
