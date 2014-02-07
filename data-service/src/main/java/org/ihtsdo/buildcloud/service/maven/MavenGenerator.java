package org.ihtsdo.buildcloud.service.maven;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;

import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;

public class MavenGenerator {

	private final DefaultMustacheFactory mustacheFactory;
	private final Mustache artifactPomMustache;
	private final Mustache buildPomMustache;
	private final Mustache packagePomMustache;	
	private final Mustache commonGroupIdMustache;
	private final Mustache inputFileArtifactIdMustache;

	public MavenGenerator() {
		mustacheFactory = new DefaultMustacheFactory();
		buildPomMustache = mustacheFactory.compile("build-pom.mustache");
		packagePomMustache = mustacheFactory.compile("package-pom.mustache");
		artifactPomMustache = mustacheFactory.compile("artifact-pom.mustache");
		commonGroupIdMustache = mustacheFactory.compile("common-group-id.mustache");
		inputFileArtifactIdMustache = mustacheFactory.compile("input-file-artifact-id.mustache");
	}

	public void generateArtifactPom(Writer writer, MavenArtifact artifact) throws IOException {
		artifactPomMustache.execute(writer, artifact).flush();
	}

	public void generateBuildPoms(Writer writer, Build build) throws IOException {
		buildPomMustache.execute(writer, build).flush();
	}
	
	/*
	 * @return the directory in which the parent pom has been generated
	 */
	public File generateBuildPoms(Build build) throws IOException {
		//Create a randomly named temp directory to hold our parent build pom 
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		String buildDir	= Files.createTempDirectory(tempDir.toPath(),"srs_").toFile().getPath();
		File parentPom = new File(buildDir + File.separator + "pom.xml");
		
		//Write the pom to it's newly minted location, filling in the build-pom.mustache template
		FileWriter fw = new FileWriter(parentPom);
		buildPomMustache.execute(fw, build).flush();
		
		//Now work through the child packages creating directory and poms for each one.
		for (Package pkg : build.getPackages()) {
			generatePackagePom(buildDir, pkg);
		}
		return parentPom;
	}
	
	private void generatePackagePom(String buildDir, Package pkg)  throws IOException {
		//Create a sub directory based on the businessKey of the package
		String packageDir = buildDir + File.separator + pkg.getBusinessKey();
		boolean packageDirOK = new File(packageDir).mkdirs();
		if (!packageDirOK) {
			throw new IOException ("Unable to create package directory " + packageDir);
		}
		File packagePom = new File(packageDir + File.separator + "pom.xml");
		
		//Write the pom to it's newly minted location, filling in the package-pom.mustache template		
		FileWriter fw = new FileWriter(packagePom);
		packagePomMustache.execute(fw, pkg).flush();
		
		//We also need a copy of assembly.xml 
		File newAssembly = new File (packageDir + File.separator + "assembly.xml");
		InputStream is = this.getClass().getResourceAsStream("/package_assembly.xml");
		if (is == null) {
			throw new IOException ("Unable to read required resource package_assembly.xml");
		}
		OutputStream os = new FileOutputStream(newAssembly);
		FileCopyUtils.copy(is, os);

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
