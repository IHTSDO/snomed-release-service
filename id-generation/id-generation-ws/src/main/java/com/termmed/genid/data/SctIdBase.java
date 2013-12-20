package com.termmed.genid.data;

import java.io.Serializable;

public class SctIdBase implements Serializable {

	private static final long serialVersionUID = -7789907291971002041L;
	private String partitionNum;
	private Long value;
	private String namespace;

	public SctIdBase() {
		super();
	}

	public SctIdBase(String partitionNum, Long value, String namespace) {
		super();
		this.partitionNum = partitionNum;
		this.value = value;
		this.namespace = namespace;
	}

	public String getPartitionNum() {
		return partitionNum;
	}

	public void setPartitionNum(String partitionNum) {
		this.partitionNum = partitionNum;
	}

	public Long getValue() {
		return value;
	}

	public void setValue(Long value) {
		this.value = value;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public String toString() {
		return "SctIdBase [partitionNum=" + partitionNum + ", value=" + value + ", namespace=" + namespace + "]";
	}
	
}
