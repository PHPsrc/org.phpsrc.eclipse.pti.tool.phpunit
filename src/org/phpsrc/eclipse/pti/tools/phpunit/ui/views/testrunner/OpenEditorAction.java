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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.search.SearchMatch;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.phpsrc.eclipse.pti.core.search.PHPSearchEngine;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;

/**
 * Abstract Action for opening a Java editor.
 */
public abstract class OpenEditorAction extends Action {
	protected IFile fFile;
	protected TestRunnerViewPart fTestRunner;
	private final boolean fActivate;

	protected OpenEditorAction(TestRunnerViewPart testRunner, String filePath) {
		this(testRunner, filePath, true);
	}

	public OpenEditorAction(TestRunnerViewPart testRunner, String filePath, boolean activate) {
		this(testRunner, PHPUnitPlugin.resolveProjectFile(filePath), activate);
	}

	public OpenEditorAction(TestRunnerViewPart testRunner, IFile file, boolean activate) {
		super(PHPUnitMessages.OpenEditorAction_action_label);
		Assert.isNotNull(file);
		Assert.isTrue(file.exists());
		fFile = file;
		fTestRunner = testRunner;
		fActivate = activate;
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(fFile.getName());

			try {
				IEditorPart editor = page.openEditor(new FileEditorInput(fFile), desc.getId());
				if (editor instanceof ITextEditor)
					reveal((ITextEditor) editor);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		}
	}

	protected Shell getShell() {
		return fTestRunner.getSite().getShell();
	}

	/**
	 * @return the Java project, or <code>null</code>
	 */
	protected IProject getLaunchedProject() {
		return fTestRunner.getLaunchedProject();
	}

	protected IFile getFile() {
		return fFile;
	}

	protected static IFile getFileForClassName(String className, String method) {
		SearchMatch[] matches = PHPSearchEngine.findClass(className);
		if (matches != null && matches.length > 0)
			return (IFile) matches[0].getResource();

		return null;
	}

	protected abstract void reveal(ITextEditor editor);
}
