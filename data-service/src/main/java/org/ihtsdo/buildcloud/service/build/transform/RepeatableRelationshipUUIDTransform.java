package org.ihtsdo.buildcloud.service.build.transform;

import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.helper.Type5UuidFactory;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class RepeatableRelationshipUUIDTransform implements LineTransformation {

	private Type5UuidFactory type5UuidFactory;

	public RepeatableRelationshipUUIDTransform() throws NoSuchAlgorithmException {
		type5UuidFactory = new Type5UuidFactory();
	}

	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		// Create repeatable UUID to ensure SCTIDs are reused.
		// (Technique lifted from workbench release process.)
		// sourceId + destinationId + typeId + relationshipGroup
		if (columnValues[0].equals(RF2Constants.NULL_STRING)) {
			try {
				UUID.randomUUID();
				UUID uuid = type5UuidFactory.get(columnValues[4] + columnValues[5] + columnValues[7] + columnValues[6]);
				columnValues[0] = uuid.toString();
			} catch (UnsupportedEncodingException e) {
				throw new TransformationException("Failed to create UUID.", e);
			}
		}
	}

	@Override
	public int getColumnIndex() {
		return -1;
	}

}
