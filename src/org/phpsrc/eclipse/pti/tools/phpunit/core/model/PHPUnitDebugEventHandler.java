/*******************************************************************************
 * Copyright (c) 2009, 2010 Sven Kiera
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.phpsrc.eclipse.pti.tools.phpunit.core.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.phpsrc.eclipse.pti.core.launching.IPHPToolLaunchConstants;
import org.phpsrc.eclipse.pti.tools.phpunit.core.PHPUnit;

public class PHPUnitDebugEventHandler {

	private final static PHPUnitDebugEventHandler fDefault = new PHPUnitDebugEventHandler();

	private final class PHPUnitDebugEventSetListener implements IDebugEventSetListener {
		public void handleDebugEvents(DebugEvent[] events) {
			String qualifiedName = PHPUnit.QUALIFIED_NAME.toString();

			for (DebugEvent event : events) {
				Object source = event.getSource();
				if (source instanceof IProcess) {
					IProcess process = (IProcess) source;
					ILaunch launch = process.getLaunch();
					if (launch != null) {
						ILaunchConfiguration config = launch.getLaunchConfiguration();
						if (config != null) {
							String name = null;
							try {
								name = config.getAttribute(IPHPToolLaunchConstants.ATTR_PHP_TOOL_QUALIFIED_NAME,
										(String) null);
							} catch (CoreException e) {
							}

							if (name != null && name.equals(qualifiedName)) {
								switch (event.getKind()) {
								case DebugEvent.CREATE:
									process.getStreamsProxy().getOutputStreamMonitor().addListener(fStreamListener);
									notifyListenerStartProcess();
									break;
								case DebugEvent.TERMINATE:
									process.getStreamsProxy().getOutputStreamMonitor().removeListener(fStreamListener);
									notifyListenerStopProcess();
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	private final IStreamListener fStreamListener = new IStreamListener() {
		public void streamAppended(String text, IStreamMonitor monitor) {
			notifyListenerAppendOutput(text);
		}
	};

	private final IDebugEventSetListener fDebugEventListener = new PHPUnitDebugEventSetListener();
	private final ListenerList fTestProcessListeners = new ListenerList();

	private PHPUnitDebugEventHandler() {

	}

	public static PHPUnitDebugEventHandler getDefault() {
		return fDefault;
	}

	public void start() {
		DebugPlugin.getDefault().addDebugEventListener(fDebugEventListener);
	}

	public void stop() {
		DebugPlugin.getDefault().removeDebugEventListener(fDebugEventListener);
	}

	public void addListener(ITestDebugProcessListener listener) {
		fTestProcessListeners.add(listener);
	}

	public void removeListener(ITestDebugProcessListener listener) {
		fTestProcessListeners.remove(listener);
	}

	private void notifyListenerStartProcess() {
		Object[] listeners = fTestProcessListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((ITestDebugProcessListener) listeners[i]).startProcess();
		}
	}

	private void notifyListenerStopProcess() {
		Object[] listeners = fTestProcessListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((ITestDebugProcessListener) listeners[i]).stopProcess();
		}
	}

	private void notifyListenerAppendOutput(String text) {
		Object[] listeners = fTestProcessListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((ITestDebugProcessListener) listeners[i]).appendOutput(text);
		}
	}
}
