package org.ihtsdo.buildcloud.core.manifest.generation.domain;

import java.util.Objects;

public class ReleaseFileSource {

	private final String source;

	public ReleaseFileSource(String source) {
		this.source = source;
	}

	public String getSource() {
		return source;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ReleaseFileSource that = (ReleaseFileSource) o;
		return Objects.equals(source, that.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source);
	}
}
