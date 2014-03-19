package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.MavenArtifact;

public class MavenArtifactHelper {

	public static String getPath(MavenArtifact artifact) {
		return getPath(artifact, artifact.getPackaging());
	}

	public static String getPath(MavenArtifact artifact, String packaging) {
		String groupIdWithSlashes = withSlashes(artifact.getGroupId());
		String artifactId = artifact.getArtifactId();
		return String.format("%s/%s/%s/%s-%3$s.%s", groupIdWithSlashes, artifactId, artifact.getVersion(),
				artifactId, packaging);
	}

	private static String withSlashes(String name) {
		return name.replace(".", "/");
	}

}
