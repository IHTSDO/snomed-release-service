package org.ihtsdo.buildcloud.core.service.build.database.map;

import java.util.Objects;

public class SCTIDKey implements Key {

	private final Long id;
	private final String effectiveTime;

	public SCTIDKey(String id, String effectiveTime) throws NumberFormatException {
		this.id = Long.parseLong(id);
		this.effectiveTime = effectiveTime;
	}

	@Override
	public int compareTo(Key otherKey) {
		SCTIDKey other = (SCTIDKey) otherKey;
		int result = id.compareTo(other.id);
		if (result == 0) {
			result = effectiveTime.compareTo(other.effectiveTime);
		}
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SCTIDKey sctidKey = (SCTIDKey) o;

		if (!Objects.equals(effectiveTime, sctidKey.effectiveTime)) {
			return false;
		}
		return Objects.equals(id, sctidKey.id);
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (effectiveTime != null ? effectiveTime.hashCode() : 0);
		return result;
	}

	@Override
	public String getIdString() {
		return id.toString();
	}

	@Override
	public String getDate() {
		return effectiveTime;
	}

}
