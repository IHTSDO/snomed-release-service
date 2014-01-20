package org.ihtsdo.releaseprocess;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.json.JSONObject;

/**
 * Test program for generating JSON based on the test conditions configuration
 * that lives in various directories within the Release Assertion Toolkit
 * 
 * @author Peter G. Williams, IHTSDO
 * @version		 %I%, %G%
 * @since			 1.0
 */
public class QAStructure {

	//specifying file path with hashes, which we will replace with Unix or Windows slash as runtime
	private static String[] s_directories = {"#release-file-qa#src#main#resources#metadata",
											 "#release-assertion-resources#src#main#resources#scripts"};


	// create an XPathFactory
	private static XPathFactory s_xFactory = XPathFactory.newInstance();	


	public static void main(String args[]) throws Exception {

		if (args.length < 1) {
			out ("Usage:	java org.ihtsdo.releaseprocess.QAStructure <Release Assertion Root Directory>");
			System.exit(-1);
		}
		
		String rootDir = args[0];
		debug ("Looking from directory root: " + rootDir);

		
		ArrayList <Map<String, Object>> QATargets = new ArrayList<Map<String, Object>>();
		QAStructure examiner = new QAStructure();
		for(String thisDir: s_directories){
			examiner.examineDirectory (rootDir + thisDir, QATargets);
		}
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("targetList", QATargets);
		System.out.println (jsonObj.toString());		

	}
	
	private void examineDirectory (String targetDir, ArrayList<Map<String, Object>> targetList) throws Exception{
		targetDir = targetDir.replace('#', File.separator.charAt(0) );

		File thisDir = new File (targetDir);
		if (!thisDir.isDirectory()) {
			debug (thisDir + " is not a valid Directory.");
			return;
		} else if (thisDir.getName().charAt(0) == '.') {
			debug ("Ignoring system directory " + thisDir.getName());
			return;
		}
		
		debug ("Examinining directory: " + thisDir.getName());
		File[] files = thisDir.listFiles();

		for (File thisFile : files){
			if(thisFile.isFile()) {
				examineFile(thisFile, targetList);
			} else if (thisFile.isDirectory()) {
				debug("[DIR] " + thisFile.getName());
				examineDirectory(thisFile.getPath(), targetList);
			} else {
				error(" [ACCESS DENIED] - " + thisFile.getPath());
			}
		}


	}
	
	//Determine what sort of file this is
	private void examineFile (File thisFile,  ArrayList<Map<String, Object>> targetList) throws Exception{
		
		String fileName = thisFile.getName();
		Map <String,Object> thisTarget = new HashMap<String,Object>();
		thisTarget.put("targetName", fileName);
		targetList.add(thisTarget);
		
		ArrayList <Map<String, Object>> aspectList = new ArrayList<Map<String, Object>>();		
		thisTarget.put("aspectList", aspectList);

		debug("[FILE] " + fileName);
		//What sort of file are we looking at?
		String ext = "";
		int idx = fileName.lastIndexOf('.');
		if (idx > 0) {
			ext = fileName.substring(idx+1);
		}
		switch (ext) {
			case("xml"):	examineXMLFile(thisFile, aspectList);
							break;
			case("sql"):	examineSQLFile(thisFile, aspectList);
							break;
			default:		debug ("Ignoring unknown file type in " + fileName);
		}		
	}
	
