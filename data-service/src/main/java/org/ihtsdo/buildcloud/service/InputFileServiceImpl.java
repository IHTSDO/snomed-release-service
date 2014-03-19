package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.maven.MavenArtifact;
import org.ihtsdo.buildcloud.service.maven.MavenGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.Date;
import java.util.List;

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
	public InputFile create(String buildCompositeKey, String packageBusinessKey, String inputFileBusinessKey,
							InputStream fileStream, long fileSize, String authenticatedId) throws IOException {

		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		Package aPackage = packageDAO.find(buildId, packageBusinessKey, authenticatedId);

		Date versionDate = new Date();

		InputFile inputFile = findOrCreateInputFile(aPackage, inputFileBusinessKey);
		inputFile.setVersionDate(versionDate);

		MavenArtifact mavenArtifact = mavenGenerator.getArtifact(inputFile);

		inputFileDAO.saveFile(fileStream, fileSize, mavenArtifact.getPath());

		// Generate input file pom
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		mavenGenerator.generateArtifactPom(new OutputStreamWriter(out), mavenArtifact);

		// Upload input file pom to S3
		byte[] buf = out.toByteArray();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
		inputFileDAO.saveFilePom(byteArrayInputStream, buf.length, mavenArtifact.getPomPath());

		// Create database entry
		inputFileDAO.save(inputFile);
		return inputFile;
	}

	@Override
	public InputStream getFileStream(InputFile inputFile) throws IOException {
		MavenArtifact mavenArtifact = mavenGenerator.getArtifact(inputFile);
		String artifactPath = mavenArtifact.getPath();
		LOGGER.info("Serving file. Path: {}", artifactPath);
		return inputFileDAO.getFileStream(artifactPath);
	}

	private InputFile findOrCreateInputFile(Package aPackage, String inputFileBusinessKey) {
		InputFile inputFile = null;
		for (InputFile file : aPackage.getInputFiles()) {
			if (file.getBusinessKey().equals(inputFileBusinessKey)) {
				inputFile = file;
			}
		}
		if (inputFile == null) {
			inputFile = new InputFile(inputFileBusinessKey);
			aPackage.addInputFile(inputFile);
		}
		return inputFile;
	}

}
