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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.phpsrc.eclipse.pti.core.jobs.MutexRule;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnit;
import org.phpsrc.eclipse.pti.tools.phpunit.validator.PHPUnitValidator;
import org.phpsrc.eclipse.pti.ui.Logger;
import org.phpsrc.eclipse.pti.ui.actions.ResourceAction;

public class RunTestCaseAction extends ResourceAction {

	final MutexRule rule = new MutexRule();

	public void run(IAction action) {
		final IResource[] resources = getSelectedResources();
		if (resources.length > 0) {
			Job job = new Job("PHPUnit") {
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Run Test", resources.length);

					PHPUnitValidator validator = new PHPUnitValidator();
					int worked = 0;
					for (IResource resource : resources) {
						monitor.subTask("Run " + resource.getProjectRelativePath().toString());

						if (resource instanceof IFolder) {
							validator.validateFolder((IFolder) resource);
						} else if (resource instanceof IFile) {
							IFile file = (IFile) resource;
							if (PHPUnit.isTestSuite(file)) {
								validator.validateTestSuite(file);
							} else {
								IFile testCaseFile = PHPUnit.searchTestCase(file);
								if (testCaseFile != null) {
									validator.validateTestCase(file);
								} else {
									Logger.logToConsole("Error: No test case / test suite found for file "
											+ file.getProjectRelativePath().toString());
								}
							}
						}

						monitor.worked(++worked);
					}

					return Status.OK_STATUS;
				}
			};
			job.setRule(rule);
			job.setUser(false);
			job.schedule();
		}
	}
}
