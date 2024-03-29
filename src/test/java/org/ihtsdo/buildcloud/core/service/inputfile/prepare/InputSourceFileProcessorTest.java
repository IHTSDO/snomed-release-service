package org.ihtsdo.buildcloud.core.service.inputfile.prepare;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.manifest.ManifestValidator;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
public class InputSourceFileProcessorTest {
	
	private static final String DA = "da";
	private static final String EN = "en";
	private static final String DE = "de";
	private static final String IT = "it";
	private static final String EXTERNALLY_MAINTAINED = "externally-maintained";
	private static final String TERMINOLOGY_SERVER = "terminology-server";
	@Autowired
	private S3PathHelper s3PathHelper;

	@Value("${srs.storage.bucketName}")
	private String buildBucketName;
	
	@Autowired
	private S3Client s3Client;
	
	private FileHelper fileHelper;
	private InputSourceFileProcessor processor;
	private Product product;
	private Build build;
	
	@BeforeEach
	public void setUp() {
		fileHelper = new FileHelper(buildBucketName, s3Client);
		product = new Product(getClass().getName());
		ReleaseCenter releaseCenter = new ReleaseCenter("International", "int");
		product.setReleaseCenter(releaseCenter);
		build = new Build(new Date(), product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), product.getBuildConfiguration(), product.getQaTestConfig());
	}

	@Test
	public void testLoadingCHManifestWithMultipleLanguageCodesAndModuleIdsInDescriptionFile() throws Exception {
		validateManifest("manifest_with_multiple_language_codes_and_module_ids.xml");
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_multiple_language_codes_and_module_ids.xml");
		processor = new InputSourceFileProcessor(fileHelper, s3PathHelper, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
		processor.loadFileProcessConfigsFromManifest(manifestStream);
		//description configs
		Map<String, FileProcessingConfig>  descriptionProcessingConfigs = processor.getDescriptionFileProcessingConfigs();
		assertEquals(5,descriptionProcessingConfigs.keySet().size());
		assertTrue(descriptionProcessingConfigs.containsKey(EN));
		assertTrue(descriptionProcessingConfigs.containsKey(DE));
		assertTrue(descriptionProcessingConfigs.containsKey(IT));
		assertTrue(descriptionProcessingConfigs.containsKey("fr-11000241103"));
		assertTrue(descriptionProcessingConfigs.containsKey("fr-2011000195101"));
		FileProcessingConfig enDescriptionConfig = descriptionProcessingConfigs.get(EN);
		assertEquals(EN,enDescriptionConfig.getKey());
		assertEquals("sct2_Description_Delta-en_CH1000195_20211207.txt", enDescriptionConfig.getTargetFileName());
		assertEquals(1, enDescriptionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, enDescriptionConfig.getSpecificSources().iterator().next());

		FileProcessingConfig fr_11000241103_DescriptionConfig = descriptionProcessingConfigs.get("fr-11000241103");
		assertEquals("fr-11000241103",fr_11000241103_DescriptionConfig.getKey());
		assertEquals("sct2_Description_Delta-fr_CH1000195_20211207.txt", fr_11000241103_DescriptionConfig.getTargetFileName());
		assertEquals(1, fr_11000241103_DescriptionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, fr_11000241103_DescriptionConfig.getSpecificSources().iterator().next());

		FileProcessingConfig fr_2011000195101_DescriptionConfig = descriptionProcessingConfigs.get("fr-2011000195101");
		assertEquals("fr-2011000195101",fr_2011000195101_DescriptionConfig.getKey());
		assertEquals("sct2_Description_Delta-fr-ch_CH1000195_20211207.txt", fr_2011000195101_DescriptionConfig.getTargetFileName());
		assertEquals(1, fr_2011000195101_DescriptionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, fr_2011000195101_DescriptionConfig.getSpecificSources().iterator().next());
	}
	
	@Test
	public void testLoadingDKManifestWithMultipleLanguageCodesInDescriptionFile() throws Exception {
		validateManifest("manifest_with_multiple_language_codes.xml");
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_multiple_language_codes.xml");
		processor = new InputSourceFileProcessor(fileHelper, s3PathHelper, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
		processor.loadFileProcessConfigsFromManifest(manifestStream);
		//description configs
		Map<String, FileProcessingConfig>  descriptionProcessingConfigs = processor.getDescriptionFileProcessingConfigs();
		assertEquals(2,descriptionProcessingConfigs.keySet().size());
		assertTrue(descriptionProcessingConfigs.containsKey(EN));
		assertTrue(descriptionProcessingConfigs.containsKey(DA));
		FileProcessingConfig enDescriptionConfig = descriptionProcessingConfigs.get(EN);
		assertEquals(EN,enDescriptionConfig.getKey());
		assertEquals("xsct2_Description_Delta_DK1000005_20170731.txt", enDescriptionConfig.getTargetFileName());
		assertEquals(1, enDescriptionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, enDescriptionConfig.getSpecificSources().iterator().next());
		
		FileProcessingConfig daDescriptionConfig = descriptionProcessingConfigs.get(DA);
		assertEquals(DA,daDescriptionConfig.getKey());
		assertEquals("xsct2_Description_Delta_DK1000005_20170731.txt", daDescriptionConfig.getTargetFileName());
		assertEquals(1, daDescriptionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, daDescriptionConfig.getSpecificSources().iterator().next());
	}
	
	@Test
	public void testLoadingDKManifest() throws Exception {
		validateManifest("manifest_ms_dk.xml");
		InputStream manifestStream = getClass().getResourceAsStream("manifest_ms_dk.xml");
		processor = new InputSourceFileProcessor(fileHelper, s3PathHelper, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
		processor.loadFileProcessConfigsFromManifest(manifestStream);
		//description configs
		Map<String, FileProcessingConfig>  descriptionProcessingConfigs = processor.getDescriptionFileProcessingConfigs();
		assertEquals(2,descriptionProcessingConfigs.keySet().size());
		assertTrue(descriptionProcessingConfigs.containsKey(EN));
		assertTrue(descriptionProcessingConfigs.containsKey(DA));
		FileProcessingConfig enDescriptionConfig = descriptionProcessingConfigs.get(EN);
		assertEquals(EN,enDescriptionConfig.getKey());
		assertEquals("xsct2_Description_Delta-en_DK1000005_20170731.txt", enDescriptionConfig.getTargetFileName());
		assertEquals(1, enDescriptionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, enDescriptionConfig.getSpecificSources().iterator().next());
		
		FileProcessingConfig daDescriptionConfig = descriptionProcessingConfigs.get(DA);
		assertEquals(DA,daDescriptionConfig.getKey());
		assertEquals("xsct2_Description_Delta-da_DK1000005_20170731.txt", daDescriptionConfig.getTargetFileName());
		assertEquals(1, daDescriptionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, daDescriptionConfig.getSpecificSources().iterator().next());

		//text definition configs
		Map<String, FileProcessingConfig>  textDefinitionConfigs = processor.getTextDefinitionFileProcessingConfigs();
		assertEquals(2,textDefinitionConfigs.keySet().size());
		assertEquals(2,textDefinitionConfigs.keySet().size());
		assertTrue(textDefinitionConfigs.containsKey(EN));
		assertTrue(textDefinitionConfigs.containsKey(DA));
		FileProcessingConfig enTextDefinitionConfig = textDefinitionConfigs.get(EN);
		assertEquals(EN,enTextDefinitionConfig.getKey());
		assertEquals("xsct2_TextDefinition_Delta-en_DK1000005_20170731.txt", enTextDefinitionConfig.getTargetFileName());
		assertEquals(1, enTextDefinitionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, enTextDefinitionConfig.getSpecificSources().iterator().next());
		
		FileProcessingConfig daTextDefinitionConfig = textDefinitionConfigs.get(DA);
		assertEquals(DA,daTextDefinitionConfig.getKey());
		assertEquals("xsct2_TextDefinition_Delta-da_DK1000005_20170731.txt", daTextDefinitionConfig.getTargetFileName());
		assertEquals(1, daTextDefinitionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, daTextDefinitionConfig.getSpecificSources().iterator().next());

		MultiValueMap<String, String> filesToCopy = processor.getFilesToCopyFromSource();
		assertEquals(0, filesToCopy.keySet().size());
		String [] filesFromTermServerOnly = {"sct2_Concept_Delta_DK1000005_20170731.txt",
				"sct2_StatedRelationship_Delta_DK1000005_20170731.txt"};
		String [] filesWithoutSourcesSpecified = {"sct2_Relationship_Delta_DK1000005_20170731.txt"};
		
		for (String fileName : filesToCopy.keySet()) {
			if (Arrays.asList(filesFromTermServerOnly).contains(fileName)) {
				assertEquals(1, filesToCopy.get(fileName).size());
				assertEquals(TERMINOLOGY_SERVER,filesToCopy.get(fileName).get(0));
			} else if (Arrays.asList(filesWithoutSourcesSpecified).contains(fileName)) {
				assertEquals(0, filesToCopy.get(fileName).size());
			} 
		}		
		Map<String, FileProcessingConfig> refsetConfigs = processor.getRefsetFileProcessingConfigs();
		assertEquals(11,refsetConfigs.size());
		System.out.println("total" + refsetConfigs.size());
		String [] associcaitionRefsetIds = {"900000000000523009","900000000000524003","900000000000525002","900000000000526001",
				"900000000000527005","900000000000528000","900000000000530003","900000000000531004"};
		String[] refsetsFromTermServer = {"900000000000523009","900000000000524003","900000000000525002",
				"900000000000526001","900000000000527005","900000000000528000","900000000000530003","900000000000489007"};
		String [] refsetIdsFromExternal = {"900000000000531004","900000000000490003"};
		String [] refsetIdsWithoutSourceSepcified = {"723264001"};
		
		for (String refsetId : associcaitionRefsetIds) {
			assertTrue(refsetConfigs.containsKey(refsetId), "must contain refsetId " + refsetId);
		}
		
		for (String refsetId : refsetsFromTermServer) {
			assertTrue(refsetConfigs.containsKey(refsetId), "must contain refsetId " + refsetId);
		}
		
		for (String refsetId : refsetIdsWithoutSourceSepcified) {
			assertTrue(refsetConfigs.containsKey(refsetId), "must contain refsetId " + refsetId);
		}
	
		for (String refsetid : refsetConfigs.keySet()) {
			if (Arrays.asList(associcaitionRefsetIds).contains(refsetid)) {
				assertEquals("xder2_cRefset_AssociationReferenceDelta_DK1000005_20170731.txt", refsetConfigs.get(refsetid).getTargetFileName());
			}
			if (Arrays.asList(refsetsFromTermServer).contains(refsetid)) {
				assertEquals(TERMINOLOGY_SERVER, refsetConfigs.get(refsetid).getSpecificSources().iterator().next());
			}
			if (Arrays.asList(refsetIdsFromExternal).contains(refsetid)) {
				assertEquals(EXTERNALLY_MAINTAINED, refsetConfigs.get(refsetid).getSpecificSources().iterator().next());
			}
			if (Arrays.asList(refsetIdsWithoutSourceSepcified).contains(refsetid)) {
				assertEquals(0, refsetConfigs.get(refsetid).getSpecificSources().size());
			}
		}
		assertEquals(0, processor.getRefsetWithAdditionalFields().size());
	}
	
	@Test
	public void testLoadingManifestMissingRefsets() throws Exception {
		validateManifest("manifest_with_mixed_sources.xml");
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
		processor = new InputSourceFileProcessor(fileHelper, s3PathHelper, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
		SourceFileProcessingReport report = processor.processFiles(manifestStream, Collections.emptyList(), build.getId(),null);
		assertNotNull(report);
		assertNull(report.getDetails().get(ReportType.ERROR));
		Map<String,FileProcessingConfig> refsetProcessingConfig = processor.getRefsetFileProcessingConfigs();
		assertEquals(6,refsetProcessingConfig.keySet().size());
		String [] refsetIds = {"900000000000525002","900000000000524003","900000000000530003","900000000000526001","900000000000528000"};
		for (String refsetId : refsetIds) {
			assertTrue(refsetProcessingConfig.containsKey(refsetId), "must contain " + refsetId);
		}
		for (FileProcessingConfig config : refsetProcessingConfig.values()) {
			assertEquals("xder2_cRefset_AssociationReferenceDelta_INT_20190731.txt" , config.getTargetFileName());
			assertEquals(1, config.getSpecificSources().size());
			if ("900000000000530003".equals(config.getKey())) {
				assertEquals(EXTERNALLY_MAINTAINED,config.getSpecificSources().iterator().next());
			} else {
				assertEquals(TERMINOLOGY_SERVER,config.getSpecificSources().iterator().next());
			}
		}
	}
	
	@Test
	public void testLoadingManifestProcessingConfigWithMixedSources() throws Exception {
		validateManifest("manifest_with_mixed_sources.xml");
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_mixed_sources.xml");
		processor = new InputSourceFileProcessor(fileHelper, s3PathHelper, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), true);
		processor.loadFileProcessConfigsFromManifest(manifestStream);
		
		Map<String, FileProcessingConfig>  descriptionProcessingConfig = processor.getDescriptionFileProcessingConfigs();
		assertEquals(1,descriptionProcessingConfig.keySet().size());
		FileProcessingConfig descriptionConfig = descriptionProcessingConfig.values().iterator().next();
		assertEquals(EN,descriptionConfig.getKey());
		assertEquals("xsct2_Description_Delta-en_INT_20170731.txt", descriptionConfig.getTargetFileName());
		assertEquals(1, descriptionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, descriptionConfig.getSpecificSources().iterator().next());
		
		Map<String, FileProcessingConfig>  testDefinitionConfigs = processor.getTextDefinitionFileProcessingConfigs();
		assertEquals(1,testDefinitionConfigs.keySet().size());
		FileProcessingConfig definitionConfig = testDefinitionConfigs.values().iterator().next();
		assertEquals(EN,definitionConfig.getKey());
		assertEquals("xsct2_TextDefinition_Delta-en_INT_20170731.txt", definitionConfig.getTargetFileName());
		assertEquals(1, definitionConfig.getSpecificSources().size());
		assertEquals(TERMINOLOGY_SERVER, descriptionConfig.getSpecificSources().iterator().next());
		
		MultiValueMap<String, String> filesToCopy = processor.getFilesToCopyFromSource();
		assertEquals(9, filesToCopy.keySet().size());
		String [] filesFromTermServerOnly = {"sct2_Concept_Delta_INT_20170731.txt",
				"sct2_StatedRelationship_Delta_INT_20170731.txt",
				"sct2_Relationship_Delta_INT_20170731.txt"};
	
		String [] filesFromExternalOnly = {"der2_sssssssRefset_MRCMDomainDelta_INT_20170731.txt",
				"der2_ssccRefset_MRCMAttributeRangeDelta_INT_20170731.txt",
				"der2_cRefset_MRCMModuleScopeDelta_INT_20170731.txt"};
		String [] filesWithBothSources = {"sct2_Identifier_Delta_INT_20170731.txt"};
		
		for (String fileName : filesToCopy.keySet()) {
			if (Arrays.asList(filesFromTermServerOnly).contains(fileName)) {
				assertEquals(1, filesToCopy.get(fileName).size());
				assertEquals(TERMINOLOGY_SERVER,filesToCopy.get(fileName).get(0));
			} else if (Arrays.asList(filesFromExternalOnly).contains(fileName)) {
				assertEquals(1, filesToCopy.get(fileName).size());
				assertEquals(EXTERNALLY_MAINTAINED,filesToCopy.get(fileName).get(0));
			} else if (Arrays.asList(filesWithBothSources).contains(fileName)) {
				assertEquals(2, filesToCopy.get(fileName).size());
				assertTrue(filesToCopy.get(fileName).contains(EXTERNALLY_MAINTAINED));
				assertTrue(filesToCopy.get(fileName).contains(TERMINOLOGY_SERVER));
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
