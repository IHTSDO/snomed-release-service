package org.ihtsdo.buildcloud.service.build.database.map;

import java.util.UUID;

public class UUIDKey implements Key {

	private final UUID uuid;
	private final String date;

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
			if (result == 0 && date != null) {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
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
		UUIDKey other = (UUIDKey) obj;
		if (date == null) {
			if (other.date != null) {
				return false;
			}
		} else if (!date.equals(other.date)) {
			return false;
		}
		if (uuid == null) {
			return other.uuid == null;
		} else return uuid.equals(other.uuid);
	}
}
