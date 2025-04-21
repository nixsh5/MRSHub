package com.example.mrshub;

public class Resource {
    private String fileName;
    private String description;
    private String downloadUrl;
    private String uploadedBy;
    private long uploadTimestamp;
    private String fileType;

    private String fileId;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }


    public Resource() {}

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public long getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(long uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
}
