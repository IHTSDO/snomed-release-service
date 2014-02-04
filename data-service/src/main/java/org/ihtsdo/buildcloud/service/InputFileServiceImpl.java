package org.ihtsdo.buildcloud.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Set;

@Service
@Transactional
public class InputFileServiceImpl implements InputFileService {

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private AmazonS3Client s3Client;

	@Autowired
	private String S3BucketName;

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
							InputStream fileStream, long fileSize, String authenticatedId) {

		// Upload data to S3
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(fileSize);
		s3Client.putObject(S3BucketName, inputFileBusinessKey, fileStream, metadata);

		// Create database entry
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		Package aPackage = packageDAO.find(buildId, packageBusinessKey, authenticatedId);
		InputFile inputFile = new InputFile(inputFileBusinessKey);
		aPackage.addInputFile(inputFile);
		inputFileDAO.save(inputFile);
		return inputFile;
	}
}
