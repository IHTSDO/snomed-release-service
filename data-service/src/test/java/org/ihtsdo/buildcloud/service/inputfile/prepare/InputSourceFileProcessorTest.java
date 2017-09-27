package org.ihtsdo.buildcloud.service.inputfile.prepare;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.manifest.ManifestValidator;
import org.ihtsdo.buildcloud.service.inputfile.prepare.FileProcessingConfig;
import org.ihtsdo.buildcloud.service.inputfile.prepare.InputSourceFileProcessor;
import org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
public class InputSourceFileProcessorTest {
	
	@Autowired
	private BuildS3PathHelper s3PathHelper;
	@Autowired
	private String buildBucketName;
	
	@Autowired
	private S3Client s3Client;
	
	@Autowired
	private S3ClientHelper s3ClientHelper;
	
	private FileHelper fileHelper;
	private InputSourceFileProcessor processor;
	private Product product;
	
	@Before
	public void setUp() {
		fileHelper = new FileHelper(buildBucketName, s3Client, s3ClientHelper);
		product = new Product(getClass().getName());
		ReleaseCenter releaseCenter = new ReleaseCenter("International", "int");
		product.setReleaseCenter(releaseCenter);
	}

	@Test
	public void testLoadingManifestWithoutSpecifyingSources() throws Exception {
		validateManifest("manifest_with_mixed_sources.xml");
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_mixed_sources.xml");
		processor = new InputSourceFileProcessor(manifestStream, fileHelper, s3PathHelper, product, true);
		processor.loadFileProcessConfigsFromManifest();
		Map<String, FileProcessingConfig>  descriptionProcessingConfig = processor.getDescriptionFileProcessingConfigs();
		for (String descritionFile : descriptionProcessingConfig.keySet()) {
			System.out.println(descriptionProcessingConfig.get(descritionFile));
		}
		MultiValueMap<String, String> filesToCopy = processor.getFilesToCopyFromSource();
		for (String fileName : filesToCopy.keySet()) {
			System.out.println("File name to copy:" + fileName);
			System.out.println(filesToCopy.get(fileName));
		}
		processor.getTextDefinitionFileProcessingConfigs();
		
	}
	
	@Test
	public void testLoadingDKManifest() throws Exception {
		validateManifest("manifest_dk.xml");
		InputStream manifestStream = getClass().getResourceAsStream("manifest_dk.xml");
		processor = new InputSourceFileProcessor(manifestStream, fileHelper, s3PathHelper, product, true);
		processor.loadFileProcessConfigsFromManifest();
		Map<String, FileProcessingConfig>  descriptionProcessingConfig = processor.getDescriptionFileProcessingConfigs();
		for (String descritionFile : descriptionProcessingConfig.keySet()) {
			System.out.println(descriptionProcessingConfig.get(descritionFile));
		}
		MultiValueMap<String, String> filesToCopy = processor.getFilesToCopyFromSource();
		for (String fileName : filesToCopy.keySet()) {
			System.out.println("File name to copy:" + fileName);
			System.out.println(filesToCopy.get(fileName));
		}
		processor.getTextDefinitionFileProcessingConfigs();
		
	}
	
	@Test
	public void testLoadingManifestMissingRefsets() throws Exception {
		validateManifest("manifest_all_sources.xml");
	}
	
	@Test
	public void testLoadingManifestRestrictedSources() throws Exception {
		validateManifest("manifest_restricted_sources.xml");
	}
	
	@Test
	public void testLoadingManifestForSeTestXml() throws Exception {
		validateManifest("manifest_se_test.xml");
	}
	
	@Test
	public void testLoadingManifestWithAdditonalFieldsXml() throws Exception {
		validateManifest("manifest_with_additonal_fields.xml");
	}
	
	@Test
	public void testLoadingManifestWithMultipleSourcesXml() throws Exception {
		validateManifest("manifest_with_multiple_sources.xml");
	}
	
	@Test
	public void testLoadingManifestWithUnprocessedXml() throws Exception {
		validateManifest("manifest_with_unprocessed.xml");
	}
	
