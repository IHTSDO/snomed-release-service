package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.ParseException;

public interface RF2TableDAO {

	TableSchema createTable(String rf2FilePath, InputStream rf2InputStream) throws SQLException, IOException, FileRecognitionException, ParseException, DatabasePopulatorException;

	void appendData(TableSchema tableSchema, InputStream rf2InputStream) throws IOException, SQLException, ParseException;

	RF2TableResults selectAllOrdered(TableSchema tableSchema) throws SQLException;

	RF2TableResults selectNone(TableSchema tableSchema) throws SQLException;

	void closeConnection() throws SQLException;

}
