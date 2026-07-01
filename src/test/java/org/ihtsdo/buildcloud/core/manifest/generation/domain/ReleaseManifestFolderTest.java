package org.ihtsdo.buildcloud.core.manifest.generation.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseManifestFolderTest {

	@Test
	void removeFilesMatching_shouldExcludeFilesContainingConfiguredFragment() {
		ReleaseManifestFolder root = new ReleaseManifestFolder("root");
		ReleaseManifestFolder snapshot = root.getOrAddFolder("Snapshot");
		ReleaseManifestFolder terminology = snapshot.getOrAddFolder("Terminology");
		terminology.addFile(new ReleaseManifestFile("sct2_Concept_Snapshot_INT_20240131.txt"));
		terminology.addFile(new ReleaseManifestFile("sct2_StatedRelationship_Snapshot_INT_20240131.txt"));
		terminology.addFile(new ReleaseManifestFile("sct2_Relationship_Snapshot_INT_20240131.txt"));

		ReleaseManifestFolder full = root.getOrAddFolder("Full");
		ReleaseManifestFolder fullTerminology = full.getOrAddFolder("Terminology");
		fullTerminology.addFile(new ReleaseManifestFile("sct2_StatedRelationship_Full_INT_20240131.txt"));

		root.removeFilesMatching(List.of("sct2_StatedRelationship"));

		assertEquals(2, terminology.getFile().size());
		assertTrue(terminology.getFile().stream().noneMatch(f -> f.getName().contains("StatedRelationship")));
		assertTrue(fullTerminology.getFile().isEmpty());
	}

	@Test
	void removeFilesMatching_shouldSupportMultiplePipeDelimitedPatterns() {
		ReleaseManifestFolder root = new ReleaseManifestFolder("root");
		ReleaseManifestFolder snapshot = root.getOrAddFolder("Snapshot");
		snapshot.addFile(new ReleaseManifestFile("sct2_StatedRelationship_Snapshot_INT_20240131.txt"));
		snapshot.addFile(new ReleaseManifestFile("der2_Refset_SimpleSnapshot_INT_20240131.txt"));
		snapshot.addFile(new ReleaseManifestFile("sct2_Concept_Snapshot_INT_20240131.txt"));

		root.removeFilesMatching(List.of("sct2_StatedRelationship", "der2_Refset_Simple"));

		assertEquals(1, snapshot.getFile().size());
		assertEquals("sct2_Concept_Snapshot_INT_20240131.txt", snapshot.getFile().get(0).getName());
	}

	@Test
	void removeFilesMatching_shouldMatchBetaPrefixedFilenames() {
		ReleaseManifestFolder root = new ReleaseManifestFolder("root");
		root.addFile(new ReleaseManifestFile("xsct2_StatedRelationship_Snapshot_INT_20240131.txt"));

		root.removeFilesMatching(List.of("sct2_StatedRelationship"));

		assertTrue(root.getFile().isEmpty());
	}

}
