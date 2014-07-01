package org.ihtsdo.buildcloud.service.execution;


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

	public TransformationFactory(String effectiveTimeInSnomedFormat, CachedSctidFactory cachedSctidFactory) {
		this.effectiveTimeInSnomedFormat = effectiveTimeInSnomedFormat;
		this.cachedSctidFactory = cachedSctidFactory;

		refsetTransformation = buildRefsetFileTransformation();
		conceptTransformation = buildConceptFileTransformation();
		descriptionTransformation = buildDescriptionFileTransformation();
		relationshipFileTransformation = buildRelationshipFileTransformation();
		identifierFileTransformation = buildIdentifierFileTransformation();
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
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat))
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// moduleId
				.addLineTransformation(new SCTIDTransformation(3, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// definitionStatusId
				.addLineTransformation(new SCTIDTransformation(4, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory));
	}

	private StreamingFileTransformation buildDescriptionFileTransformation() {
		// TIG - www.snomed.org/tig?t=trg2main_format_des
		return new StreamingFileTransformation()
				// effectiveTime
				.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveTimeInSnomedFormat))
				// id
				.addLineTransformation(new SCTIDTransformation(0, 3, ShortFormatSCTIDPartitionIdentifier.DESCRIPTION, cachedSctidFactory))
				// moduleId
				.addLineTransformation(new SCTIDTransformation(3, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// conceptId
				.addLineTransformation(new SCTIDTransformation(4, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// typeId
				.addLineTransformation(new SCTIDTransformation(6, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// caseSignificanceId
				.addLineTransformation(new SCTIDTransformation(8, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
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
				.addLineTransformation(new SCTIDTransformation(3, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// sourceId
				.addLineTransformation(new SCTIDTransformation(4, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// destinationId
				.addLineTransformation(new SCTIDTransformation(5, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// typeId
				.addLineTransformation(new SCTIDTransformation(7, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// characteristicTypeId
				.addLineTransformation(new SCTIDTransformation(8, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// modifierId
				.addLineTransformation(new SCTIDTransformation(9, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
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
				.addLineTransformation(new SCTIDTransformation(4, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory))
				// referencedComponentId
				.addLineTransformation(new SCTIDTransformation(5, 3, ShortFormatSCTIDPartitionIdentifier.CONCEPT, cachedSctidFactory));
	}

}
