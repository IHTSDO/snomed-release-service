package org.ihtsdo.telemetry.server;

import org.apache.commons.lang3.NotImplementedException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class StreamFactory {

	public static final String FILE = "file";

	public BufferedWriter createStreamWriter(String streamUri) throws IOException {
		String[] split = streamUri.split("://");
		String protocol = split[0];
		String path = split[1];
		if (FILE.equals(protocol)) {
			return new BufferedWriter(new FileWriter(path));
		} else {
			throw new NotImplementedException("Unrecognised stream URI protocol: " + protocol);
		}
	}
}
