package org.ihtsdo.buildcloud.core.service.build.compare;

import com.github.difflib.text.DiffRow.Tag;

public class DiffRow {
	private Tag tag;
	private String oldLine;
	private String newLine;

	public DiffRow() {
	}

	public DiffRow(Tag tag, String oldLine, String newLine) {
		this.tag = tag;
		this.oldLine = oldLine;
		this.newLine = newLine;
	}

	public com.github.difflib.text.DiffRow.Tag getTag() {
		return tag;
	}

	public void setTag(com.github.difflib.text.DiffRow.Tag tag) {
		this.tag = tag;
	}

	public String getOldLine() {
		return oldLine;
	}

	public String getNewLine() {
		return newLine;
	}
}
