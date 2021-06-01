package org.ihtsdo.buildcloud.core.service.build;

public class RF2BuildUtils {

	
	/**Release package name is updated e.g SnomedCT_InternationalRF2_Production_20170131T120000.zip
	 * instead of SnomedCT_Release_INT_20170131.zip
	 * @param dependencyReleasePackage
	 * @return
	 */
	public  static String getReleaseDateFromReleasePackage(String dependencyReleasePackage) {
		if (dependencyReleasePackage != null && dependencyReleasePackage.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
			String [] splits = dependencyReleasePackage.split(RF2Constants.FILE_NAME_SEPARATOR);
			String releaseDate = splits[splits.length - 1];
			if (releaseDate.length() > 8) {
				releaseDate = releaseDate.substring(0, 8);
			}
			return releaseDate;
		}
		return null;
	}
}
