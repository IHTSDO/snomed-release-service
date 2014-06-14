package org.ihtsdo.buildcloud.service.execution;

import org.apache.commons.io.FilenameUtils;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Properties;

/**
 * BuildXMLParser
 * <p/>
 * Generates the directory structure and copies files into the correct directories
 * as defined in an input XML file.
 */
public class PackageBuilder {

	private static final String propsFile = "/packager.properties";
	private static final String DEFAULT_XML_FILE_LOCATION = "xml/IntlRelease.xml";
	private static final String DEFAULT_TARGET = "target";
	private static final String DEFAULT_SOURCE = "source";
	private static final Logger LOGGER = LoggerFactory.getLogger(PackageBuilder.class);

	private final String sourcePath;
	private final String targetPath;
	private final String manifestPath;
	private final String zipName;
	
	private int filesInManifest = 0;
	private int filesAssembled = 0;
	private int directoriesInManifest = 0;
	private int directoriesAssembled = 0;
	
	private boolean warnOnMissingFiles = true;
	private boolean createZip = false;

	public static void main(String args[]) {
		try {
			PackageBuilder buildXMLParser = new PackageBuilder();
			buildXMLParser.process();
		} catch (NoSuchFileException e) {
			LOGGER.error("No such file: {}", e.getLocalizedMessage());
		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}
	}
	
	public void warnOnMissingFiles(boolean doWarnings) {
		this.warnOnMissingFiles = doWarnings;
	}

	private PackageBuilder() throws IOException {
		LOGGER.debug("Reading properties file {}", propsFile);
		Properties props = new Properties();
		props.load(getClass().getResourceAsStream(propsFile));
		sourcePath = props.getProperty("packaging.source", DEFAULT_SOURCE);
		targetPath = props.getProperty("packaging.target", DEFAULT_TARGET);
		manifestPath = props.getProperty("packaging.xmlFile", DEFAULT_XML_FILE_LOCATION);
		zipName = null;
	}
	
	public PackageBuilder (String sourceDir, String targetDir, String zipName, String manifestPath) {
		this.sourcePath = sourceDir;
		this.targetPath = targetDir;
		this.manifestPath = manifestPath;
		this.zipName = zipName;
		createZip = true;
	}

	/**
	 * 
	 * @return A File object representing a zip file created in the parent directory of the target path
	 * @throws Exception
	 */
	public File process() throws Exception {
		File result = null;
		LOGGER.debug("Reading manifest {}", manifestPath);
		Document dom = parseXmlFile(manifestPath);
		parseDocument(dom);

		LOGGER.debug("Manifest directories {}, files {}.  Assembled directories {}, files {}", directoriesInManifest, filesInManifest, directoriesAssembled, filesAssembled);
		
		if (createZip) {
			String zipLocation = FilenameUtils.getFullPathNoEndSeparator(this.targetPath) + File.separator + this.zipName;
			result = FileUtils.zipDir(zipLocation, this.targetPath);
			LOGGER.info("Package Builder Processing complete.  Created zip file: {}", result.getAbsolutePath());
		}
		return result;
	}

	private Document parseXmlFile(String path) throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		return documentBuilder.parse(path);
	}

	private void parseDocument(Document dom) throws Exception {
		Node node = dom.getFirstChild();
		processNode(node, targetPath);
	}

	private void processNode(Node node, String path) throws Exception {

		NodeList childNodeList = node.getChildNodes();
		if (childNodeList != null) {
			for (int i = 0; i < childNodeList.getLength(); i++) {
				Node n = childNodeList.item(i);
				try {
					if (n.getNodeType() == Node.ELEMENT_NODE) {
						if (n.getNodeName().equals("folder")) {
							directoriesInManifest++;
							String folderName = getAttribute(n, "Name", "unknown");
							LOGGER.debug("Folder node: {}", folderName);
							// create directory
							String newPath = path + "/" + folderName;
							createFolder(newPath);
							// make recursive call
							processNode(n, newPath);
						} else if (n.getNodeName().equals("file")) {
							filesInManifest++;
							String fileName = getAttribute(n, "Name", "unknown");
							LOGGER.debug("File node: {}", fileName);
							String src = sourcePath + "/" + fileName;
							String dst = path + "/" + fileName;
							copy(src, dst);
						}
					}
				} catch (FileNotFoundException fnf) {
					//TODO Telemetry will know the process name and current stage, just need info here.
					if (warnOnMissingFiles) {
					 LOGGER.warn ("File not Found during package building at manifest element [{}: {} {}] {}",i, n.getNodeName(), getAttribute(n, "Name", "unknown"), fnf.getLocalizedMessage() );
					}
			
				} catch (Exception e) {
					LOGGER.warn ("Unexpected problem during package building at manifest element [{}: {} {}] {}",i, n.getNodeName(), getAttribute(n, "Name", "unknown"), e.getLocalizedMessage() );
				}
			}
		} else {
			LOGGER.error("Child is not present");
		}
	}

	private String getAttribute(Node node, String name, String defaultValue) {
		Node n = node.getAttributes().getNamedItem(name);
		return (n != null) ? (n.getNodeValue()) : (defaultValue);
	}

	private void createFolder(String targetPath) {
		// if ParentDir may not exist yet, you can use
		// mkDirs() instead and all directories will be created
		File targetDir = new File(targetPath);
		if (!targetDir.exists()) {
			if (targetDir.mkdirs()) {
				LOGGER.debug("Folder created: {}", targetDir);
				directoriesAssembled++;
			} else {
				LOGGER.error("Failed to create Folder: {}", targetDir);
			}
		}
	}

	private void copy(String source, String destination) throws IOException {
		File destFile = new File(destination);
		File sourceFile = new File(source);
		
		if (!sourceFile.exists()) {
			throw new FileNotFoundException ("Failed to find expected file: " + source);
		}
		
		if (!destFile.isFile()) {
			Files.copy(sourceFile.toPath(), destFile.toPath());
			LOGGER.debug("File copied to: {}", destination);
			filesAssembled++;
		} else {
			LOGGER.info("File already exists: {}", destination);
		}
	}

}
						