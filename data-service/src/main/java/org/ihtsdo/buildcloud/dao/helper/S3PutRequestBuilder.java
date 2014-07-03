package org.ihtsdo.buildcloud.dao.helper;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.io.InputStream;

/**
 * Wraps the JetS3t ObjectMetadata enabling builder pattern.
 */
public class S3PutRequestBuilder extends PutObjectRequest {

	private S3ClientHelper helper;

	protected S3PutRequestBuilder(String bucketName, String key, InputStream input, S3ClientHelper helper) {
		super(bucketName, key, input, new ObjectMetadata());
		this.helper = helper;
	}

	public S3PutRequestBuilder length(long contentLength) {
		this.getMetadata().setContentLength(contentLength);
		return this;
	}
	
	public S3PutRequestBuilder withMD5(String md5HexString) throws DecoderException {
		//Amazon expects the md5 value to be base64 encoded
		byte[] decodedHex = Hex.decodeHex(md5HexString.toCharArray());
		
		//Apparently we need the unchunked string encoding method here to match what AWS is expecting.
		String md5Base64 = Base64.encodeBase64String(decodedHex);
		this.getMetadata().setContentMD5(md5Base64);
		return this;
	}

	public S3PutRequestBuilder useBucketAcl() {
		helper.useBucketAcl(this);
		return this;
	}

	public void setHelper(S3ClientHelper helper) {
		this.helper = helper;
	}
}
