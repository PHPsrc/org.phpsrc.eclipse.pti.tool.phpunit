package org.phpsrc.eclipse.pti.tools.phpunit.ui.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.phpsrc.eclipse.pti.core.PHPToolCorePlugin;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnit;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnitToolkitUtil;

public class ToogleTestCaseTestElementAction implements
		IWorkbenchWindowActionDelegate {
	public void run(IAction action) {
		IWorkbenchPage page = PHPUnitPlugin.getActivePage();
		if (page != null) {
			IEditorPart editor = page.getActiveEditor();
			if (editor != null) {
				IEditorInput input = editor.getEditorInput();
				if (input != null) {
					IFile file = (IFile) input.getAdapter(IFile.class);
					if (file != null) {
						if (PHPUnit.isTestSuite(file))
							return;

						IFile targetFile = null;
						String errorMsg = null;
						if (PHPUnit.isTestCase(file)) {
							targetFile = PHPUnit.searchTestElement(file);
							errorMsg = "Can't find php class for test case class ";
						} else {
							targetFile = PHPUnit.searchTestCase(file);
							errorMsg = "Can't find test case class for php class ";
						}

						if (!openFile(page, targetFile)) {
							String sourceClassName = PHPUnitToolkitUtil
									.getClassNameWithNamespace(file);
							if (sourceClassName == null)
								sourceClassName = "unknown";

							MessageDialog
									.openError(PHPToolCorePlugin
											.getActiveWorkbenchShell(),
											"Error", errorMsg + "'"
													+ sourceClassName + "'");
						}
					}
				}
			}
		}
	}

	protected boolean openFile(IWorkbenchPage page, IFile file) {
		if (page != null && file != null) {
			IEditorDescriptor desc = PlatformUI.getWorkbench()
					.getEditorRegistry().getDefaultEditor(file.getName());

			try {
				page.openEditor(new FileEditorInput(file), desc.getId());
				return true;
			} catch (PartInitException e) {
			}
		}
		return false;
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}
}
