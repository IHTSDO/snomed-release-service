package org.ihtsdo.buildcloud.service.inputfile.prepare;

/**
 * User: huyle
 * Date: 5/29/2017
 * Time: 10:06 AM
 */
public class FileProcessingReportDetail {

    private ReportType type;

    private String fileName;

    private String refsetId;

    private String source;

    private String message;



    public FileProcessingReportDetail(){}

    public FileProcessingReportDetail(ReportType type, String message) {
        this.type = type;
        this.message = message;
    }

    public FileProcessingReportDetail(ReportType type, String fileName, String refsetId, String source, String message) {
        this.type = type;
        this.fileName = fileName;
        this.refsetId = refsetId;
        this.source = source;
        this.message = message;

    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((refsetId == null) ? 0 : refsetId.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileProcessingReportDetail other = (FileProcessingReportDetail) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (refsetId == null) {
			if (other.refsetId != null)
				return false;
		} else if (!refsetId.equals(other.refsetId))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return type == other.getType();
	}

	public ReportType getType() {
        return type;
    }

    public void setType(ReportType type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRefsetId() {
        return refsetId;
    }

    public void setRefsetId(String refsetId) {
        this.refsetId = refsetId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
}
