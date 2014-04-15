package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import org.hibernate.Query;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.helper.S3PutRequestBuilder;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Repository;

import java.io.InputStream;

@Repository
public class InputFileDAOImpl extends EntityDAOImpl<InputFile> implements InputFileDAO {

	@Autowired
	private S3Client s3Client;

	@Autowired
	private S3ClientHelper s3ClientHelper;

	private String mavenS3BucketName;

	@Override
	public InputFile find(Long buildId, String packageBusinessKey, String inputFileBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select inputFile " +
				"from ReleaseCenterMembership membership " +
				"join membership.releaseCenter releaseCenter " +
				"join releaseCenter.extensions extension " +
				"join extension.products product " +
				"join product.builds build " +
				"join build.packages package " +
				"join package.inputFiles inputFile " +
				"where membership.user.oauthId = :oauthId " +
				"and build.id = :buildId " +
				"and package.businessKey = :packageBusinessKey " +
				"and inputFile.businessKey = :inputFileBusinessKey " +
				"order by inputFile.id ");
		query.setString("oauthId", authenticatedId);
		query.setLong("buildId", buildId);
		query.setString("packageBusinessKey", packageBusinessKey);
		query.setString("inputFileBusinessKey", inputFileBusinessKey);

		return (InputFile) query.uniqueResult();
	}

	@Override
	public void saveFile(InputStream fileStream, long fileSize, String artifactPath) {
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(mavenS3BucketName, artifactPath, fileStream).length(fileSize).useBucketAcl();
		s3Client.putObject(putRequest);
	}

	@Override
	public void saveFilePom(InputStream inputStream, int length, String pomPath) {
		S3PutRequestBuilder putRequest = s3ClientHelper.newPutRequest(mavenS3BucketName, pomPath, inputStream).length(length).useBucketAcl();
		s3Client.putObject(putRequest);
	}

	@Override
	public InputStream getFileStream(InputFile inputFile) {
		try {
			S3Object s3Object = s3Client.getObject(mavenS3BucketName, inputFile.getPath());
			if (s3Object != null) {
				return s3Object.getObjectContent();
			}
		} catch (AmazonS3Exception e) {
			if (404 != e.getStatusCode()) {
				throw e;
			}
		}
		return null;
	}

	@Required
	public void setMavenS3BucketName(String mavenS3BucketName) {
		this.mavenS3BucketName = mavenS3BucketName;
	}

}
