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

    @Autowired
    SRSFileDAO srsFileDAO;


    public File extractAndConvertExportWithRF2FileNameFormat(File archive, File manifestFile, String releaseCenter, String releaseDate) throws ProcessWorkflowException, IOException, JAXBException, ResourceNotFoundException {
        // We're going to create release files in a temp directory
        File extractDir = Files.createTempDir();
        srsFileDAO.unzipFlat(archive, extractDir);
        logger.debug("Unzipped files to {}", extractDir.getAbsolutePath());
        
        

        return extractDir;
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

    public void unzip(File archive, File targetDir) throws IOException, ProcessWorkflowException {
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            throw new ProcessWorkflowException(targetDir + " is not a viable directory in which to extract archive");
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(archive));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = targetDir.getAbsolutePath() + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();

    }

    private void extractFile(ZipInputStream zis, String filePath) throws IOException {
        File extractedFile = new File(filePath);
        OutputStream out = new FileOutputStream(extractedFile);
        IOUtils.copy(zis, out);
        IOUtils.closeQuietly(out);
    }



}
