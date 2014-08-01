package org.ihtsdo.buildcloud.service.execution.transform;
import java.util.List;

import org.ihtsdo.buildcloud.service.execution.database.ShortFormatSCTIDPartitionIdentifier;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

public class TransformationFactory {

	private final String effectiveTimeInSnomedFormat;
	private final CachedSctidFactory cachedSctidFactory;
	private final StreamingFileTransformation conceptTransformation;
	private final StreamingFileTransformation descriptionTransformation;
	private final StreamingFileTransformation textDefinitionFileTransformation;
	private final StreamingFileTransformation relationshipFileTransformation;
	private final StreamingFileTransformation identifierFileTransformation;
	private final StreamingFileTransformation preProcessConceptFileTransformation;
	private final StreamingFileTransformation preProcessDescriptionFileTransformation;
	private final UUIDGenerator uuidGenerator;

	public TransformationFactory(String effectiveTimeInSnomedFormat, CachedSctidFactory cachedSctidFactory, UUIDGenerator uuidGeneratorX) {
		this.effectiveTimeInSnomedFormat = effectiveTimeInSnomedFormat;
		this.cachedSctidFactory = cachedSctidFactory;

		preProcessConceptFileTransformation = buildPreProcessConceptFileTransformation();
		preProcessDescriptionFileTransformation = buildPreProcessDescriptionFileTransformation();
		conceptTransformation = buildConceptFileTransformation();
		descriptionTransformation = buildDescriptionFileTransformation();
		textDefinitionFileTransformation = buildTextDefinitionFileTransformation();
		relationshipFileTransformation = buildRelationshipFileTransformation();
		identifierFileTransformation = buildIdentifierFileTransformation();
		uuidGenerator = uuidGeneratorX;
	}

	public StreamingFileTransformation getPreProcessFileTransformation(ComponentType componentType) {
		if (componentType == ComponentType.CONCEPT) {
			return preProcessConceptFileTransformation;
		} else if (componentType == ComponentType.DESCRIPTION) {
			return preProcessDescriptionFileTransformation;
		} else {
			return null;
		}
	}

	public StreamingFileTransformation getSteamingFileTransformation(TableSchema tableSchema) throws FileRecognitionException {
		StreamingFileTransformation transformation;

		switch (tableSchema.getComponentType()) {
			case CONCEPT:
				transformation = conceptTransformation;
				break;
			case DESCRIPTION:
				transformation = descriptionTransformation;
				break;
			case TEXT_DEFINITION:
				transformation = textDefinitionFileTransformation;
				break;
			case STATED_RELATIONSHIP:
				transformation = relationshipFileTransformation;
				break;
			case RELATIONSHIP:
				transformation = relationshipFileTransformation;
				break;
			case IDENTIFIER:
				transformation = identifierFileTransformation;
				break;
			case REFSET:
				transformation = createRefsetTransformation(tableSchema);
				break;
			default:
				throw new FileRecognitionException("No transformation available for table type " + tableSchema);
		}

		return transformation;
	}

	private StreamingFileTransformation buildPreProcessConceptFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_cpt
		return new StreamingFileTransformation()
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory));
	}

	private StreamingFileTransformation buildPreProcessDescriptionFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_cpt
		return new StreamingFileTransformation()
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.DESCRIPTION, cachedSctidFactory));
	}

	private StreamingFileTransformation buildConceptFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_cpt
		return new StreamingFileTransformation()
				// id transform already done
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, true))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// definitionStatusId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory));
	}

	private StreamingFileTransformation buildDescriptionFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_des
		return new StreamingFileTransformation()
				// id transform already done
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, true))
						// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
						// conceptId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
						// typeId
				.addLineTransformation(new SCTIDTransformationFromCache(6, cachedSctidFactory))
						// caseSignificanceId
				.addLineTransformation(new SCTIDTransformationFromCache(8, cachedSctidFactory))
				;

	}

	private StreamingFileTransformation buildTextDefinitionFileTransformation() {
		return new StreamingFileTransformation()
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.DESCRIPTION, cachedSctidFactory))
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, true))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// conceptId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// typeId
				.addLineTransformation(new SCTIDTransformationFromCache(6, cachedSctidFactory))
				// caseSignificanceId
				.addLineTransformation(new SCTIDTransformationFromCache(8, cachedSctidFactory))
				;

	}

	private StreamingFileTransformation buildRelationshipFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_rel
		return new StreamingFileTransformation()
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.RELATIONSHIP, cachedSctidFactory))
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, true))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// sourceId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// destinationId
				.addLineTransformation(new SCTIDTransformationFromCache(5, cachedSctidFactory))
				// typeId
				.addLineTransformation(new SCTIDTransformationFromCache(7, cachedSctidFactory))
				// characteristicTypeId
				.addLineTransformation(new SCTIDTransformationFromCache(8, cachedSctidFactory))
				// modifierId
				.addLineTransformation(new SCTIDTransformationFromCache(9, cachedSctidFactory))
				;
	}

	private StreamingFileTransformation buildIdentifierFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_idfile
		return new StreamingFileTransformation()
				// identifierSchemeId
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(2, effectiveTimeInSnomedFormat, true))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// referencedComponentId
				.addLineTransformation(new SCTIDTransformationFromCache(5, cachedSctidFactory));
	}

	private StreamingFileTransformation createRefsetTransformation(TableSchema tableSchema) {
		StreamingFileTransformation transformation = createSimpleRefsetTransformation();

		// Add any additional transformations for extended refsets.
		List<Field> fields = tableSchema.getFields();
		for (int i = SchemaFactory.SIMPLE_REFSET_FIELD_COUNT; i < fields.size(); i++) {
			Field field = fields.get(i);
			if(field.getType().equals(DataType.SCTID)) {
				transformation.addLineTransformation(new SCTIDTransformationFromCache(i, cachedSctidFactory));
			}
		}
		return transformation;
	}

	private StreamingFileTransformation createSimpleRefsetTransformation() {
		// TIG - www.snomed.org/tig?t=trg2rfs_spec_simple_struct
		return new StreamingFileTransformation()
				// id
				.addLineTransformation(new UUIDTransformation(0, uuidGenerator))
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, true))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// refsetId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// referencedComponentId
				.addLineTransformation(new SCTIDTransformationFromCache(5, cachedSctidFactory));
	}

}
