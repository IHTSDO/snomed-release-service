package org.ihtsdo.buildcloud.core.manifest.generation.domain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;

public class ReleaseManifestFolder {

	@JacksonXmlProperty(isAttribute = true, localName = "Name")
	private String name;

	@JacksonXmlElementWrapper(useWrapping = false)
	private List<ReleaseManifestFile> file;

	@JacksonXmlElementWrapper(useWrapping = false)
	private List<ReleaseManifestFolder> folder;

	public ReleaseManifestFolder(String name) {
		this.name = name;
		file = new ArrayList<>();
		folder = new ArrayList<>();
	}

	public void addFile(ReleaseManifestFile file) {
		this.file.add(file);
	}

	public ReleaseManifestFile getOrAddFile(String name) {
		for (ReleaseManifestFile existingFile : this.file) {
			if (existingFile.getName().equals(name)) {
				return existingFile;
			}
		}
		ReleaseManifestFile newFile = new ReleaseManifestFile(name);
		newFile.addSource("terminology-server");
		this.file.add(newFile);
		return newFile;
	}

	public ReleaseManifestFolder getOrAddFolder(String name) {
		for (ReleaseManifestFolder existingFolder : folder) {
			if (existingFolder.getName().equals(name)) {
				return existingFolder;
			}
		}
		ReleaseManifestFolder newFolder = new ReleaseManifestFolder(name);
		folder.add(newFolder);
		return newFolder;
	}

	public String getName() {
		return name;
	}

	public List<ReleaseManifestFile> getFile() {
		return file;
	}

	public List<ReleaseManifestFolder> getFolder() {
		return folder;
	}
}
