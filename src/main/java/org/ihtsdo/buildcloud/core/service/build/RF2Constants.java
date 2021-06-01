package org.ihtsdo.buildcloud.core.service.build;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.time.FastDateFormat;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;

public class RF2Constants {

	public static final String COLUMN_SEPARATOR = "\t";
	public static final Charset UTF_8 = StandardCharsets.UTF_8;
	public static final String LINE_ENDING = "\r\n";
	public static final String TXT_FILE_EXTENSION = ".txt";
	public static final String DELTA = "Delta";
	public static final String FULL = "Full";
	public static final String SNAPSHOT = "Snapshot";
	public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyyMMdd");
	public static final String EFFECTIVE_TIME = "effectiveTime";
	public static final String MANIFEST_CONTEXT_PATH = "org.ihtsdo.buildcloud.core.manifest";
	public static final String FILE_NAME_SEPARATOR = "_";
	public static final String INPUT_FILE_PREFIX = "rel2";
	public static final String DER2 = "der2";
	public static final String SCT2 = "sct2";
	public static final String README_FILENAME_PREFIX = "Readme";
	public static final String README_FILENAME_EXTENSION = ".txt";
	public static final String RELEASE_INFORMATION_FILENAME_PREFIX = "release";
	public static final String RELEASE_INFORMATION_FILENAME_EXTENSION = ".json";
	public static final String ZIP_FILE_EXTENSION = ".zip";
	public static final String BOOLEAN_TRUE = "1";
	public static final String BOOLEAN_FALSE = "0";
	public static final CharSequence ATTRIBUTE_VALUE_FILE_IDENTIFIER = "AttributeValue";
	public static final String EMPTY_SPACE = "";
	public static final String DATA_PROBLEM = "Data Problem:";
	public static final String INFERRED_RELATIONSHIPS_TXT = "inferred_relationships.txt";
	public static final String EQUIVALENCY_REPORT_TXT = "equivalency_report.txt";
	public static final String CONCEPTS_WITH_CYCLES_TXT = "concepts_with_cycles.txt";
	public static final String STATED = "Stated";
	public static final String NULL_STRING = "null";
	public static final CharSequence SIMPLE_MAP_FILE_IDENTIFIER = "SimpleMap";
	public static final CharSequence ASSOCIATION_REFERENCE_FILE_IDENTIFIER = "AssociationReference";
	public static final CharSequence EXTENEDED_MAP_FILE_IDENTIFIER = "ExtendedMap";
	public static final CharSequence REFERENCE_SET_DESCRIPTOR_FILE_IDENTIFIER = "RefsetDescriptor";
	public static final CharSequence MODULE_DEPENDENCY_FILE_IDENTIFIER = "ModuleDependency";
	public static final CharSequence COMPLEX_MAP_FILE_IDENTIFIER = "ComplexMap";
	public static final String  RELASHIONSHIP_DELTA_PREFIX = "sct2_Relationship_Delta_";
	public static final String CONCEPT_SNAPSHOT_PREFIX= "sct2_Concept_Snapshot_INT_";
	public static final String INT_STATED_RELATIONSHIP_SNAPSHOT_PREFIX = "sct2_StatedRelationship_Snapshot_INT_";
	public static final String INT_RELATIONSHIP_SNAPSHOT_PREFIX = "sct2_Relationship_Snapshot_INT_";
	public static final int INTERNATIONAL_NAMESPACE_ID = 0;
	public static final String SNOMED_ID_REFSET_ID = "900000000000498005";
	public static final String CTV3_ID_REFSET_ID = "900000000000497000";
	public static final String BETA_RELEASE_PREFIX = "x";
	public static final String INTERNATIONAL_CORE_MODULE_ID ="900000000000207008";
	public static final String INTERNATIONAL_MODEL_COMPONENT_ID ="900000000000012004";
	public static final String IS_A = "116680003";
	public static final ReleaseCenter INT_RELEASE_CENTER = new ReleaseCenter("International Release Center", "International");
	public static final String INT = "INT";
	public static final String ADDITIONAL_TXT = "_additional.txt";
	public static final String SNOMEDCT = "SNOMEDCT";
	public static final String 	DAILY_BUILD_TIME_FORMAT = "yyyy-MM-dd-HHmmss";
	public static final String REFSET = "Refset";
	public static final String LANGUAGE = "Language";
	public static final String USERNAME = "username";
	public static final String RVF_FAILURE_EXPORT_MAX = "RvfFailureExportMax";
	public static final String RELEASE_CENTER_KEY = "releaseCenterKey";
	public static final String PRODUCT_KEY = "productKey";
	public static final String BUILD_ID = "buildId";
	public static final String PROPERTY_IS_REQUIRED = " property is required";
	public static final String MRCM_VALIDATION_FORM = "mrcmValidationForm";

	public enum RelationshipFileType {
		STATED, INFERRED
	}
}
