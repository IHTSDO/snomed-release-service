package org.ihtsdo.buildcloud.service.maven;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class MavenGenerator {

	private final DefaultMustacheFactory mustacheFactory;
	private final Mustache artifactPomMustache;
	private final Mustache projectPomMustache;
	private final Mustache commonGroupIdMustache;
	private final Mustache inputFileArtifactIdMustache;

	public MavenGenerator() {
		mustacheFactory = new DefaultMustacheFactory();
		projectPomMustache = mustacheFactory.compile("project-pom.mustache");
		artifactPomMustache = mustacheFactory.compile("artifact-pom.mustache");
		commonGroupIdMustache = mustacheFactory.compile("common-group-id.mustache");
		inputFileArtifactIdMustache = mustacheFactory.compile("input-file-artifact-id.mustache");
	}

	public void generateArtifactPom(Writer writer, MavenArtifact artifact) throws IOException {
		artifactPomMustache.execute(writer, artifact).flush();
	}

	public void generateBuildPoms(Writer writer, Build build) throws IOException {
		projectPomMustache.execute(writer, build).flush();
	}

	public MavenArtifact getArtifact(InputFile inputFile) throws IOException {
		Package aPackage = inputFile.getPackage();
		Build build = aPackage.getBuild();

		StringWriter groupIdWriter = new StringWriter();
		commonGroupIdMustache.execute(groupIdWriter, build).flush();
		String groupId = groupIdWriter.toString();

		StringWriter artifactIdWriter = new StringWriter();
		inputFileArtifactIdMustache.execute(artifactIdWriter, inputFile).flush();
		String artifactId = artifactIdWriter.toString();

		String version = "1.0";
		String packaging = "zip";
		return new MavenArtifact(groupId, artifactId, version, packaging);
	}

	public String getPath(MavenArtifact mavenArtifact) {
		return getPath(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(), mavenArtifact.getVersion(), mavenArtifact.getPackaging());
	}

	public String getPomPath(MavenArtifact mavenArtifact) {
		return getPath(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(), mavenArtifact.getVersion(), "pom");
	}

	private String getPath(String groupId, String artifactId, String version, String packaging) {
		String groupIdWithSlashes = withSlashes(groupId);
		String artifactIdWithSlashes = withSlashes(artifactId);
		return String.format("%s/%s/%s/%s-%3$s.%s", groupIdWithSlashes, artifactIdWithSlashes, version, artifactId, packaging);
	}

	private String withSlashes(String name) {
		return name.replace(".", "/");
	}
}
