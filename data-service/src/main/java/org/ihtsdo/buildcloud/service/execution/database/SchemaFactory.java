package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SchemaFactory {

	public static final char REFSET_FILENAME_CONCEPT_FIELD = 'c';
	public static final char REFSET_FILENAME_INTEGER_FIELD = 'i';
	public static final char REFSET_FILENAME_STRING_FIELD = 's';
	public static final int SIMPLE_REFSET_FIELD_COUNT = 6;

	private static final String NO_MATCH_PREFIX = "Input file not RF2, ";
	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaFactory.class);

	public TableSchema createSchemaBean(String filename) throws FileRecognitionException {
		// General File Naming Pattern
		// <FileType>_<ContentType>_<ContentSubType>_<Country|Namespace>_<VersionDate>.<Extension>
		// See http://www.snomed.org/tig?t=fng2_convention

		if (filename.endsWith(".txt")) {

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

						// Use the contentType prefix characters for datatypes of additional fields without field names at this point.
						char[] additionalFieldTypes = contentType.replace("Refset", "").toCharArray();

						for (char additionalFieldType : additionalFieldTypes) {
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
							refset.field(null, type);
						}
						return refset;
					} else {
						LOGGER.info(NO_MATCH_PREFIX + "file type {} with Content type {} is not supported.", fileType, contentType);
					}
				} else if (fileType.equals("sct2") || fileType.equals("xsct2")) {
					if (contentType.equals("Concept")) {
						return new TableSchema(TableType.CONCEPT, filenameNoExtension)
								.field("id", DataType.SCTID)
								.field("effectiveTime", DataType.TIME)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", DataType.SCTID)
								.field("definitionStatusId", DataType.SCTID);

					} else if (contentType.equals("Description")) {
						return new TableSchema(TableType.DESCRIPTION, filenameNoExtension)
								.field("id", DataType.SCTID)
								.field("effectiveTime", DataType.TIME)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", DataType.SCTID)
								.field("conceptId", DataType.SCTID)
								.field("languageCode", DataType.STRING)
								.field("typeId", DataType.SCTID)
								.field("term", DataType.STRING)
								.field("caseSignificanceId", DataType.SCTID);

					} else if (contentType.equals("StatedRelationship") || contentType.equals("Relationship")) {
						TableType tableType = contentType.equals("StatedRelationship") ? TableType.STATED_RELATIONSHIP : TableType.RELATIONSHIP;
						return new TableSchema(tableType, filenameNoExtension)
								.field("id", DataType.SCTID)
								.field("effectiveTime", DataType.TIME)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", DataType.SCTID)
								.field("sourceId", DataType.SCTID)
								.field("destinationId", DataType.SCTID)
								.field("relationshipGroup", DataType.INTEGER)
								.field("typeId", DataType.SCTID)
								.field("characteristicTypeId", DataType.SCTID)
								.field("modifierId", DataType.SCTID);

					} else if (contentType.equals("Identifier")) {
						return new TableSchema(TableType.IDENTIFIER, filenameNoExtension)
								.field("identifierSchemeId", DataType.SCTID)
								.field("alternateIdentifier", DataType.STRING)
								.field("effectiveTime", DataType.TIME)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", DataType.SCTID)
								.field("referencedComponentId", DataType.SCTID);

					} else {
						LOGGER.info(NO_MATCH_PREFIX + "file type {} with Content type {} is not supported.", fileType, contentType);
					}
				} else {
					LOGGER.info(NO_MATCH_PREFIX + "file type {} is not supported.", fileType);
				}
			} else {
				LOGGER.info(NO_MATCH_PREFIX + "unexpected filename format. Filename contains {} underscores, expected 4. Filename: {}", (nameParts.length - 1), filename);
			}
		} else {
			LOGGER.info(NO_MATCH_PREFIX + "incorrect file extension: {}", filename);
		}
		return null;
	}

	public void populateExtendedRefsetAdditionalFieldNames(TableSchema schema, String headerLine) {
		String[] fieldNames = headerLine.split(RF2Constants.COLUMN_SEPARATOR);
		List<TableSchema.Field> fields = schema.getFields();
		for (int i = 0; i < fields.size(); i++) {
			TableSchema.Field field = fields.get(i);
			if (field.getName() == null) {
				field.setName(fieldNames[i]);
			}
		}
	}

	private TableSchema createSimpleRefsetSchema(String filenameNoExtension) {
		return new TableSchema(TableType.REFSET, filenameNoExtension)
								.field("id", DataType.UUID)
								.field("effectiveTime", DataType.TIME)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", DataType.SCTID)
								.field("refSetId", DataType.SCTID)
								.field("referencedComponentId", DataType.SCTID);
	}

}
