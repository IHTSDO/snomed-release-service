package org.ihtsdo.buildcloud.service.build.readme;

import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

@Service
public class ReadmeGenerator {

	private static final String INDENTATION = "    ";

	public void generate(String readmeHeader, String readmeEndDate, ListingType manifestListing, OutputStream readmeOutputStream) throws IOException, BadConfigurationException {
		readmeEndDate = (readmeEndDate != null) ? readmeEndDate : "";
		if (readmeHeader == null) {
			throw new IllegalArgumentException("Readme header has not been supplied.  Unable to continue");
		}
		readmeHeader = readmeHeader.replace("{readmeEndDate}", readmeEndDate);
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(readmeOutputStream, RF2Constants.UTF_8))) {
			writer.write(readmeHeader);
			writer.write(RF2Constants.LINE_ENDING);
			FolderType rootFolder = manifestListing.getFolder();
			if (rootFolder != null) {
				writeFolder(rootFolder, 1, writer);
			}
		}
	}

	private void writeFolder(FolderType folder, int depth, BufferedWriter writer) throws IOException {
		// Write this folder name
		writeLine(folder.getName(), depth, writer);

		// Iterate child folders
		for (FolderType childFolder : folder.getFolder()) {
			writeFolder(childFolder, depth + 1, writer);
		}

		// Write out files in this directory
		for (FileType file : folder.getFile()) {
			writeLine(file.getName(), depth + 1, writer);
		}
	}

	private void writeLine(String line, int depth, BufferedWriter writer) throws IOException {
		for (int a = 0; a < depth; a++) {
			writer.write(INDENTATION);
		}
		if (line != null) {
			writer.write(line);
		}
		writer.write(RF2Constants.LINE_ENDING);
	}

}
