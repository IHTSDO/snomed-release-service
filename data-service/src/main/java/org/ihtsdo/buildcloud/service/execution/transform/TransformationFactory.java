package org.ihtsdo.buildcloud.service.execution.transform;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import org.ihtsdo.buildcloud.service.execution.database.ShortFormatSCTIDPartitionIdentifier;
import org.ihtsdo.buildcloud.service.execution.transform.conditional.ConditionalTransformation;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

public class TransformationFactory {

	private final String effectiveTimeInSnomedFormat;
	private final CachedSctidFactory cachedSctidFactory;
	private final UUIDGenerator uuidGenerator;
	private final String coreModuleSctid;
	private final String modelModuleSctid;
	private final Integer transformBufferSize;
	private Set<String> modelConceptIdsForModuleIdFix;

	public TransformationFactory(final String effectiveTimeInSnomedFormat, final CachedSctidFactory cachedSctidFactory, final UUIDGenerator uuidGenerator,
			final String coreModuleSctid, final String modelModuleSctid, final Integer transformBufferSize) {
		this.effectiveTimeInSnomedFormat = effectiveTimeInSnomedFormat;
		this.cachedSctidFactory = cachedSctidFactory;
		this.coreModuleSctid = coreModuleSctid;
		this.modelModuleSctid = modelModuleSctid;
		this.uuidGenerator = uuidGenerator;
		this.transformBufferSize = transformBufferSize;
	}

	public StreamingFileTransformation getPreProcessFileTransformation(final ComponentType componentType) {
		if (componentType == ComponentType.CONCEPT) {
			return getPreProcessConceptFileTransformation();
		} else if (componentType == ComponentType.DESCRIPTION) {
			return getPreProcessDescriptionFileTransformation();
		} else {
			return null;
		}
	}

	public StreamingFileTransformation getSteamingFileTransformation(final TableSchema tableSchema) throws FileRecognitionException, NoSuchAlgorithmException {
		StreamingFileTransformation transformation;

		switch (tableSchema.getComponentType()) {
			case CONCEPT:
				transformation = getConceptFileTransformation();
				break;
			case DESCRIPTION:
				transformation = getDescriptionFileTransformation();
				break;
			case TEXT_DEFINITION:
				transformation = getTextDefinitionFileTransformation();
				break;
			case STATED_RELATIONSHIP:
				transformation = getStatedRelationshipFileTransformation();
				break;
			case RELATIONSHIP:
				transformation = getRelationshipFileTransformation();
				break;
			case IDENTIFIER:
				transformation = getIdentifierFileTransformation();
				break;
			case REFSET:
				transformation = createRefsetTransformation(tableSchema);
				break;
			default:
				throw new FileRecognitionException("No transformation available for table type " + tableSchema);
		}

		return transformation;
	}

	private StreamingFileTransformation getPreProcessConceptFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_cpt
		final StreamingFileTransformation streamingFileTransformation = newStreamingFileTransformation()
				// id
				.addTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If id is a model concept and active set moduleId to modelModuleSctid, otherwise set moduleId to coreModuleSctid
			final ConditionalTransformation conditionalTransformationForConceptFile = new ConditionalTransformation()
					.addIf().columnValueInCollection(0, modelConceptIdsForModuleIdFix)
					.and().columnValueEquals(2, "1")
					.then(new ReplaceValueLineTransformation(3, modelModuleSctid))
					.otherwise(new ReplaceValueLineTransformation(3, coreModuleSctid));

			streamingFileTransformation.addTransformationToFrontOfList(conditionalTransformationForConceptFile);
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation getPreProcessDescriptionFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_cpt
		final StreamingFileTransformation streamingFileTransformation = newStreamingFileTransformation()
				// id
				.addTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.DESCRIPTION, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If conceptId is a model concept and active set moduleId to modelModuleSctid, otherwise set moduleId to coreModuleSctid
			final ConditionalTransformation conditionalTransformationForDescriptionFile = new ConditionalTransformation()
					.addIf().columnValueInCollection(4, modelConceptIdsForModuleIdFix)
					.and().columnValueEquals(2, "1")
					.then(new ReplaceValueLineTransformation(3, modelModuleSctid))
					.otherwise(new ReplaceValueLineTransformation(3, coreModuleSctid));

