package com.collaborative.sharing.entity;

import java.time.LocalDateTime;

public class DataElementBusinessRecord {
    private Long id;
    private Long batchId;
    private String dataType;       // 固定为"业务数据"
    private String dataSubtype;    // 9类之一
    private Integer rowIndex;
    private String projectId;      // 关联同一行拆分的记录
    private String displayName;    // 列表/预览标题
    private String recordJson;     // 识别后的字段和值 JSON
    private String extraJson;      // 未识别字段和值 JSON
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DataElementBusinessRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getDataSubtype() { return dataSubtype; }
    public void setDataSubtype(String dataSubtype) { this.dataSubtype = dataSubtype; }

    public Integer getRowIndex() { return rowIndex; }
    public void setRowIndex(Integer rowIndex) { this.rowIndex = rowIndex; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRecordJson() { return recordJson; }
    public void setRecordJson(String recordJson) { this.recordJson = recordJson; }

    public String getExtraJson() { return extraJson; }
    public void setExtraJson(String extraJson) { this.extraJson = extraJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
