/*******************************************************************************
 * Copyright (c) 2014 Liviu Ionescu.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Liviu Ionescu - initial version
 *******************************************************************************/

package org.eclipse.embedcdt.debug.gdbjtag.core.data;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.embedcdt.debug.core.data.ISVDPathManager;
import org.eclipse.embedcdt.debug.core.data.ISVDPathManagerFactory;
import org.eclipse.embedcdt.internal.debug.gdbjtag.core.Activator;

public class SVDPathManagerProxy implements ISVDPathManager {

	// ------------------------------------------------------------------------

	private static final String FACTORY_ELEMENT = "factory";
	private static final String CLASS_ATTRIBUTE = "class";

	// ------------------------------------------------------------------------

	private static SVDPathManagerProxy fgInstance;

	public static SVDPathManagerProxy getInstance() {

		if (fgInstance == null) {
			fgInstance = new SVDPathManagerProxy();
		}
		return fgInstance;
	}

	// ------------------------------------------------------------------------

	private ISVDPathManager fPathManagers[];

	public SVDPathManagerProxy() {

		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT_ID);
		if (extensionPoint == null) {
			Activator.log("Extension point " + EXTENSION_POINT_ID + " not found");
			return;
		}
		
		IExtension[] extensions = extensionPoint.getExtensions();
		if (extensions.length == 0) {
			Activator.log("Extension point " + EXTENSION_POINT_ID + " has no extensions");
			return;
		}

		fPathManagers = new ISVDPathManager[extensions.length];

		for (int i = 0; i < extensions.length; ++i) {

			fPathManagers[i] = null;

			IExtension extension = extensions[i];
			IConfigurationElement[] configElements = extension.getConfigurationElements();
			IConfigurationElement configElement = configElements[0];

			if (FACTORY_ELEMENT.equals(configElement.getName())) {

				ISVDPathManagerFactory factory;
				try {
					Object obj = configElement.createExecutableExtension(CLASS_ATTRIBUTE);

					if (obj instanceof ISVDPathManagerFactory) {
						factory = (ISVDPathManagerFactory) obj;

						// Create the extension point data manager.
						fPathManagers[i] = factory.create();
					} else {
						Activator.log("no ISVDPathManagerFactory");
					}
				} catch (CoreException e) {
					Activator.log("cannot get factory for " + EXTENSION_POINT_ID);
				}
			} else {
				Activator.log("no <factory> element");
			}

		}
	}

	// ------------------------------------------------------------------------

	@Override
	public IPath getSVDAbsolutePath(String deviceVendorId, String deviceName) {

		if (fPathManagers == null) {
			return null;
		}

		// Iterate all managers and return the first value available.
		for (int i = 0; i < fPathManagers.length; ++i) {
			IPath path = null;
			if (fPathManagers[i] != null) {
				path = fPathManagers[i].getSVDAbsolutePath(deviceVendorId, deviceName);
				if (path != null) {
					return path;
				}
			}
		}

		return null;
	}

	// ------------------------------------------------------------------------
}
