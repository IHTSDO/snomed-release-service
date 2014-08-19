package org.ihtsdo.buildcloud.service.execution.database.map;

import java.util.UUID;

public class UUIDKey implements Key {

	private UUID uuid;
	private String date;

	public UUIDKey(String uuidString, String date) {
		this.uuid = UUID.fromString(uuidString);
		this.date = date;
	}

	public int compareTo(Key other) {
		int result;
		if (other instanceof StringKey) {
			result = 1;
		} else {
			UUIDKey otherUUIDKey = (UUIDKey) other;
			result = uuid.compareTo(otherUUIDKey.uuid);
			if (result == 0) {
				result = date.compareTo(otherUUIDKey.date);
			}
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
