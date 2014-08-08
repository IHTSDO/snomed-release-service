package org.ihtsdo.buildcloud.service.execution.database.map;

public interface Key<T> extends Comparable<T> {

	String getIdString();

	String getDate();

}
