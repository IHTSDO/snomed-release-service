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
			if (result == 0 && effectiveTime != null) {
				result = effectiveTime.compareTo(other.getDate());
			}
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
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		StringKey other = (StringKey) obj;
		if (effectiveTime == null) {
			if (other.getDate() != null) {
				return false;
			}
		} else if (!effectiveTime.equals(other.getDate())) {
			return false;
		}
		if (compKey == null) {
			return other.getIdString() == null;
		} else return compKey.equals(other.getIdString());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((effectiveTime == null) ? 0 : effectiveTime.hashCode());
		result = prime * result + ((compKey == null) ? 0 : compKey.hashCode());
		return result;
	}

}
