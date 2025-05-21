package org.ihtsdo.buildcloud.core.service.build;

import org.ihtsdo.buildcloud.core.service.helper.Rf2FileNameTransformation;

import java.io.File;
import java.text.Normalizer;
import java.util.Objects;

public class FileUtils {

    private FileUtils() {
    }

    public static File getRf2FileFromDirectory(File extractedDirectory, String fileName) {
        Rf2FileNameTransformation rf2FileNameTransformation = new Rf2FileNameTransformation();
        String targetFileNameStripped = rf2FileNameTransformation.transformFilename(fileName);
        if (!Normalizer.isNormalized(targetFileNameStripped, Normalizer.Form.NFC)) {
            targetFileNameStripped = Normalizer.normalize(targetFileNameStripped, Normalizer.Form.NFC);
        }
        for (File file : Objects.requireNonNull(extractedDirectory.listFiles())) {
            if (Normalizer.normalize(file.getName(), Normalizer.Form.NFC).contains(targetFileNameStripped)) {
                return file;
            }
        }
        return null;
    }
}
