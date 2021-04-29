package org.ihtsdo.buildcloud.service.inputfile.prepare;

public enum ReportType implements Comparable<ReportType> {

    INFO("info",3), WARNING("warning",2), ERROR("error",1);
	
	private final String name;
	private final int order;
	
	ReportType(String name, int order) {
		this.name = name;
		this.order = order;
	}

	public String getName() {
		return name;
	}

	public int getOrder() {
		return order;
	}
}
