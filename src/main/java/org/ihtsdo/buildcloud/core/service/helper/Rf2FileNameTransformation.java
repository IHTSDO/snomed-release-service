package org.ihtsdo.buildcloud.core.service.helper;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;

public class Rf2FileNameTransformation implements FileNameTransformation {

	// General File Naming Pattern
	// <FileType>_<ContentType>_<ContentSubType>_<Country|Namespace>_<VersionDate>.<Extension>
	// See http://www.snomed.org/tig?t=fng2_convention

	@Override
	public String transformFilename(String inputFileName) {
		//Strip out any 8 digit numbers starting with an underscore eg _20140131
		return inputFileName.replaceAll("_[0-9]{8}", "").replace(RF2Constants.TXT_FILE_EXTENSION, "");
	}

}
