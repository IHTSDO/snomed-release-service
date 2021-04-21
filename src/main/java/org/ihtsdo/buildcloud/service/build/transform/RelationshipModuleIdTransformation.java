package org.ihtsdo.buildcloud.service.build.transform;

import java.util.Map;


/**Note:
 	Regarding overrides component model concept module (JIRA ISRS-113)
	"The way the 2 modules were setup in the first place was that the Core was dependent on the Component Model module.  
	 This meant that you could distribute the Component Model module standalone, but NOT the Core on its own.
	 Therefore all of the components of this Concept (which is the Component Model module concept itself) reside correctly in the Component Model module. 
	 However, the Relationship CANNOT reside inside the Component Model module, as if this module is then distributed standalone 
	 the relationship would be a broken one,as the concept it relates to will be inside the Core and not part of the Component Model module.
 */
public class RelationshipModuleIdTransformation implements LineTransformation  {
	private final int moduleIdColumn = 3;
	private final int sourceConceptColumn = 4;
	private final Map<String, String> conceptToModuleIdMap;

	public RelationshipModuleIdTransformation(Map<String, String> conceptToModuleIdMap) {
		this.conceptToModuleIdMap = conceptToModuleIdMap;
		//override model component SCTID to use core instead of 900000000000012004
		conceptToModuleIdMap.put("900000000000441003", "900000000000207008");
	}
	
	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		String source = columnValues[sourceConceptColumn];
		if (conceptToModuleIdMap.containsKey(source)) {
			columnValues[moduleIdColumn] = conceptToModuleIdMap.get(source);
		}
	}

	@Override
	public int getColumnIndex() {
		return moduleIdColumn;
	}
}
