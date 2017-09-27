package org.ihtsdo.buildcloud.manifest;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXParseException;

public class ManifestValidator {
	
	public static String validate(InputStream manifestInputStream) {
		String failureMessage = null;
		try
	    {
	        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	        Schema schema = factory.newSchema(ManifestValidator.class.getClassLoader().getResource("manifest.xsd"));
	        Validator validator = schema.newValidator();
	        validator.validate(new StreamSource(manifestInputStream));
	    }
	    catch (Exception e) {
	    	failureMessage = e.getMessage();
	    	if (failureMessage == null && e.getCause() != null) {
	    		failureMessage = e.getCause().getMessage();
	    	}
	    	if (e instanceof SAXParseException) {
	    		StringBuilder msgBuilder = new StringBuilder();
	    		msgBuilder.append(failureMessage);
	    		msgBuilder.append(" The issue lies in the manifest.xml at line " + ((SAXParseException) e).getLineNumber());
	    		msgBuilder.append(" and column " + ((SAXParseException) e).getColumnNumber());
	    		failureMessage = msgBuilder.toString();
	    	}
	    }
		return failureMessage;
	}
}
