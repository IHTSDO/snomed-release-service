package com.termmed.genid.data;

import java.io.Serializable;

public class SctIdIdentifier implements Serializable {

	private static final long serialVersionUID = 3649745602471285098L;

	private String partitionId;
	private String namespaceId;
    private String artifactId;
    private String releaseId;
    private Long itemId;
    private String sctId;
    private String code;
    
    public SctIdIdentifier(){
    	super();
    }

	public SctIdIdentifier(String partitionId, String namespaceId, String artifactId, String releaseId, Long itemId, String sctId, String code) {
		super();
		this.partitionId = partitionId;
		this.namespaceId = namespaceId;
		this.artifactId = artifactId;
		this.releaseId = releaseId;
		this.itemId = itemId;
		this.sctId = sctId;
		this.code = code;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getReleaseId() {
		return releaseId;
	}

	public void setReleaseId(String releaseId) {
		this.releaseId = releaseId;
	}

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public String getSctId() {
		return sctId;
	}

	public void setSctId(String sctId) {
		this.sctId = sctId;
	}

	public String getPartitionId() {
		return partitionId;
	}

	public void setPartitionId(String partitionId) {
		this.partitionId = partitionId;
	}

	public String getNamespaceId() {
		return namespaceId;
	}

	public void setNamespaceId(String namespaceId) {
		this.namespaceId = namespaceId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return "SctIdIdentifier [partitionId=" + partitionId + ", namespaceId=" + namespaceId + ", artifactId=" + artifactId + ", releaseId=" + releaseId + ", itemId=" + itemId + ", sctId="
				+ sctId + ", code=" + code + "]";
	}
	
	

}