	@Test
	public void testSourcesDefinedBothInFileTypeAndRefset() throws Exception {
		validateManifest("manifest_with_sources_in_fileType_and_refset.xml");
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_sources_in_fileType_and_refset.xml");
		manifestStream = getClass().getResourceAsStream("manifest_with_sources_in_fileType_and_refset.xml");
		processor = new InputSourceFileProcessor(manifestStream, fileHelper, s3PathHelper, product, true);
		SourceFileProcessingReport report = processor.processFiles(Collections.emptyList());
		System.out.println(report);
		assertNotNull(report);
		assertNull(report.getDetails().get(ReportType.ERROR));
		Map<String,FileProcessingConfig> refsetProcessingConfig = processor.getRefsetFileProcessingConfigs();
		assertEquals(6,refsetProcessingConfig.keySet().size());
		assertTrue("must contain 900000000000525002", refsetProcessingConfig.keySet().contains("900000000000525002"));
		assertTrue("must contain 900000000000527005", refsetProcessingConfig.keySet().contains("900000000000527005"));
		assertTrue("must contain 900000000000524003", refsetProcessingConfig.keySet().contains("900000000000524003"));
		assertTrue("must contain 900000000000530003", refsetProcessingConfig.keySet().contains("900000000000530003"));
		assertTrue("must contain 900000000000526001", refsetProcessingConfig.keySet().contains("900000000000526001"));
		assertTrue("must contain 900000000000528000", refsetProcessingConfig.keySet().contains("900000000000528000"));
		for (FileProcessingConfig config : refsetProcessingConfig.values()) {
			assertEquals("xder2_cRefset_AssociationReferenceDelta_INT_20190731.txt" , config.getTargetFileName());
			assertEquals(1, config.getSpecifiedSources().size());
			if ("900000000000530003".equals(config.getValue())) {
				assertEquals("externally-maintained",config.getSpecifiedSources().iterator().next());
			} else {
				assertEquals("terminology-server",config.getSpecifiedSources().iterator().next());
			}
		}
	}
	
	@Test
	public void testLoadingManifestProcessingConfigWithMixedSources() throws Exception {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_mixed_sources.xml");
		Product product = new Product();
		processor = new InputSourceFileProcessor(manifestStream, fileHelper, s3PathHelper, product, true);
		processor.loadFileProcessConfigsFromManifest();
		
		Map<String, FileProcessingConfig>  descriptionProcessingConfig = processor.getDescriptionFileProcessingConfigs();
		assertEquals(1,descriptionProcessingConfig.keySet().size());
		FileProcessingConfig descriptionConfig = descriptionProcessingConfig.values().iterator().next();
		assertEquals("en",descriptionConfig.getValue());
		assertEquals("xsct2_Description_Delta-en_INT_20170731.txt", descriptionConfig.getTargetFileName());
		assertEquals(1, descriptionConfig.getSpecifiedSources().size());
		assertEquals("terminology-server", descriptionConfig.getSpecifiedSources().iterator().next());
		
		Map<String, FileProcessingConfig>  testDefinitionConfigs = processor.getTextDefinitionFileProcessingConfigs();
		assertEquals(1,testDefinitionConfigs.keySet().size());
		FileProcessingConfig definitionConfig = testDefinitionConfigs.values().iterator().next();
		assertEquals("en",definitionConfig.getValue());
		assertEquals("xsct2_TextDefinition_Delta-en_INT_20170731.txt", definitionConfig.getTargetFileName());
		assertEquals(1, definitionConfig.getSpecifiedSources().size());
		assertEquals("terminology-server", descriptionConfig.getSpecifiedSources().iterator().next());
		
		MultiValueMap<String, String> filesToCopy = processor.getFilesToCopyFromSource();
		assertEquals(13, filesToCopy.keySet().size());
		String [] filesFromTermServerSource = {"sct2_Concept_Delta_INT_20170731.txt",
				"sct2_StatedRelationship_Delta_INT_20170731.txt",
				"sct2_Relationship_Delta_INT_20170731.txt"};
	
		String [] filesFromExternalource = {"der2_sssssssRefset_MRCMDomainDelta_INT_20170731.txt",
				"der2_ssccRefset_MRCMAttributeRangeDelta_INT_20170731.txt",
				"der2_cRefset_MRCMModuleScopeDelta_INT_20170731.txt"};
		String [] filesWithBothSources = {"sct2_Identifier_Delta_INT_20170731.txt"};
		
		for (String fileName : filesToCopy.keySet()) {
			if (Arrays.asList(filesFromTermServerSource).contains(fileName)) {
				assertEquals(1, filesToCopy.get(fileName).size());
				assertEquals("terminology-server",filesToCopy.get(fileName).get(0));
			} else if (Arrays.asList(filesFromExternalource).contains(fileName)) {
				assertEquals(1, filesToCopy.get(fileName).size());
				assertEquals("externally-maintained",filesToCopy.get(fileName).get(0));
			} else if (Arrays.asList(filesWithBothSources).contains(fileName)) {
				assertEquals(2, filesToCopy.get(fileName).size());
				assertTrue(filesToCopy.get(fileName).contains("externally-maintained"));
				assertTrue(filesToCopy.get(fileName).contains("terminology-server"));
			} else {
				assertEquals(0,filesToCopy.get(fileName).size());
			}
		}
	}

	private void validateManifest(String manifestFileName) {
		InputStream manifestStream = getClass().getResourceAsStream(manifestFileName);
		String validationMsg = ManifestValidator.validate(manifestStream);
		if (validationMsg != null) {
			fail(manifestFileName + " is not valid. " + validationMsg);
		}
	}
}
