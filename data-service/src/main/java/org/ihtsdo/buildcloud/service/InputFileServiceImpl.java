package org.ihtsdo.buildcloud.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.maven.MavenArtifact;
import org.ihtsdo.buildcloud.service.maven.MavenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.Set;

@Service
@Transactional
public class InputFileServiceImpl implements InputFileService {

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private MavenGenerator mavenGenerator;

	@Autowired
	private AmazonS3Client s3Client;

	@Autowired
	private String s3BucketName;

	@Override
	public Set<InputFile> findAll(String buildCompositeKey, String packageBusinessKey, String authenticatedId) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		Set<InputFile> inputFiles = packageDAO.find(buildId, packageBusinessKey, authenticatedId).getInputFiles();
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

		InputFile inputFile = new InputFile(inputFileBusinessKey);
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		Package aPackage = packageDAO.find(buildId, packageBusinessKey, authenticatedId);
		aPackage.addInputFile(inputFile);

		MavenArtifact mavenArtifact = mavenGenerator.getArtifact(inputFile);

		// Upload input file to S3
		String artifactPath = mavenGenerator.getPath(mavenArtifact);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(fileSize);
		s3Client.putObject(s3BucketName, artifactPath, fileStream, metadata);

		// Generate input file pom
		String pomPath = mavenGenerator.getPomPath(mavenArtifact);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		mavenGenerator.generateArtifactPom(new OutputStreamWriter(out), mavenArtifact);

		// Upload input file pom to S3
		byte[] buf = out.toByteArray();
		ObjectMetadata pomMetadata = new ObjectMetadata();
		pomMetadata.setContentLength(buf.length);
		s3Client.putObject(s3BucketName, pomPath, new ByteArrayInputStream(buf), pomMetadata);

		// Create database entry
		inputFileDAO.save(inputFile);
		return inputFile;
	}
}
