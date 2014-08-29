package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.buildcloud.service.execution.database.ShortFormatSCTIDPartitionIdentifier;
import org.ihtsdo.snomed.util.rf2.schema.*;

import java.util.List;
import java.util.Set;

public class TransformationFactory {

	private final String effectiveTimeInSnomedFormat;
	private final CachedSctidFactory cachedSctidFactory;
	private final UUIDGenerator uuidGenerator;
	private final String modelModuleSctid;
	private Set<String> modelConceptIdsForModuleIdFix;

	public TransformationFactory(String effectiveTimeInSnomedFormat, CachedSctidFactory cachedSctidFactory, UUIDGenerator uuidGenerator,
			String modelModuleSctid) {
		this.effectiveTimeInSnomedFormat = effectiveTimeInSnomedFormat;
		this.cachedSctidFactory = cachedSctidFactory;
		this.modelModuleSctid = modelModuleSctid;
		this.uuidGenerator = uuidGenerator;
	}

	public StreamingFileTransformation getPreProcessFileTransformation(ComponentType componentType) {
		if (componentType == ComponentType.CONCEPT) {
			return getPreProcessConceptFileTransformation();
		} else if (componentType == ComponentType.DESCRIPTION) {
			return getPreProcessDescriptionFileTransformation();
		} else {
			return null;
		}
	}

	public StreamingFileTransformation getSteamingFileTransformation(TableSchema tableSchema) throws FileRecognitionException {
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
				transformation = getRelationshipFileTransformation();
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
		StreamingFileTransformation streamingFileTransformation = new StreamingFileTransformation()
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If id is a model concept set moduleId to modelModuleSctid
			streamingFileTransformation.addLineTransformationToFrontOfList(new ConditionalReplaceTransformation(0, modelConceptIdsForModuleIdFix, 3, modelModuleSctid));
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation getPreProcessDescriptionFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_cpt
		StreamingFileTransformation streamingFileTransformation = new StreamingFileTransformation()
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.DESCRIPTION, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If conceptId is a model concept set moduleId to modelModuleSctid
			streamingFileTransformation.addLineTransformationToFrontOfList(new ConditionalReplaceTransformation(4, modelConceptIdsForModuleIdFix, 3, modelModuleSctid));
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation getConceptFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_cpt
		return new StreamingFileTransformation()
				// id transform already done
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// definitionStatusId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory));
	}

	private StreamingFileTransformation getDescriptionFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_des
		return new StreamingFileTransformation()
				// id transform already done
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// conceptId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// typeId
				.addLineTransformation(new SCTIDTransformationFromCache(6, cachedSctidFactory))
				// caseSignificanceId
				.addLineTransformation(new SCTIDTransformationFromCache(8, cachedSctidFactory));
	}

	private StreamingFileTransformation getTextDefinitionFileTransformation() {
		StreamingFileTransformation streamingFileTransformation = new StreamingFileTransformation()
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.DESCRIPTION, cachedSctidFactory))
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// conceptId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// typeId
				.addLineTransformation(new SCTIDTransformationFromCache(6, cachedSctidFactory))
				// caseSignificanceId
				.addLineTransformation(new SCTIDTransformationFromCache(8, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If conceptId is a model concept set moduleId to modelModuleSctid
			streamingFileTransformation.addLineTransformationToFrontOfList(new ConditionalReplaceTransformation(4, modelConceptIdsForModuleIdFix, 3, modelModuleSctid));
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation getRelationshipFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_rel
		StreamingFileTransformation streamingFileTransformation = new StreamingFileTransformation()
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.RELATIONSHIP, cachedSctidFactory))
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, false))
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
				.addLineTransformation(new SCTIDTransformationFromCache(9, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If destinationId is a model concept set moduleId to modelModuleSctid
			streamingFileTransformation.addLineTransformationToFrontOfList(new ConditionalReplaceTransformation(5, modelConceptIdsForModuleIdFix, 3, modelModuleSctid));
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation getIdentifierFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_idfile
		StreamingFileTransformation streamingFileTransformation = new StreamingFileTransformation()
				// identifierSchemeId
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(2, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// referencedComponentId
				.addLineTransformation(new SCTIDTransformationFromCache(5, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If referencedComponentId is a model concept set moduleId to modelModuleSctid
			streamingFileTransformation.addLineTransformationToFrontOfList(new ConditionalReplaceTransformation(5, modelConceptIdsForModuleIdFix, 4, modelModuleSctid));
		}

		return streamingFileTransformation;
	}

	private StreamingFileTransformation createRefsetTransformation(TableSchema tableSchema) {
		StreamingFileTransformation transformation = createSimpleRefsetTransformation();

		// Add any additional transformations for extended refsets.
		List<Field> fields = tableSchema.getFields();
		for (int i = SchemaFactory.SIMPLE_REFSET_FIELD_COUNT; i < fields.size(); i++) {
			Field field = fields.get(i);
			if (field.getType().equals(DataType.SCTID) || field.getType().equals(DataType.SCTID_OR_UUID)) {
				transformation.addLineTransformation(new SCTIDTransformationFromCache(i, cachedSctidFactory));
			}
		}
		return transformation;
	}

	private StreamingFileTransformation createSimpleRefsetTransformation() {
		// TIG - www.snomed.org/tig?t=trg2rfs_spec_simple_struct
		StreamingFileTransformation streamingFileTransformation = new StreamingFileTransformation()
				// id
				.addLineTransformation(new UUIDTransformation(0, uuidGenerator))
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat, false))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// refsetId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// referencedComponentId
				.addLineTransformation(new SCTIDTransformationFromCache(5, cachedSctidFactory));

		if (modelConceptIdsForModuleIdFix != null) {
			// If referencedComponentId is a model concept set moduleId to modelModuleSctid
			streamingFileTransformation.addLineTransformationToFrontOfList(new ConditionalReplaceTransformation(5, modelConceptIdsForModuleIdFix, 3, modelModuleSctid));
		}

		return streamingFileTransformation;
	}

	public void setModelConceptIdsForModuleIdFix(Set<String> modelConceptIdsForModuleIdFix) {
		this.modelConceptIdsForModuleIdFix = modelConceptIdsForModuleIdFix;
	}

}
