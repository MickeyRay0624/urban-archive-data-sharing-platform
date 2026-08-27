package com.collaborative.sharing.mapper;

/**
 * 子类汇总统计信息
 */
public class SubtypeSummary {
    private String dataSubtype;
    private int recordCount;
    private String lastUploadTime;
    private String lastUploader;
    private String lastFileName;

    public String getDataSubtype() { return dataSubtype; }
    public void setDataSubtype(String dataSubtype) { this.dataSubtype = dataSubtype; }

    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }

    public String getLastUploadTime() { return lastUploadTime; }
    public void setLastUploadTime(String lastUploadTime) { this.lastUploadTime = lastUploadTime; }

    public String getLastUploader() { return lastUploader; }
    public void setLastUploader(String lastUploader) { this.lastUploader = lastUploader; }

    public String getLastFileName() { return lastFileName; }
    public void setLastFileName(String lastFileName) { this.lastFileName = lastFileName; }
}
