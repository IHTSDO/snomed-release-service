package org.ihtsdo.buildgeneration;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.ihtsdo.buildcloud.entity.Package;

import java.io.IOException;
import java.io.Writer;

public class BuildGenerator {

	private final DefaultMustacheFactory mustacheFactory;
	private Mustache projectMustache;

	public BuildGenerator() {
		mustacheFactory = new DefaultMustacheFactory();
		projectMustache = mustacheFactory.compile("project.mustache");
	}

	public void generate(Writer writer, Package aPackage) throws IOException {
		projectMustache.execute(writer, aPackage).flush();
	}

}
