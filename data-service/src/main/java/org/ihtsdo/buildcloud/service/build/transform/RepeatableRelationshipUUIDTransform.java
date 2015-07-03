package org.ihtsdo.buildcloud.service.build.transform;

import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.build.RF2Constants.RelationshipFileType;
import org.ihtsdo.buildcloud.service.helper.Type5UuidFactory;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class RepeatableRelationshipUUIDTransform implements LineTransformation {

	private Type5UuidFactory type5UuidFactory;

	private RelationshipFileType relationshipFileType;

	private static final String STATED_RELATIONSHIP_MODIFIER = "S";

	public RepeatableRelationshipUUIDTransform(RelationshipFileType stated) throws NoSuchAlgorithmException {
		type5UuidFactory = new Type5UuidFactory();
	}

	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		// Create repeatable UUID to ensure SCTIDs are reused.
		// (Technique lifted from workbench release process.)
		// sourceId + destinationId + typeId + relationshipGroup
		if (columnValues[0] == null || columnValues[0].equals(RF2Constants.NULL_STRING) || columnValues[0].isEmpty()) {
			try {
				columnValues[0] = getCalculatedUuidFromRelationshipValues(columnValues);
			} catch (UnsupportedEncodingException e) {
				throw new TransformationException("Failed to create UUID.", e);
			}
		}
	}

	public String getCalculatedUuidFromRelationshipValues(String[] columnValues) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		sb.append(columnValues[4]).append(columnValues[5]).append(columnValues[7]).append(columnValues[6]);

		// Stated relationships need to be different from inferred ones, for the same triple + group
		if (relationshipFileType == RF2Constants.RelationshipFileType.STATED) {
			sb.append(STATED_RELATIONSHIP_MODIFIER);
		}
		return type5UuidFactory.get(sb.toString()).toString();
	}

	@Override
	public int getColumnIndex() {
		return -1;
	}

}