			streamingFileTransformation.addTransformationToFrontOfList(conditionalTransformationForDescriptionFile);
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation getConceptFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_cpt
		return newStreamingFileTransformation()
				// id transform already done
				// effectiveTime
				.addTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// definitionStatusId
				.addTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory));
	}

	private StreamingFileTransformation getDescriptionFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_des
		return newStreamingFileTransformation()
				// id transform already done
				// effectiveTime
				.addTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// conceptId
				.addTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// typeId
				.addTransformation(new SCTIDTransformationFromCache(6, cachedSctidFactory))
				// caseSignificanceId
				.addTransformation(new SCTIDTransformationFromCache(8, cachedSctidFactory));
	}

	private StreamingFileTransformation getTextDefinitionFileTransformation() {
		final StreamingFileTransformation streamingFileTransformation = newStreamingFileTransformation()
				// id
				.addTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.DESCRIPTION, cachedSctidFactory))
				// effectiveTime
				.addTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// conceptId
				.addTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// typeId
				.addTransformation(new SCTIDTransformationFromCache(6, cachedSctidFactory))
				// caseSignificanceId
				.addTransformation(new SCTIDTransformationFromCache(8, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If conceptId is a model concept and active set moduleId to modelModuleSctid, otherwise set moduleId to coreModuleSctid
			final ConditionalTransformation conditionalTransformationForTextDefinitionFile = new ConditionalTransformation()
					.addIf().columnValueInCollection(4, modelConceptIdsForModuleIdFix)
					.and().columnValueEquals(2, "1")
					.then(new ReplaceValueLineTransformation(3, modelModuleSctid))
					.otherwise(new ReplaceValueLineTransformation(3, coreModuleSctid));

			streamingFileTransformation.addTransformationToFrontOfList(conditionalTransformationForTextDefinitionFile);
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation getStatedRelationshipFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_rel
		final StreamingFileTransformation streamingFileTransformation = newStreamingFileTransformation()
				// id
				.addTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.RELATIONSHIP, cachedSctidFactory))
				// effectiveTime
				.addTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// sourceId
				.addTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// destinationId
				.addTransformation(new SCTIDTransformationFromCache(5, cachedSctidFactory))
				// typeId
				.addTransformation(new SCTIDTransformationFromCache(7, cachedSctidFactory))
				// characteristicTypeId
				.addTransformation(new SCTIDTransformationFromCache(8, cachedSctidFactory))
				// modifierId
				.addTransformation(new SCTIDTransformationFromCache(9, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If destinationId is a model concept and active set moduleId to modelModuleSctid, otherwise set moduleId to coreModuleSctid
			final ConditionalTransformation conditionalTransformationForRelationshipFile = new ConditionalTransformation()
					.addIf().columnValueInCollection(5, modelConceptIdsForModuleIdFix)
					.and().columnValueEquals(2, "1")
					.then(new ReplaceValueLineTransformation(3, modelModuleSctid))
					.otherwise(new ReplaceValueLineTransformation(3, coreModuleSctid));

			streamingFileTransformation.addTransformationToFrontOfList(conditionalTransformationForRelationshipFile);
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation getRelationshipFileTransformation() throws NoSuchAlgorithmException {
		// TIG - www.snomed.org/tig?t=trg2main_format_rel
		final StreamingFileTransformation streamingFileTransformation = newStreamingFileTransformation()
				// id
				.addTransformation(new RepeatableRelationshipUUIDTransform())
				.addTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.RELATIONSHIP, cachedSctidFactory));

		return streamingFileTransformation;
	}

	private StreamingFileTransformation getIdentifierFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_idfile
		final StreamingFileTransformation streamingFileTransformation = newStreamingFileTransformation()
				// identifierSchemeId
				.addTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// effectiveTime
				.addTransformation(new ReplaceValueLineTransformation(2, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// referencedComponentId
				.addTransformation(new SCTIDTransformationFromCache(5, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If referencedComponentId is a model concept and active set moduleId to modelModuleSctid, otherwise set moduleId to coreModuleSctid
			final ConditionalTransformation conditionalTransformationForIdentifierFile = new ConditionalTransformation()
					.addIf().columnValueInCollection(5, modelConceptIdsForModuleIdFix)
					.and().columnValueEquals(3, "1")
					.then(new ReplaceValueLineTransformation(4, modelModuleSctid))
					.otherwise(new ReplaceValueLineTransformation(4, coreModuleSctid));

			streamingFileTransformation.addTransformationToFrontOfList(conditionalTransformationForIdentifierFile);
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation createRefsetTransformation(final TableSchema tableSchema) {
		final StreamingFileTransformation transformation = createSimpleRefsetTransformation();

		// Add any additional transformations for extended refsets.
		final List<Field> fields = tableSchema.getFields();
		for (int i = SchemaFactory.SIMPLE_REFSET_FIELD_COUNT; i < fields.size(); i++) {
			final Field field = fields.get(i);
			if (field.getType().equals(DataType.SCTID) || field.getType().equals(DataType.SCTID_OR_UUID)) {
				transformation.addTransformation(new SCTIDTransformationFromCache(i, cachedSctidFactory));
			}
		}
		return transformation;
	}

	private StreamingFileTransformation createSimpleRefsetTransformation() {
		// TIG - www.snomed.org/tig?t=trg2rfs_spec_simple_struct
		final StreamingFileTransformation streamingFileTransformation = newStreamingFileTransformation()
				// id
				.addTransformation(new UUIDTransformation(0, uuidGenerator))
				// effectiveTime
				.addTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// refsetId
				.addTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// referencedComponentId
				.addTransformation(new SCTIDTransformationFromCache(5, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If referencedComponentId is a model concept and active set moduleId to modelModuleSctid, otherwise set moduleId to coreModuleSctid
			final ConditionalTransformation conditionalTransformationForRefsetFile = new ConditionalTransformation()
					.addIf().columnValueInCollection(5, modelConceptIdsForModuleIdFix)
					.and().columnValueEquals(2, "1")
					.then(new ReplaceValueLineTransformation(3, modelModuleSctid))
					.otherwise(new ReplaceValueLineTransformation(3, coreModuleSctid));

			streamingFileTransformation.addTransformationToFrontOfList(conditionalTransformationForRefsetFile);
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation newStreamingFileTransformation() {
		return new StreamingFileTransformation(transformBufferSize);
	}

	public void setModelConceptIdsForModuleIdFix(final Set<String> modelConceptIdsForModuleIdFix) {
		this.modelConceptIdsForModuleIdFix = modelConceptIdsForModuleIdFix;
	}

	public CachedSctidFactory getCachedSctidFactory() {
		return cachedSctidFactory;
	}
}
