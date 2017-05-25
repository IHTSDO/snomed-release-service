package org.ihtsdo.buildcloud.service.srs;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * User: huyle
 * Date: 5/24/2017
 * Time: 10:56 AM
 */
public class FileProcessor {

    private final Logger logger = LoggerFactory.getLogger(FileProcessor.class);
    private final String FILE_EXTENSION_TXT = ".txt";
    private static final String HEADER_REFSETS = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\ttargetComponent";
    private static final String HEADER_TERM_DESCRIPTION = "id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId";
    private static final int REFSETID_COL = 4;
    private static final int DESCRIPTION_COL = 5;
    private static final String INPUT_FILE_TYPE_REFSET = "Refset";
    private static final String INPUT_FILE_TYPE_DESCRIPTION = "Description";

    static Map<String, InputFileConfig> fileTypesHeaders;
    static {
        fileTypesHeaders = new HashMap<>();
        fileTypesHeaders.put(INPUT_FILE_TYPE_REFSET, new InputFileConfig(INPUT_FILE_TYPE_REFSET, HEADER_REFSETS, REFSETID_COL));
        fileTypesHeaders.put(INPUT_FILE_TYPE_DESCRIPTION, new InputFileConfig(INPUT_FILE_TYPE_DESCRIPTION, HEADER_TERM_DESCRIPTION, DESCRIPTION_COL));
    }


    private void processFiles(File extractDir, String sourceFileName, String matchingValue, String targetFileName, String fileType) throws IOException {
        if(!sourceFileName.equalsIgnoreCase(targetFileName) &&sourceFileName.endsWith(FILE_EXTENSION_TXT)) {
            File sourceFile = new File(extractDir, sourceFileName);
            List<String> lines = FileUtils.readLines(sourceFile, CharEncoding.UTF_8);
            if(lines.get(0).equalsIgnoreCase(fileTypesHeaders.get(fileType).getHeader())) {
                File targetFile = new File(extractDir, targetFileName);
                List<String> copiedLines = new ArrayList<>();
                if(!targetFile.exists()) {
                    boolean createFile = targetFile.createNewFile();
                    if(createFile) {
                        logger.info("Create new {} file {}", fileType, targetFile.getAbsolutePath());
                    } else {
                        logger.error("Failed to create new refset file {}", targetFileName);
                    }
                    copiedLines.add(lines.get(0));
                }
                for (String line : lines) {
                    String[] splits = line.split("\t");
                    if(matchingValue.equals(splits[fileTypesHeaders.get(fileType).getProcessedColumn()])) copiedLines.add(line);
                }
                FileUtils.writeLines(targetFile, copiedLines, CharEncoding.UTF_8);
            }
        }
    }
    



}
