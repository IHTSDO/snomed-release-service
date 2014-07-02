package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.buildcloud.service.execution.database.FileRecognitionException;
import org.ihtsdo.buildcloud.service.execution.database.ShortFormatSCTIDPartitionIdentifier;
import org.ihtsdo.buildcloud.service.execution.database.TableType;

public class TransformationFactory {

	private final String effectiveTimeInSnomedFormat;
	private final CachedSctidFactory cachedSctidFactory;

	private final StreamingFileTransformation refsetTransformation;
	private final StreamingFileTransformation conceptTransformation;
	private final StreamingFileTransformation descriptionTransformation;
	private final StreamingFileTransformation relationshipFileTransformation;
	private final StreamingFileTransformation identifierFileTransformation;
	private final StreamingFileTransformation preProcessConceptFileTransformation;

	public TransformationFactory(String effectiveTimeInSnomedFormat, CachedSctidFactory cachedSctidFactory) {
		this.effectiveTimeInSnomedFormat = effectiveTimeInSnomedFormat;
		this.cachedSctidFactory = cachedSctidFactory;

		preProcessConceptFileTransformation = buildPreProcessConceptFileTransformation();
		refsetTransformation = buildRefsetFileTransformation();
		conceptTransformation = buildConceptFileTransformation();
		descriptionTransformation = buildDescriptionFileTransformation();
		relationshipFileTransformation = buildRelationshipFileTransformation();
		identifierFileTransformation = buildIdentifierFileTransformation();
	}

	public StreamingFileTransformation getPreProcessConceptFileTransformation() {
		return preProcessConceptFileTransformation;
	}

	public StreamingFileTransformation getSteamingFileTransformation(TableType tableType) throws FileRecognitionException {
		StreamingFileTransformation transformation;

		switch (tableType) {
			case REFSET:
				transformation = refsetTransformation;
				break;
			case CONCEPT:
				transformation = conceptTransformation;
				break;
			case DESCRIPTION:
				transformation = descriptionTransformation;
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
			default:
				throw new FileRecognitionException("No transformation available for table type " + tableType);
		}

		return transformation;
	}

	private StreamingFileTransformation buildPreProcessConceptFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_cpt
		return new StreamingFileTransformation()
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory));

	}

	private StreamingFileTransformation buildRefsetFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2rfs_spec_simple_struct
		return new StreamingFileTransformation()
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat))
				// id
				.addLineTransformation(new UUIDTransformation(0));
	}

	private StreamingFileTransformation buildConceptFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_cpt
		return new StreamingFileTransformation()
				// id transform already done
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(3, cachedSctidFactory))
				// definitionStatusId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory));
	}

	private StreamingFileTransformation buildDescriptionFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_des
		return new StreamingFileTransformation()
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat))
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.DESCRIPTION, cachedSctidFactory))
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
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat))
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.RELATIONSHIP, cachedSctidFactory))
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
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(2, effectiveTimeInSnomedFormat))
				// identifierSchemeId
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// moduleId
				.addLineTransformation(new SCTIDTransformationFromCache(4, cachedSctidFactory))
				// referencedComponentId
				.addLineTransformation(new SCTIDTransformationFromCache(5, cachedSctidFactory));
	}

}
