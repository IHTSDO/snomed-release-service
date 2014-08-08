package org.ihtsdo.buildcloud.service.execution.database.map;

import java.util.UUID;

public class UUIDKey implements Key<UUIDKey> {

	private UUID uuid;
	private String date;

	public UUIDKey(String uuidString, String date) {
		this.uuid = UUID.fromString(uuidString);
		this.date = date;
	}

	public int compareTo(UUIDKey other) {
		int result = uuid.compareTo(other.uuid);
		if (result == 0) {
			result = date.compareTo(other.date);
		}
		return result;
	}

	public String getIdString() {
		return uuid.toString();
	}

	public String getDate() {
		return date;
	}

}
