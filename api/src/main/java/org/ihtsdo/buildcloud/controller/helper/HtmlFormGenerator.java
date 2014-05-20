package org.ihtsdo.buildcloud.controller.helper;

import java.io.File;
import java.text.ParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class HtmlFormGenerator {
	
	private enum Status {UNCREATED, CREATED, INITIALISED, IN_ERROR}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlFormGenerator.class);
	
	private File xsdFile;
	private Document doc;
	private Status status = Status.UNCREATED;

	public HtmlFormGenerator (File xsdFile) {
		this.xsdFile = xsdFile;
		this.status = Status.CREATED;
	}
	
	public void init() {
		try {
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
			this.doc = documentBuilder.parse(xsdFile);
			doc.getDocumentElement().normalize();
			this.status = Status.INITIALISED;
			
		} catch (Exception e) {
			LOGGER.warn ("Failed to read XSD", e);
		}
	}
	
	public String generateHTML() throws ParseException{
		String result = "";
		
		//Do we need to intialise?
		switch (this.status) {
			case UNCREATED:
			case CREATED : 	init();
							break;
			default:
		}

		//find that top element - will always be "schema" for an xsd
		NodeList nodeList = doc.getElementsByTagName("xs:schema");
		
		//We must have exactly 1 schema tag, and it must be the root element
		if (nodeList.getLength() != 1 || !nodeList.item(0).isSameNode(doc.getDocumentElement())) {
			this.status = Status.IN_ERROR;
			throw new ParseException ("Failed to find valid schema element in " + xsdFile.getPath() + "" + xsdFile.getName(),0);
		}
	
		Node schema = doc.getDocumentElement();
	
		//TODO Recursively walk through the DOM and build up a structure of repeating blocks, complex types and elements.
		//Then use that structure to generate HTML with javascript to dynamically add repeating blocks as requried.

		return result;
	}
}