	private void examineXMLFile (File thisFile,ArrayList <Map<String, Object>> aspectList) throws Exception{
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder;
			Document doc = null;
			builder = factory.newDocumentBuilder();
			doc = builder.parse(thisFile);
			//Watch that files that start with Metadata have the column elements within the file (correct!)
			//But RF2 files have the file and column tests as sibling elements.
			
			//When file is root element
			examinePath (doc, "/file", aspectList);
			
			//When file is under manifest
			examinePath (doc, "/metadata/file", aspectList);
			
			//Now look at the columns, anywhere in DOM
			examinePath (doc, "//column", aspectList);

			
		} catch (Exception e) {
			error ("Failed to parse " + thisFile.getPath() + " due to " + e.getLocalizedMessage());
		}
	}
	
	private void examinePath (Document doc, String xPathStr, ArrayList <Map<String, Object>> aspectList) throws Exception {

		// create an XPath object
		XPath xpath = s_xFactory.newXPath();

		// compile the XPath expression
		XPathExpression expr = xpath.compile(xPathStr);
		
		// run the query and get a nodeset
		Object result = expr.evaluate(doc, XPathConstants.NODESET);

		// cast the result to a DOM NodeList
		NodeList nodes = (NodeList) result;
		for (int i=0; i<nodes.getLength();i++){
			examineNode(nodes.item(i),aspectList);
		}
	}
	
	private void examineNode (Node thisNode, ArrayList <Map<String, Object>> aspectList) throws Exception {
		//A file has a description, and a column has a header and a position.  Those are our aspects
		String aspectName = thisNode.getNodeName() + ": ";
		
		XPath xpath = s_xFactory.newXPath();		
		String xPathStr = "(description|header|position)";
		XPathExpression expr = xpath.compile(xPathStr);
		NodeList nodeList = (NodeList) expr.evaluate(thisNode, XPathConstants.NODESET);
		boolean foundOne = false;
		for (int i=0; i<nodeList.getLength();i++){
			String detail = nodeList.item(i).getTextContent();
			//Have we already added to the aspect name?
			if (foundOne == true) {
				aspectName += "(" + detail + ")";
			} else aspectName += detail;
			foundOne = true;
		}
		
		//Add this aspect to the aspectList, and fill it with tests
		Map <String,Object> thisAspect = new HashMap<String,Object>();
		aspectList.add(thisAspect);
		thisAspect.put("aspectName", aspectName);
		ArrayList <Map<String, Object>> testList = new ArrayList<Map<String, Object>>();		
		thisAspect.put("testList", testList);		
		
		//Now see how many tests we have for this aspect
		xpath = s_xFactory.newXPath();		
		xPathStr = "regex";
		expr = xpath.compile(xPathStr);
		nodeList = (NodeList) expr.evaluate(thisNode, XPathConstants.NODESET);
		for (int i=0; i<nodeList.getLength();i++){
			examineTestNode (nodeList.item(i),testList);
		}

	}
	
	private void examineTestNode (Node thisNode, ArrayList <Map<String, Object>> testList) throws Exception {
		//Loop through the children of <regex>  pull out test as test name
		//and description as assertion (and put the actual expression in brackets
		String testName = "Test Unknown";
		String assertionDesc = "";
		
		NodeList children = thisNode.getChildNodes();
		for (int i=0; i<children.getLength();i++){
			Node childNode = children.item(i);
			switch (childNode.getNodeName()){
				case "test" : 	testName = childNode.getTextContent();
								break;
				case "description" : 	assertionDesc = childNode.getTextContent() + assertionDesc;
										break;
				case "expression" : 	assertionDesc = " (" + childNode.getTextContent() + ")";
				break;										
			}
		}
		
		if (assertionDesc.length() == 0){
			assertionDesc = "Assertion Unknown";
		}
		Map <String,Object> test = new HashMap<String,Object>();
		test.put ("test name", testName);
		test.put ("assertion", assertionDesc);
		testList.add (test);		
		
	}
		
	
	/*
	private static void examineSQLFile (File thisFile, Map <String, Object> tests) throws Exception{
		//Assertion description is expected to be on next line after "Assertion"
		BufferedReader br = new BufferedReader(new FileReader(thisFile));
		String line;
		boolean foundMarker = false;
		while ((line = br.readLine()) != null) {
			if (foundMarker) {
				tests.put(thisFile.getName(), line.trim());
				debug (thisFile.getName() + ": " + line.trim());
				break;
			} else {
				if (line.toLowerCase().contains("assertion")) {
					foundMarker = true;
				}
			}
		}
		br.close();		
	}
	*/
	private void examineSQLFile (File thisFile, ArrayList <Map<String, Object>> aspectList) throws Exception{
		
		String assertionDesc = "Assertion Unknown";
		//Assertion description is expected to be on next line after "Assertion"...or start on the same line.	

		int headerSize = 360;
		String fileHeader;
		DataInputStream dis = null;
		
		try{
			byte[] buff = new byte[headerSize];
			dis = new DataInputStream(new FileInputStream(thisFile));
			dis.readFully(buff);
			fileHeader = new String (buff);
		} catch (EOFException e) {
			debug ("File too short to find assertion: " + thisFile.getName());
			return;
		} finally {
			if (dis != null ) dis.close();
		}
		
		int idxStart = fileHeader.toLowerCase().indexOf("assertion:");
		if (idxStart > -1) {
			int idxEnd = fileHeader.indexOf("*",idxStart);
			if (idxEnd > idxStart) {
				assertionDesc = fileHeader.substring(idxStart + 10, idxEnd).trim();
				//Compress all whitespaces to single space
				assertionDesc = assertionDesc.replaceAll("[\\s]+", " ");
			}
		}
		Map <String,Object> thisAspect = new HashMap<String,Object>();
		//TODO I think we can get much more clever than this by working out what table the SQL relates to.
		//Also the directory that the script lives in can tell us something.
		thisAspect.put("aspectName", "SQL");
		aspectList.add(thisAspect);
		ArrayList <Map<String, Object>> testList = new ArrayList<Map<String, Object>>();
		thisAspect.put("testList", testList);
		
		//Only 1 test per SQL file
		Map <String,Object> test = new HashMap<String,Object>();
		test.put ("test name", thisFile.getName());
		test.put ("assertion", assertionDesc);
		testList.add (test);
	}
	
	private static void debug (String msg) {
		//System.err.println(msg);
	}
	
	private static void error (String msg) {
		System.err.println(msg);
	}	
	
	private static void out (String msg) {
		System.out.println(msg);
	}	
}
