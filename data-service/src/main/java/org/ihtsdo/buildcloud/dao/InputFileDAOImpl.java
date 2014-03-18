package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.hibernate.Query;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Repository;

import java.io.InputStream;

@Repository
public class InputFileDAOImpl extends EntityDAOImpl<InputFile> implements InputFileDAO {

	@Autowired
	private AmazonS3Client s3Client;

	private String mavenS3BucketName;

	@Override
	public InputFile find(Long buildId, String packageBusinessKey, String inputFileBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select inputFile " +
				"from ReleaseCentreMembership membership " +
				"join membership.releaseCentre releaseCentre " +
				"join releaseCentre.extensions extension " +
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
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(fileSize);
		s3Client.putObject(mavenS3BucketName, artifactPath, fileStream, metadata);
	}

	@Override
	public void saveFilePom(InputStream inputStream, int length, String pomPath) {
		ObjectMetadata pomMetadata = new ObjectMetadata();
		pomMetadata.setContentLength(length);
		s3Client.putObject(mavenS3BucketName, pomPath, inputStream, pomMetadata);
	}

	@Override
	public InputStream getFileStream(String artifactPath) {
		try {
			S3Object s3Object = s3Client.getObject(mavenS3BucketName, artifactPath);
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
