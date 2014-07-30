package org.ihtsdo.buildcloud.service.execution.database.map;

public class SCTIDKey implements Key<SCTIDKey> {

	private final Long id;
	private final String effectiveTime;

	public SCTIDKey(String id, String effectiveTime) {
		this.id = Long.parseLong(id);
		this.effectiveTime = effectiveTime;
	}

	@Override
	public int compareTo(SCTIDKey other) {
		int result = id.compareTo(other.id);
		if (result == 0) {
			result = effectiveTime.compareTo(other.effectiveTime);
		}
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
