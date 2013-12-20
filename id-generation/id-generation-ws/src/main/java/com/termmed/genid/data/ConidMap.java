package com.termmed.genid.data;

import java.io.Serializable;

public class ConidMap implements Serializable {

	private static final long serialVersionUID = 7651409214407801482L;

	private String conceptId;
	private String ctv3Id;
	private String snomedId;
	private String code;
	private String gid;
	private String executionId;

	public ConidMap() {
		super();
	}	

	public ConidMap(String conceptId, String ctv3Id, String snomedId, String code, String gid, String executionId) {
		super();
		this.conceptId = conceptId;
		this.ctv3Id = ctv3Id;
		this.snomedId = snomedId;
		this.code = code;
		this.gid = gid;
		this.executionId = executionId;
	}

	public String getConceptId() {
		return conceptId;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	public String getCtv3Id() {
		return ctv3Id;
	}

	public void setCtv3Id(String ctv3Id) {
		this.ctv3Id = ctv3Id;
	}

	public String getSnomedId() {
		return snomedId;
	}

	public void setSnomedId(String snomedId) {
		this.snomedId = snomedId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getGid() {
		return gid;
	}

	public void setGid(String gid) {
		this.gid = gid;
	}

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	@Override
	public String toString() {
		return "ConidMap [conceptId=" + conceptId + ", ctv3Id=" + ctv3Id + ", snomedId=" + snomedId + ", code=" + code + ", gid=" + gid + ", executionId=" + executionId + "]";
	}

}
