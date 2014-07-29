package org.ihtsdo.buildcloud.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;

/**
 * Service to publish a build release after verification tests are 
 * carried out successfully. It also provides functions to list all available published release packages.
 */
public interface PublishService {

	List<String> getPublishedPackages(Product product);
	void publishExecutionPackage(Execution execution, Package pk) throws IOException;
	void publishPackage(String buildCompositeKey, String packageBusinessKey,
			String productBusinessKey, InputStream inputStream, String originalFilename, long size,
			User subject) throws ResourceNotFoundException, BadRequestException, IOException;
	boolean exists(Product product, String previouslyPublishedPackageName);
}
