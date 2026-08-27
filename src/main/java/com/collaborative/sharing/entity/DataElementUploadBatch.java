package com.collaborative.sharing.entity;

import java.time.LocalDateTime;

public class DataElementUploadBatch {
    private Long id;
    private String dataType;
    private String batchName;
    private String originalFileName;
    private String storedFilePath;
    private String fileHash;
    private Long fileSize;
    private String uploader;
    private LocalDateTime uploadTime;
    private Integer totalRows;
    private Integer businessRecordCount;
    private String recognizedColumns;
    private String unrecognizedColumns;
    private String involvedSubtypes;
    private Long duplicateOfBatchId;
    private String importPolicy;    // skip, force, replace
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DataElementUploadBatch() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getStoredFilePath() { return storedFilePath; }
    public void setStoredFilePath(String storedFilePath) { this.storedFilePath = storedFilePath; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getUploader() { return uploader; }
    public void setUploader(String uploader) { this.uploader = uploader; }

    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }

    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }

    public Integer getBusinessRecordCount() { return businessRecordCount; }
    public void setBusinessRecordCount(Integer businessRecordCount) { this.businessRecordCount = businessRecordCount; }

    public String getRecognizedColumns() { return recognizedColumns; }
    public void setRecognizedColumns(String recognizedColumns) { this.recognizedColumns = recognizedColumns; }

    public String getUnrecognizedColumns() { return unrecognizedColumns; }
    public void setUnrecognizedColumns(String unrecognizedColumns) { this.unrecognizedColumns = unrecognizedColumns; }

    public String getInvolvedSubtypes() { return involvedSubtypes; }
    public void setInvolvedSubtypes(String involvedSubtypes) { this.involvedSubtypes = involvedSubtypes; }

    public Long getDuplicateOfBatchId() { return duplicateOfBatchId; }
    public void setDuplicateOfBatchId(Long duplicateOfBatchId) { this.duplicateOfBatchId = duplicateOfBatchId; }

    public String getImportPolicy() { return importPolicy; }
    public void setImportPolicy(String importPolicy) { this.importPolicy = importPolicy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
