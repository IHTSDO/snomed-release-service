package org.ihtsdo.buildcloud.service.execution.database.map;

public interface Key extends Comparable<Key> {

	String getIdString();

	String getDate();

}
