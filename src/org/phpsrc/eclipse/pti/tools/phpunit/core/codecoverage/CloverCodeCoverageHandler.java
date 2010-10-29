package org.phpsrc.eclipse.pti.tools.phpunit.core.codecoverage;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.phpsrc.eclipse.pti.core.php.source.PHPSourceFile;
import org.phpsrc.eclipse.pti.tools.phpunit.PHPUnitPlugin;
import org.phpsrc.eclipse.pti.ui.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CloverCodeCoverageHandler extends DefaultHandler {
	public static final String NODE_FILE = "file"; //$NON-NLS-1$
	public static final String NODE_LINE = "line"; //$NON-NLS-1$

	public static final String ATTR_NAME = "name"; //$NON-NLS-1$
	public static final String ATTR_NUM = "num"; //$NON-NLS-1$
	public static final String ATTR_COUNT = "count"; //$NON-NLS-1$

	protected PHPSourceFile coverageFile;

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (NODE_FILE.equals(qName)) {
			IFile file = PHPUnitPlugin.resolveProjectFile(attributes
					.getValue(ATTR_NAME));
			System.out.println(file);
			try {
				coverageFile = file != null ? new PHPSourceFile(file) : null;
			} catch (CoreException e) {
				Logger.logException(e);
				coverageFile = null;
			} catch (IOException e) {
				Logger.logException(e);
				coverageFile = null;
			}
			System.out.println(coverageFile);
		} else if (NODE_LINE.equals(qName) && coverageFile != null) {
			try {
				int count = Integer.parseInt(attributes.getValue(ATTR_COUNT));
				System.out.println("Count: " + count);
				if (count == 0) {
					int line = Integer.parseInt(attributes.getValue(ATTR_NUM));
					System.out.println("Line: " + line);
					if (line > 0) {
						System.out.println("new marker");
						IMarker marker = coverageFile
								.getFile()
								.createMarker(
										"org.phpsrc.eclipse.pti.tools.phpunit.validator.phpToolPHPUnitCodeCoverageMarker");

						marker.setAttribute(IMarker.MESSAGE,
								"No code coverage for line " + line);

						marker.setAttribute(IMarker.PROBLEM, false);
						marker.setAttribute(IMarker.LINE_NUMBER, line);
						marker.setAttribute(IMarker.SEVERITY,
								IMarker.SEVERITY_INFO);
						marker.setAttribute(IMarker.CHAR_START,
								coverageFile.lineStart(line));
						marker.setAttribute(IMarker.CHAR_END,
								coverageFile.lineEnd(line));
					}
				}
			} catch (CoreException e) {
				Logger.logException(e);
			}
		}
	}

	public static void importXml(File file) {
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			parser.parse(file, new CloverCodeCoverageHandler());
		} catch (ParserConfigurationException e) {
			Logger.logException(e);
		} catch (SAXException e) {
			Logger.logException(e);
		} catch (IOException e) {
			Logger.logException(e);
		}
	}
}
