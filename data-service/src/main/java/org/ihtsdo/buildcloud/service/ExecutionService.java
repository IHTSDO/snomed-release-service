package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Execution;

import java.io.IOException;

public interface ExecutionService {

	Execution create(String buildCompositeKey, String authenticatedId) throws IOException;

}
