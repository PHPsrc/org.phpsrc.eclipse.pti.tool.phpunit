package org.phpsrc.eclipse.pti.tools.phpunit.core;

import java.io.File;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.search.SearchMatch;
import org.eclipse.dltk.internal.core.SourceType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.php.internal.ui.PHPUiConstants;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.FileEditorInput;
import org.phpsrc.eclipse.pti.core.PHPToolCorePlugin;
import org.phpsrc.eclipse.pti.core.PHPToolkitUtil;
import org.phpsrc.eclipse.pti.core.search.PHPSearchEngine;
import org.phpsrc.eclipse.pti.tools.phpunit.IPHPUnitConstants;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;
import org.phpsrc.eclipse.pti.tools.phpunit.core.preferences.PHPUnitPreferences;
import org.phpsrc.eclipse.pti.tools.phpunit.core.preferences.PHPUnitPreferencesFactory;
import org.phpsrc.eclipse.pti.tools.phpunit.ui.preferences.PHPUnitConfigurationBlock;
import org.phpsrc.eclipse.pti.ui.Logger;

@SuppressWarnings("restriction")
public class PHPUnitUtil {
	public static String generateTestCaseClassName(String sourceClass) {
		return sourceClass + "Test";
	}

	public static String generatePHPClassName(String testClass) {
		if (testClass.endsWith("Test"))
			testClass = testClass.substring(0, testClass.length() - 4);
		return testClass;
	}

	public static File generateProjectRelativeTestCaseFile(IFile classFile) {
		SearchMatch[] matches = PHPSearchEngine.findClass(
				PHPToolkitUtil.getClassName(classFile),
				PHPSearchEngine.createProjectScope(classFile.getProject()));
		if (matches.length > 0) {
			for (SearchMatch match : matches) {
				if (classFile == null || match.getResource().equals(classFile)) {
					return generateProjectRelativeTestCaseFile(
							(SourceType) match.getElement(), classFile);
				}
			}
		}
		return null;
	}

	public static File generateProjectRelativeTestCaseFile(SourceType type,
			IResource resource) {
		Assert.isNotNull(type);
		Assert.isNotNull(resource);

		String patternFolder = null;
		String patternFile = null;

		PHPUnitPreferences preferences = PHPUnitPreferencesFactory
				.factory(resource);
		if (preferences != null) {
			patternFolder = preferences.getTestFilePatternFolder();
			patternFile = preferences.getTestFilePatternFile();
		}

		if (patternFolder == null)
			patternFolder = PHPUnitConfigurationBlock.TEST_FILE_PATTERN_FOLDER_DEFAULT;

		String patternProject = "";
		String patternPath = "";

		String path = type.getPath().toOSString();

		int firstSeparator = path.indexOf(File.separatorChar, 1);
		if (firstSeparator > 0) {
			patternProject = path.substring(1, firstSeparator);
			int lastSeparator = path.lastIndexOf(File.separatorChar);
			if (firstSeparator + 1 < lastSeparator)
				patternPath = path.substring(firstSeparator + 1, lastSeparator);
		} else {
			patternProject = path;
		}

		Pattern pDirPlaceholder = Pattern.compile(Pattern
				.quote(IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_DIR)
				+ "(\\{([0-9]*)(,)?([0-9]*)\\})?");

		String[] folderParts = patternPath.split(Pattern.quote(""
				+ File.separatorChar));

		Matcher mDirPlaceholder = pDirPlaceholder.matcher(patternFolder);
		while (mDirPlaceholder.find()) {
			if (mDirPlaceholder.group(1) != null
					&& !"".equals(mDirPlaceholder.group(1))) {
				int start = 1;
				int end = 0;
				try {
					start = Integer.parseInt(mDirPlaceholder.group(2));
				} catch (Exception e) {
				}
				try {
					end = Integer.parseInt(mDirPlaceholder.group(4));
				} catch (Exception e) {
				}

				if (end == 0) {
					if (",".equals(mDirPlaceholder.group(3))) {
						end = folderParts.length;
					} else {
						end = start;
					}
				} else if (end > folderParts.length) {
					end = folderParts.length;
				}

				String folderSubstring = "";
				for (int i = start; i <= end; ++i) {
					if (i > folderParts.length)
						break;

					if (folderSubstring.length() > 0) {
						folderSubstring += File.separatorChar;
					}
					folderSubstring += folderParts[i - 1];
				}

				patternFolder = patternFolder.replace(mDirPlaceholder.group(),
						folderSubstring);
			} else {
				patternFolder = patternFolder
						.replaceFirst(
								Pattern.quote(IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_DIR),
								patternPath.replace("\\", "\\\\"));
			}
		}

		patternFolder = patternFolder.replace(
				IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_PROJECT,
				patternProject);

		if (patternFile == null)
			patternFile = PHPUnitConfigurationBlock.TEST_FILE_PATTERN_FILE_DEFAULT;

		String fileName = resource.getName();
		int firstDotPos = fileName.indexOf(".");
		int lastDotPos = fileName.lastIndexOf(".");
		patternFile = patternFile.replace(
				IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILENAME_LONG,
				fileName.substring(0, lastDotPos));
		patternFile = patternFile.replace(
				IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILENAME,
				fileName.substring(0, firstDotPos));
		patternFile = patternFile.replace(
				IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILE_EXTENSION,
				fileName.substring(lastDotPos + 1));

		return new File(patternFolder + File.separatorChar + patternFile);
	}

