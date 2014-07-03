package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;

public class SchemaFactory {

	public static final char REFSET_FILENAME_CONCEPT_FIELD = 'c';
	public static final char REFSET_FILENAME_INTEGER_FIELD = 'i';
	public static final char REFSET_FILENAME_STRING_FIELD = 's';

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

					TableSchema simpleRefset = createSimpleRefsetSchema(filenameNoExtension);
					return simpleRefset;

				} else if (contentType.endsWith("Refset")) {
					// Other Refset

					// Start with Simple Refset
					TableSchema refset = createSimpleRefsetSchema(filenameNoExtension);

					// Use the contentType prefix characters for datatypes and the header line for names of additional fields.
					char[] additionalFieldTypes = contentType.replace("Refset", "").toCharArray();
					String[] fieldNames = headerLine.split(RF2Constants.COLUMN_SEPARATOR);
					int fieldOffset = refset.getFields().size() - 1;

					for (char additionalFieldType : additionalFieldTypes) {
						fieldOffset++;
						DataType type;
						switch (additionalFieldType) {
							case REFSET_FILENAME_CONCEPT_FIELD:
								type = DataType.SCTID;
								break;
							case REFSET_FILENAME_INTEGER_FIELD:
								type = DataType.INTEGER;
								break;
							case REFSET_FILENAME_STRING_FIELD:
								type = DataType.STRING;
								break;
							default:
								throw new FileRecognitionException("Unexpected character '" + additionalFieldType + "' within content " +
										"type section of Refset filename.");
						}
						String fieldName = fieldNames[fieldOffset];
						refset.field(fieldName, type);
					}
					return refset;
				} else {
					throw new FileRecognitionException("Content type '" + contentType + "' is not supported.");
				}
			} else {
				throw new FileRecognitionException("File type '" + fileType + "' is not supported.");
			}
		} else {
			throw new FileRecognitionException("Unexpected filename format. Filename contains " + (nameParts.length - 1) +
					" underscores, expected 4. Filename: " + filename);
		}

	}

	private TableSchema createSimpleRefsetSchema(String filenameNoExtension) {
		return new TableSchema(filenameNoExtension)
								.field("id", DataType.UUID)
								.field("effectiveTime", DataType.TIME)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", DataType.SCTID)
								.field("refSetId", DataType.SCTID)
								.field("referencedComponentId", DataType.SCTID);
	}

}
