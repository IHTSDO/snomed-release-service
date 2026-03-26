package org.ihtsdo.buildcloud.core.entity.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Converts between List&lt;String&gt; and a single string for DB storage.
 */
public class CollectionConverter {

    public static final String DELIMITER = ",";

    private CollectionConverter () {
    }

    public static String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return String.join(DELIMITER, attribute);
    }

    public static List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(dbData.split(DELIMITER, -1)));
    }
}

