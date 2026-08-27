package com.collaborative.sharing.entity;

import java.time.LocalDateTime;

public class ServiceComponent {
    private Long id;
    private String serviceName;
    private String serviceCode;
    private String description;
    private String version;
    private String apiUrl;
    private String ownerDepartment;
    private String securityLevel;
    private String authorizedUnits;
    private String configJson;
    private String remark;
    private ServiceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 合规审查字段
    private String complianceStatus;
    private String complianceApplyUnit;
    private String compliancePurpose;
    private String complianceDataScope;
    private String complianceRemark;
    private LocalDateTime complianceSubmitTime;
    private String complianceResult;
    
    // 构造函数
    public ServiceComponent() {
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getOwnerDepartment() {
        return ownerDepartment;
    }

    public void setOwnerDepartment(String ownerDepartment) {
        this.ownerDepartment = ownerDepartment;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getAuthorizedUnits() {
        return authorizedUnits;
    }

    public void setAuthorizedUnits(String authorizedUnits) {
        this.authorizedUnits = authorizedUnits;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public String getComplianceApplyUnit() {
        return complianceApplyUnit;
    }

    public void setComplianceApplyUnit(String complianceApplyUnit) {
        this.complianceApplyUnit = complianceApplyUnit;
    }

    public String getCompliancePurpose() {
        return compliancePurpose;
    }

    public void setCompliancePurpose(String compliancePurpose) {
        this.compliancePurpose = compliancePurpose;
    }

    public String getComplianceDataScope() {
        return complianceDataScope;
    }

    public void setComplianceDataScope(String complianceDataScope) {
        this.complianceDataScope = complianceDataScope;
    }

    public String getComplianceRemark() {
        return complianceRemark;
    }

    public void setComplianceRemark(String complianceRemark) {
        this.complianceRemark = complianceRemark;
    }

    public LocalDateTime getComplianceSubmitTime() {
        return complianceSubmitTime;
    }

    public void setComplianceSubmitTime(LocalDateTime complianceSubmitTime) {
        this.complianceSubmitTime = complianceSubmitTime;
    }

    public String getComplianceResult() {
        return complianceResult;
    }

    public void setComplianceResult(String complianceResult) {
        this.complianceResult = complianceResult;
    }
    
    public enum ServiceStatus {
        DRAFT("草稿"),
        PENDING_APPROVAL("待审批"),
        PUBLISHED("已发布"),
        AUTHORIZED("已授权"),
        OFFLINE("已下架");
        
        private final String displayName;
        
        ServiceStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