	public static boolean syncPHPClassToTestCase(String sourceClassName,
			IFile sourceClassFile, String testClassName, String testClassFile,
			String testSuperClass) {
		try {
			PHPUnit phpunit = PHPUnit.getInstance();
			phpunit.createTestSkeleton(sourceClassName, sourceClassFile,
					testClassName, testClassFile, testSuperClass);

			Path path = new Path(testClassFile);
			IFile testFile = ResourcesPlugin.getWorkspace().getRoot()
					.getFile(path);
			IEditorInput editorInput = new FileEditorInput(testFile);
			IWorkbenchWindow window = PHPUnitPlugin.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			page.openEditor(editorInput, PHPUiConstants.PHP_EDITOR_ID);

			return true;
		} catch (PHPUnitException e) {
			MessageDialog.openError(
					PHPToolCorePlugin.getActiveWorkbenchShell(),
					"Failed creating PHPUnit Test Case", e.getMessage());
			Logger.logException(e);
		} catch (InvalidObjectException e) {
			MessageDialog.openError(
					PHPToolCorePlugin.getActiveWorkbenchShell(),
					"Failed creating PHPUnit Test Case", e.getMessage());
			Logger.logException(e);
		} catch (InvalidClassException e) {
			MessageDialog.openError(
					PHPToolCorePlugin.getActiveWorkbenchShell(),
					"Failed creating PHPUnit Test Case", e.getMessage());
			Logger.logException(e);
		} catch (CoreException e) {
			MessageDialog.openError(
					PHPToolCorePlugin.getActiveWorkbenchShell(),
					"Failed creating PHPUnit Test Case", e.getMessage());
			Logger.logException(e);
		}

		return false;
	}

	public static File generateProjectRelativePHPClassFile(IFile classFile) {
		SearchMatch[] matches = PHPSearchEngine.findClass(
				PHPToolkitUtil.getClassName(classFile),
				PHPSearchEngine.createProjectScope(classFile.getProject()));
		if (matches.length > 0) {
			for (SearchMatch match : matches) {
				if (classFile == null || match.getResource().equals(classFile)) {
					return generateProjectRelativePHPClassFile(
							(SourceType) match.getElement(), classFile);
				}
			}
		}
		return null;
	}

