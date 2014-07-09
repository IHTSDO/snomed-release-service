package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;

import java.util.ArrayList;
import java.util.List;

public class TableSchema {

	private String filenameNoExtension;
	private TableType tableType;
	private List<Field> fields;

	public TableSchema(TableType tableType, String filenameNoExtension) {
		this.tableType = tableType;
		this.filenameNoExtension = filenameNoExtension;

		fields = new ArrayList<>();
	}

	public TableSchema field(String name, DataType type) {
		fields.add(new Field(name, type));
		return this;
	}

	public TableType getTableType() {
		return tableType;
	}

	public String getTableName() {
		return filenameNoExtension.replace("-", "");
	}

	public String getFilename() {
		return filenameNoExtension + RF2Constants.TXT_FILE_EXTENSION;
	}

	public List<Field> getFields() {
		return fields;
	}

	public static class Field {

		private String name;
		private DataType type;

		private Field(String name, DataType type) {
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public DataType getType() {
			return type;
		}

	}

}
