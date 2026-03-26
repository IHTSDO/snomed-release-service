package org.ihtsdo.buildcloud.core.manifest.generation.domain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(namespace = "http://release.ihtsdo.org/manifest/1.0.0", localName = "listing")
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
