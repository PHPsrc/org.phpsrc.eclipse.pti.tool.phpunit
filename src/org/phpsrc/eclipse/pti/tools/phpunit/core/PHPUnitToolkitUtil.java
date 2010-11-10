package org.phpsrc.eclipse.pti.tools.phpunit.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.search.SearchMatch;
import org.phpsrc.eclipse.pti.core.PHPToolkitUtil;
import org.phpsrc.eclipse.pti.core.search.PHPSearchEngine;
import org.phpsrc.eclipse.pti.ui.Logger;

public class PHPUnitToolkitUtil {
	public static String getClassName(IFile file) {
		return getClassName(PHPToolkitUtil.getSourceModule(file));
	}

	public static String getClassName(ISourceModule module) {
		if (module != null) {
			try {
				IType[] types = module.getAllTypes();
				return getClassType(types).getElementName();
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

	private static IType getClassType(IType[] types) {
		if (types != null && types.length > 0) {
			try {
				if (types.length == 1) {
					return types[0];
				} else {
					// Namespaces have subtypes and classes not
					if (types[0].getTypes().length > 0) {
						return types[1];
					} else {
						return types[0];
					}
				}
			} catch (ModelException e) {
				Logger.logException(e);
			}
		}

		return null;
	}

	public static boolean hasSuperClass(IResource resource, String className) {
		ISourceModule module = PHPToolkitUtil.getSourceModule(resource);
		if (module != null)
			return hasSuperClass(module, className);

		return false;
	}

	public static boolean hasSuperClass(ISourceModule module, String className) {
		Assert.isNotNull(module);
		Assert.isNotNull(className);
		try {
			IType[] types = module.getAllTypes();
			if (types.length > 0) {
				String[] classes = getClassType(types).getSuperClasses();
				for (String c : classes) {
					if (c.indexOf('\\') >= 0)
						c = c.substring(c.lastIndexOf('\\') + 1);

					if (c.equals(className)) {
						return true;
					} else {
						SearchMatch[] matches = PHPSearchEngine.findClass(c,
								PHPSearchEngine.createProjectScope(module
										.getScriptProject().getProject()));
						for (SearchMatch match : matches) {
							if (hasSuperClass(match.getResource(), className))
								return true;
						}
					}
				}
			}
		} catch (ModelException e) {
			Logger.logException(e);
		}

		return false;
	}
}
