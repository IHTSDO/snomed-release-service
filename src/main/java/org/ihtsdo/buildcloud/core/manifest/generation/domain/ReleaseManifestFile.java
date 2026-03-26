package org.ihtsdo.buildcloud.core.manifest.generation.domain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReleaseManifestFile {

	@JacksonXmlProperty(isAttribute = true, localName = "Name")
	private final String name;

	@JacksonXmlElementWrapper(useWrapping = false)
	private Set<ReleaseFileSource> sources;

	@JacksonXmlElementWrapper(localName = "contains-reference-sets")
	private Set<ReleaseRefset> refset;

	@JacksonXmlElementWrapper(localName = "contains-additional-fields")
	private List<ReleaseField> field;

	@JacksonXmlElementWrapper(localName = "contains-language-codes")
	private Set<String> code;

	public ReleaseManifestFile(String name) {
		this.name = name;
	}

	public ReleaseManifestFile copy(String name) {
		return new ReleaseManifestFile(name);
	}

	public void addSource(String source) {
		if (sources == null) {
			sources = new HashSet<>();
		}
		sources.add(new ReleaseFileSource(source));
	}

	public void addRefset(String conceptId, String term) {
		if (refset == null) {
			refset = new HashSet<>();
		}
		refset.add(new ReleaseRefset(conceptId, term));
	}

	public void addField(String name) {
		if (field == null) {
			field = new ArrayList<>();
		}
		field.add(new ReleaseField(name));
	}

	public void addLanguageCode(String languageCode) {
		if (code == null) {
			code = new HashSet<>();
		}
		code.add(languageCode);
	}

	public void clearSource() {
		sources = null;
	}

	public String getName() {
		return name;
	}

	public Set<ReleaseFileSource> getSources() {
		return sources;
	}

	public Set<ReleaseRefset> getRefset() {
		return refset;
	}

	public List<ReleaseField> getField() {
		return field;
	}

	public Set<String> getCode() {
		return code;
	}
}
