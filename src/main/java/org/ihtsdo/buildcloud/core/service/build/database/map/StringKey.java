package org.ihtsdo.buildcloud.core.service.build.database.map;

public class StringKey implements Key {

	private final String compKey;

	private String effectiveTime;

	public StringKey(String compKey) {
		this.compKey = compKey;
		this.effectiveTime = null;
	}

	public StringKey(String compKey, String effectiveTime) {
		this(compKey);
		this.effectiveTime = effectiveTime;
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
		return this.effectiveTime;
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
