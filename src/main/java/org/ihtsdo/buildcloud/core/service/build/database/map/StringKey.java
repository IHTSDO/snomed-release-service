package org.ihtsdo.buildcloud.core.service.build.database.map;

public class StringKey implements Key {

	private final String compKey;

	public StringKey(String compKey) {
		this.compKey = compKey;
	}

	@Override
	public int compareTo(Key other) {
		int result;
		if (other instanceof UUIDKey) {
			result = -1;
		} else {
			StringKey otherStringKey = (StringKey) other;
			result = compKey.compareTo(otherStringKey.compKey);
		}
		return result;
	}
	@Override
	public String getIdString() {
		return compKey;
	}

	@Override
	public String getDate() {
		return null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " \"" + compKey + "\"";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StringKey objStringKey) {
			return compKey.equals(objStringKey.compKey);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return compKey.hashCode();
	}

}
