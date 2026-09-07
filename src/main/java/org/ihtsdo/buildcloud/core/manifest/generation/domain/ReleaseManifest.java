package org.ihtsdo.buildcloud.core.manifest.generation.domain;

import com.fasterxml.jackson.annotation.JsonRootName;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName(namespace = "http://release.ihtsdo.org/manifest/1.0.0", value = "listing")
public class ReleaseManifest {

	@JacksonXmlProperty(localName = "folder")
	private ReleaseManifestFolder rootFolder;

	public ReleaseManifest(ReleaseManifestFolder rootFolder) {
		this.rootFolder = rootFolder;
	}

	public ReleaseManifestFolder getRootFolder() {
		return rootFolder;
	}
}
