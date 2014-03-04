package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Execution;

import java.io.IOException;
import java.util.List;

public interface ExecutionService {

	Execution create(String buildCompositeKey, String authenticatedId) throws IOException;

	List<Execution> findAll(String buildCompositeKey, String authenticatedId);

	Execution find(String buildCompositeKey, String executionId, String authenticatedId);

}
