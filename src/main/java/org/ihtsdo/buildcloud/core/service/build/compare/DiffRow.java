package org.ihtsdo.buildcloud.core.service.build.compare;

import com.github.difflib.text.DiffRow.Tag;

public class DiffRow {
	private String oldLine;
	private String newLine;

	public DiffRow() {
	}

	public DiffRow(String oldLine, String newLine) {
		this.oldLine = oldLine;
		this.newLine = newLine;
	}

	public void setNewLine(String newLine) {
		this.newLine = newLine;
	}

	public void setOldLine(String oldLine) {
		this.oldLine = oldLine;
	}

	public String getOldLine() {
		return oldLine;
	}

	public String getNewLine() {
		return newLine;
	}
}