	public static File generateProjectRelativePHPClassFile(SourceType type,
			IResource resource) {
		Assert.isNotNull(type);
		Assert.isNotNull(resource);

		String patternTestFolder = null;
		String patternSourceFolder = null;
		String patternFile = null;

		PHPUnitPreferences preferences = PHPUnitPreferencesFactory
				.factory(resource);
		if (preferences != null) {
			patternSourceFolder = preferences.getSourceFilePatternFolder();
			if ("".equals(patternSourceFolder))
				patternSourceFolder = null;
			patternFile = preferences.getTestFilePatternFile();
			patternTestFolder = preferences.getTestFilePatternFolder();
		}

		if (patternTestFolder == null)
			patternTestFolder = PHPUnitConfigurationBlock.TEST_FILE_PATTERN_FOLDER_DEFAULT;

		String patternProject = "";
		String patternPath = "";

		String path = type.getPath().toOSString();
		int firstSeparator = path.indexOf(File.separatorChar, 1);
		if (firstSeparator > 0) {
			patternProject = path.substring(1, firstSeparator);
			int lastSeparator = path.lastIndexOf(File.separatorChar);
			if (firstSeparator + 1 < lastSeparator)
				patternPath = path.substring(firstSeparator + 1, lastSeparator);
		} else {
			patternProject = path;
		}

		String filePath;

		if (patternSourceFolder != null) {

			Pattern pDirPlaceholder = Pattern.compile(Pattern
					.quote(IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_DIR)
					+ "(\\{([0-9]*)(,)?([0-9]*)\\})?");

			String[] folderParts = patternPath.split(Pattern.quote(""
					+ File.separatorChar));

			Matcher mDirPlaceholder = pDirPlaceholder
					.matcher(patternSourceFolder);
			while (mDirPlaceholder.find()) {
				if (mDirPlaceholder.group(1) != null
						&& !"".equals(mDirPlaceholder.group(1))) {
					int start = 1;
					int end = 0;
					try {
						start = Integer.parseInt(mDirPlaceholder.group(2));
					} catch (Exception e) {
					}
					try {
						end = Integer.parseInt(mDirPlaceholder.group(4));
					} catch (Exception e) {
					}

					if (end == 0) {
						if (",".equals(mDirPlaceholder.group(3))) {
							end = folderParts.length;
						} else {
							end = start;
						}
					} else if (end > folderParts.length) {
						end = folderParts.length;
					}

					String folderSubstring = "";
					for (int i = start; i <= end; ++i) {
						if (i > folderParts.length)
							break;

						if (folderSubstring.length() > 0) {
							folderSubstring += File.separatorChar;
						}
						folderSubstring += folderParts[i - 1];
					}

					patternSourceFolder = patternSourceFolder.replace(
							mDirPlaceholder.group(), folderSubstring);
				} else {
					patternSourceFolder = patternSourceFolder
							.replaceFirst(
									Pattern.quote(IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_DIR),
									patternPath.replace("\\", "\\\\"));
				}
			}

			patternSourceFolder = patternSourceFolder.replace(
					IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_PROJECT,
					patternProject);

			filePath = patternSourceFolder;
		} else {
			String targetFolder = File.separatorChar + patternProject
					+ File.separatorChar + patternPath;

			if (patternTestFolder.charAt(patternTestFolder.length() - 1) == File.separatorChar)
				patternTestFolder = patternTestFolder.substring(0,
						patternTestFolder.length() - 1);

			patternTestFolder = patternTestFolder.replace(
					IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_PROJECT,
					"([^" + File.separatorChar + "]+)"); //$NON-NLS-1$  //$NON-NLS-2$
			patternTestFolder = patternTestFolder
					.replace(
							IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_DIR,
							"(.+)"); //$NON-NLS-1$
			patternTestFolder = patternTestFolder.replace("\\", "\\\\"); //$NON-NLS-1$  //$NON-NLS-2$

			Pattern p = Pattern.compile(patternTestFolder);
			Matcher m = p.matcher(targetFolder);
			if (m.matches()) {
				targetFolder = File.separatorChar + m.group(1)
						+ File.separatorChar + m.group(2);
			}

			filePath = targetFolder;
		}

		if (patternFile == null)
			patternFile = PHPUnitConfigurationBlock.TEST_FILE_PATTERN_FILE_DEFAULT;

		String fileName = resource.getName();

		patternFile = patternFile.replace(".", "\\."); //$NON-NLS-1$ //$NON-NLS-2$
		patternFile = patternFile.replace(
				IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILENAME_LONG,
				"(.+)"); //$NON-NLS-1$
		patternFile = patternFile.replace(
				IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILENAME,
				"(.+)"); //$NON-NLS-1$
		patternFile = patternFile.replace(
				IPHPUnitConstants.TEST_FILE_PATTERN_PLACEHOLDER_FILE_EXTENSION,
				"([^.]+)"); //$NON-NLS-1$

		Matcher m = Pattern.compile(patternFile).matcher(fileName);
		if (m.matches()) {
			fileName = m.group(1) + "." + m.group(2); //$NON-NLS-1$
		}

		return new File(filePath + File.separatorChar + fileName);
	}

	public static boolean syncTestCaseToPHPClass(String testClassName,
			IFile testClassFile, String phpClassName, String phpClassFile,
			String phpSuperClass) {
		try {
			PHPUnit phpunit = PHPUnit.getInstance();
			phpunit.createPHPClassSkeleton(testClassName, testClassFile,
					phpClassName, phpClassFile, phpSuperClass);

			Path path = new Path(phpClassFile);
			IFile testFile = ResourcesPlugin.getWorkspace().getRoot()
					.getFile(path);
			IEditorInput editorInput = new FileEditorInput(testFile);
			IWorkbenchWindow window = PHPUnitPlugin.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			page.openEditor(editorInput, PHPUiConstants.PHP_EDITOR_ID);

			return true;
		} catch (PHPUnitException e) {
			MessageDialog.openError(
					PHPToolCorePlugin.getActiveWorkbenchShell(),
					"Failed creating PHP Class", e.getMessage());
			Logger.logException(e);
		} catch (InvalidObjectException e) {
			MessageDialog.openError(
					PHPToolCorePlugin.getActiveWorkbenchShell(),
					"Failed creating PHP Class", e.getMessage());
			Logger.logException(e);
		} catch (InvalidClassException e) {
			MessageDialog.openError(
					PHPToolCorePlugin.getActiveWorkbenchShell(),
					"Failed creating PHP Class", e.getMessage());
			Logger.logException(e);
		} catch (CoreException e) {
			MessageDialog.openError(
					PHPToolCorePlugin.getActiveWorkbenchShell(),
					"Failed creating PHP Class", e.getMessage());
			Logger.logException(e);
		}

		return false;
	}
}