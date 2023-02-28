package org.ihtsdo.buildcloud.core.service.build.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParentSctIdFinder {
	
	private static final String IS_A_RELATIONSHIP_ID = "116680003";

	public static final Pattern IS_A_RELATIONSHIP_PATTERN = Pattern.compile("[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t([^\t]*)\t([^\t]*)\t[^\t]*\t" + IS_A_RELATIONSHIP_ID + "\t.*");

	public ParentSctIdFinder() {
		
	}
	
	public Map<Long, Long> getParentSctIdFromStatedRelationship(final InputStream input, final List<Long> sourceIds) throws TransformationException {
		final Map<Long, Long> result = new HashMap<>();
		final List<Long> sourceIdsToFind = new ArrayList<>(sourceIds);
		if (input != null) {
			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
				//find the line matches isA relationship
				String line = null;
				while (!sourceIdsToFind.isEmpty() && (line = reader.readLine()) != null) {
					final Matcher matcher = IS_A_RELATIONSHIP_PATTERN.matcher(line);
					if (matcher.matches()) {
						final String sourceId = matcher.group(1);
						if (sourceIdsToFind.contains(Long.valueOf(sourceId))) {
							result.put(Long.valueOf(sourceId), Long.valueOf(matcher.group(2)));
							sourceIdsToFind.remove(sourceId);
						}
					}
				}
			} catch (final IOException e) {
				throw new TransformationException("Failed to read from inputsteam", e);
			}
		}
		return result;
	}
}
