package com.termmed.genid.data;

import java.io.Serializable;

public class SnomedIdRange implements Serializable {
	private static final long serialVersionUID = 5822517873220520699L;
	private String start;
	private String end;
	private Integer startLine;
	private Integer pageLenght;

	public Integer getStartLine() {
		return startLine;
	}

	public void setStartLine(Integer startLine) {
		this.startLine = startLine;
	}

	public Integer getPageLenght() {
		return pageLenght;
	}

	public void setPageLenght(Integer pageLenght) {
		this.pageLenght = pageLenght;
	}

	public SnomedIdRange(String startId, String endId) {
		this.start = startId;
		this.end =endId;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public String getEnd() {
		return end;
	}

	public void setEnd(String end) {
		this.end = end;
	}

	@Override
	public String toString() {
		return "SnomedIdRange [start=" + start + ", end=" + end + "]";
	}
	
	
}
