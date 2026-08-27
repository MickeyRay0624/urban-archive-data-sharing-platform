package com.collaborative.sharing.entity;

import java.time.LocalDateTime;

public class DataElementSubtypeConfig {
    private Long id;
    private String dataType;
    private String dataSubtype;
    private String tableName;
    private Integer fieldCount;
    private Boolean builtin;
    private Boolean visible;
    private Integer sortOrder;
    private String selfCheckStatus;
    private String securityCheckStatus;
    private String complianceResult;
    private LocalDateTime selfCheckTime;
    private LocalDateTime securityCheckTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DataElementSubtypeConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getDataSubtype() { return dataSubtype; }
    public void setDataSubtype(String dataSubtype) { this.dataSubtype = dataSubtype; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public Integer getFieldCount() { return fieldCount; }
    public void setFieldCount(Integer fieldCount) { this.fieldCount = fieldCount; }

    public Boolean getBuiltin() { return builtin; }
    public void setBuiltin(Boolean builtin) { this.builtin = builtin; }

    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getSelfCheckStatus() { return selfCheckStatus; }
    public void setSelfCheckStatus(String selfCheckStatus) { this.selfCheckStatus = selfCheckStatus; }

    public String getSecurityCheckStatus() { return securityCheckStatus; }
    public void setSecurityCheckStatus(String securityCheckStatus) { this.securityCheckStatus = securityCheckStatus; }

    public String getComplianceResult() { return complianceResult; }
    public void setComplianceResult(String complianceResult) { this.complianceResult = complianceResult; }

    public LocalDateTime getSelfCheckTime() { return selfCheckTime; }
    public void setSelfCheckTime(LocalDateTime selfCheckTime) { this.selfCheckTime = selfCheckTime; }

    public LocalDateTime getSecurityCheckTime() { return securityCheckTime; }
    public void setSecurityCheckTime(LocalDateTime securityCheckTime) { this.securityCheckTime = securityCheckTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
