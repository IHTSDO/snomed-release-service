package org.ihtsdo.buildcloud.service.fileprocessing;

import java.util.ArrayList;
import java.util.List;

/**
 * User: huyle
 * Date: 5/29/2017
 * Time: 10:06 AM
 */
public class FileProcessingReport {

    private List<FileProcessingReportDetail> details;

    public FileProcessingReport() {
        this.details = new ArrayList<>();
    }

    public List<FileProcessingReportDetail> getDetails() {
        return details;
    }

    public void add(FileProcessingReportDetail fileProcessingReportDetail) {
        this.details.add(fileProcessingReportDetail);
    }
}
