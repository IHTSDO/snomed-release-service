package org.ihtsdo.buildcloud.core.manifest.generation.domain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class ReleaseRefset {

	@JacksonXmlProperty(isAttribute = true)
	private final String id;

	@JacksonXmlProperty(isAttribute = true)
	private final String label;

	public ReleaseRefset(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}
}
