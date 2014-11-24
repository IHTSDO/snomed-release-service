package org.ihtsdo.buildcloud.service.build.database.map;

public interface Key extends Comparable<Key> {

	String getIdString();

	String getDate();

}
