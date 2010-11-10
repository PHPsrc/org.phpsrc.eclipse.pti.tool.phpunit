package org.phpsrc.eclipse.pti.tools.phpunit.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.core.ModelException;
import org.phpsrc.eclipse.pti.core.PHPToolkitUtil;
import org.phpsrc.eclipse.pti.ui.Logger;

public class PHPUnitToolkitUtil {
	public static String getClassName(IFile file) {
		return getClassName(PHPToolkitUtil.getSourceModule(file));
	}

	public static String getClassName(ISourceModule module) {
		if (module != null) {
			try {
				IType[] types = module.getAllTypes();
				if (types != null && types.length > 0) {
					if (types.length == 1) {
						return types[0].getElementName();
					} else {
						// Namespaces have subtypes and classes not
						if (types[0].getTypes().length > 0) {
							return types[1].getElementName();
						} else {
							return types[0].getElementName();
						}
					}
				}
			} catch (ModelException e) {
				Logger.logException(e);
			}
		}

		return null;
	}

	public static String getNamespace(IFile file) {
		return getNamespace(PHPToolkitUtil.getSourceModule(file));
	}

	public static String getNamespace(ISourceModule module) {
		if (module != null) {
			try {
				IType[] types = module.getAllTypes();
				if (types != null && types.length > 0) {
					if (types.length > 1) {
						// Namespaces have subtypes and classes not
						if (types[0].getTypes().length > 0) {
							return types[0].getElementName();
						}
					}
				}
			} catch (ModelException e) {
				Logger.logException(e);
			}
		}

		return null;
	}

	public static String getClassNameWithNamespace(IFile file) {
		return getClassNameWithNamespace(PHPToolkitUtil.getSourceModule(file));
	}

	public static String getClassNameWithNamespace(ISourceModule module) {
		String className = getClassName(module);
		if (className != null) {
			String namespace = getNamespace(module);
			if (namespace != null) {
				return namespace + "\\" + className;
			} else {
				return className;
			}
		}

		return null;
	}
}
