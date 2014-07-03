package org.ihtsdo.buildcloud.dto;

public class ExecutionPackageDTO {

	private final String id;
	private final String name;

	public ExecutionPackageDTO(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

}
