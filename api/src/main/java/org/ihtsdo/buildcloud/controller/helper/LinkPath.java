package org.ihtsdo.buildcloud.controller.helper;

/*
 * Wrapper class to allow every sub-entity to have it's own instance root
 */
public class LinkPath {
	
	private String link;
	
	private String instanceRoot;
	
	private String filter;

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getInstanceRoot() {
		return instanceRoot;
	}
	
	public boolean hasInstanceRoot() {
		return instanceRoot != null;
	}

	public void setInstanceRoot(String instanceRoot) {
		this.instanceRoot = instanceRoot;
	}
	
	public LinkPath(String link) {
		this(link, null, null);
	}

	public LinkPath(String link, String instanceRoot) {
		this(link, instanceRoot, null);
	}
	
	public LinkPath(String link, String instanceRoot, String filter) {
		this.link = link;
		this.instanceRoot = instanceRoot;
		this.filter = filter;
	}

	public boolean hasFilter() {
		return filter != null;
	}
	
	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}	

}
