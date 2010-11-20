/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.ui.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.php.internal.ui.preferences.IStatusChangeListener;
import org.eclipse.php.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.phpsrc.eclipse.pti.core.launching.PHPToolLauncher;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnit;

@SuppressWarnings("restriction")
public class PHPUnitPreferencePage extends PropertyAndPreferencePage {

	public static final String PREF_ID = "org.phpsrc.eclipse.pti.tools.phpunit.ui.preferences.PreferencePage"; //$NON-NLS-1$
	public static final String PROP_ID = "org.phpsrc.eclipse.pti.tools.phpunit.ui.propertyPages.PreferencePage"; //$NON-NLS-1$

	private PHPUnitConfigurationBlock fConfigurationBlock;
	private IStatus origStatusOk;

	public PHPUnitPreferencePage() {
		setPreferenceStore(PHPUnitPlugin.getDefault().getPreferenceStore());
		origStatusOk = getPreferenceContentStatus();
	}

	/*
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */

	public void createControl(Composite parent) {
		IWorkbenchPreferenceContainer container = (IWorkbenchPreferenceContainer) getContainer();
		fConfigurationBlock = new PHPUnitConfigurationBlock(
				getNewStatusChangedListener(), getProject(), container);

		super.createControl(parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#
	 * createPreferenceContent(org.eclipse.swt.widgets.Composite)
	 */

	protected Control createPreferenceContent(Composite composite) {
		return fConfigurationBlock.createContents(composite);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#
	 * hasProjectSpecificOptions(org.eclipse.core.resources.IProject)
	 */

	protected boolean hasProjectSpecificOptions(IProject project) {
		return fConfigurationBlock.hasProjectSpecificOptions(project);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#
	 * getPreferencePageID()
	 */

	protected String getPreferencePageID() {
		return PREF_ID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#
	 * getPropertyPageID()
	 */

	protected String getPropertyPageID() {
		return PROP_ID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#
	 * enableProjectSpecificSettings(boolean)
	 */

	protected void enableProjectSpecificSettings(
			boolean useProjectSpecificSettings) {
		// Order is important!
		super.enableProjectSpecificSettings(useProjectSpecificSettings);
		if (fConfigurationBlock != null) {
			fConfigurationBlock
					.useProjectSpecificSettings(useProjectSpecificSettings);
		}
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performDefaults()
	 */

	protected void performDefaults() {
		super.performDefaults();
		if (fConfigurationBlock != null) {
			fConfigurationBlock.performDefaults();
		}
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */

	public boolean performOk() {
		if (fConfigurationBlock != null && !fConfigurationBlock.performOk()) {
			return false;
		}

		PHPToolLauncher.deleteAllConfigs(PHPUnit.getScriptFile().toOSString());

		return super.performOk();
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performApply()
	 */
	public void performApply() {
		if (fConfigurationBlock != null) {
			fConfigurationBlock.performApply();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */

	public void dispose() {
		if (fConfigurationBlock != null) {
			fConfigurationBlock.dispose();
		}
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#setElement
	 * (org.eclipse.core.runtime.IAdaptable)
	 */

	public void setElement(IAdaptable element) {
		super.setElement(element);
		setDescription(null); // no description for property page
	}

	protected IStatusChangeListener getNewStatusChangedListener() {
		return new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				if (status == null || status.isOK())
					setPreferenceContentStatus(origStatusOk);
				else
					setPreferenceContentStatus(status);
			}
		};
	}
}
