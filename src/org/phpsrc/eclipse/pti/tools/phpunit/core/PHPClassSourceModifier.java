package org.phpsrc.eclipse.pti.tools.phpunit.core;

import java.io.InvalidClassException;
import java.util.ArrayList;

import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.IMethod;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.core.ModelException;

public class PHPClassSourceModifier {
	protected StringBuffer sourceStart = new StringBuffer();
	protected StringBuffer sourceEnd = new StringBuffer();

	protected ArrayList<String> fMethods = new ArrayList<String>();

	public PHPClassSourceModifier(ISourceModule module, String className) throws InvalidClassException, ModelException {
		Assert.isNotNull(module);
		Assert.isTrue(module.exists());
		Assert.isNotNull(className);

		boolean found = false;
		for (IType type : module.getAllTypes()) {
			if (type.getElementName().equals(className)) {
				for (IMethod method : type.getMethods()) {
					fMethods.add(method.getElementName());
				}
				parseSourceCode(module, type);
				found = true;
				break;
			}
		}

		if (!found)
			throw new InvalidClassException("Class " + className + " not found");
	}

	private void parseSourceCode(ISourceModule module, IType type) throws ModelException {
		String fileSource = module.getSource();

		ISourceRange range = type.getSourceRange();
		if (range.getOffset() > 0)
			sourceStart.append(fileSource.substring(0, range.getOffset()).trim() + "\n");

		String classSource = type.getSource();
		sourceStart.append(classSource.substring(0, classSource.lastIndexOf('}')));

		sourceEnd.append(classSource.substring(classSource.lastIndexOf('}')));

		int offsetEnd = range.getOffset() + range.getLength();
		if (offsetEnd + 1 < fileSource.length())
			sourceEnd.append(fileSource.substring(offsetEnd));
	}

	public boolean hasMethod(IMethod method) {
		Assert.isNotNull(method);
		return fMethods.contains(method.getElementName());
	}

	public void addMethod(IMethod method) throws ModelException {
		if (!hasMethod(method)) {
			fMethods.add(method.getElementName());
			sourceStart.append("\n    " + method.getSource() + "\n");
		}
	}

	public String getSource() throws ModelException {
		return sourceStart.toString() + sourceEnd;
	}
}
