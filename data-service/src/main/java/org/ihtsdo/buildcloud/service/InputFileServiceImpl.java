package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.maven.MavenGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class InputFileServiceImpl extends EntityServiceImpl<InputFile> implements InputFileService {

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private MavenGenerator mavenGenerator;

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileServiceImpl.class);

	@Autowired
	protected InputFileServiceImpl(InputFileDAO dao) {
		super(dao);
	}

	@Override
	public List<InputFile> findAll(String buildCompositeKey, String packageBusinessKey, String authenticatedId) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		List<InputFile> inputFiles = packageDAO.find(buildId, packageBusinessKey, authenticatedId).getInputFiles();
		Hibernate.initialize(inputFiles);
		return inputFiles;
	}

	@Override
	public InputFile find(String buildCompositeKey, String packageBusinessKey, String inputFileBusinessKey, String authenticatedId) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		return inputFileDAO.find(buildId, packageBusinessKey, inputFileBusinessKey, authenticatedId);
	}

	@Override
	public InputFile createUpdate(String buildCompositeKey, String packageBusinessKey, String inputFileName,
								  InputStream fileStream, long fileSize, boolean isManifest, String authenticatedId) throws IOException {

		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		Package aPackage = packageDAO.find(buildId, packageBusinessKey, authenticatedId);

		Date versionDate = new Date();

		InputFile inputFile = findOrCreateInputFile(aPackage, inputFileName);
		mavenGenerator.generateArtifactAndGroupId(inputFile);
		inputFile.setVersionDate(versionDate);

		inputFileDAO.saveFile(fileStream, fileSize, inputFile.getPath());

		// Generate input file pom
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		Map<String, String> metaData = new HashMap<String,String>();
		if (isManifest){
			metaData.put("isManifest", "true");
			inputFile.setMetaData(metaData);
		}
		mavenGenerator.generateArtifactPom(inputFile, new OutputStreamWriter(out));

		// Upload input file pom to S3
		byte[] buf = out.toByteArray();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
		inputFileDAO.saveFilePom(byteArrayInputStream, buf.length, inputFile.getPomPath());

		// Create database entry
		inputFileDAO.save(inputFile);
		return inputFile;
	}

	@Override
	public InputStream getFileStream(InputFile inputFile) throws IOException {
		return inputFileDAO.getFileStream(inputFile);
	}

	private InputFile findOrCreateInputFile(Package aPackage, String inputFileName) {
		String inputFileBusinessKey = EntityHelper.formatAsBusinessKey(inputFileName);
		InputFile inputFile = null;
		for (InputFile file : aPackage.getInputFiles()) {
			if (file.getBusinessKey().equals(inputFileBusinessKey)) {
				inputFile = file;
			}
		}
		if (inputFile == null) {
			inputFile = new InputFile(inputFileName);
			aPackage.addInputFile(inputFile);
		}
		return inputFile;
	}

}
