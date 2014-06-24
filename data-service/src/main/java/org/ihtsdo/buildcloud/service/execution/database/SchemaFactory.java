package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;

public class SchemaFactory {

	public TableSchema createSchemaBean(String filename, String headerLine) throws FileRecognitionException {

		// General File Naming Pattern
		// <FileType>_<ContentType>_<ContentSubType>_<Country|Namespace>_<VersionDate>.<Extension>
		// See http://www.snomed.org/tig?t=fng2_convention

		String filenameNoExtension = filename.substring(0, filename.indexOf("."));

		String[] nameParts = filenameNoExtension.split(RF2Constants.FILE_NAME_SEPARATOR);
		if (nameParts.length == 5) {
			String fileType = nameParts[0];
			String contentType = nameParts[1];

			if (fileType.equals("der2") || fileType.equals("xder2")) {
				if (contentType.equals("Refset")) {
					// Simple Refset

					TableSchema simpleRefset = new TableSchema(filenameNoExtension)
							.field("id", DataType.UUID)
							.field("effectiveTime", DataType.TIME)
							.field("active", DataType.BOOLEAN)
							.field("moduleId", DataType.SCTID)
							.field("refSetId", DataType.SCTID)
							.field("referencedComponentId", DataType.SCTID);

					return simpleRefset;

				} else if (contentType.endsWith("Refset")) {
					// Other Refset

					// Use the contentType prefix characters for datatypes and the header line for names of additional fields.

					throw new FileRecognitionException("Only the simple refset type is currently supported.");
				} else {
					throw new FileRecognitionException("Content type '" + contentType + "' is not supported.");
				}
			} else {
				throw new FileRecognitionException("File type '" + fileType + "' is not supported.");
			}
		} else {
			throw new FileRecognitionException("Unexpected filename format. Filename contains " + (nameParts.length - 1) +
					" underscores, expected 4.");
		}

	}

}
