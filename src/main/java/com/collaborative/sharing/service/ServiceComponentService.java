package com.collaborative.sharing.service;

import com.collaborative.sharing.entity.ServiceComponent;
import com.collaborative.sharing.mapper.ServiceComponentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServiceComponentService {

    @Autowired
    private ServiceComponentMapper serviceComponentMapper;

    public List<ServiceComponent> findAll() {
        return serviceComponentMapper.findAll();
    }

    public ServiceComponent findById(Long id) {
        return serviceComponentMapper.findById(id);
    }

    public void create(ServiceComponent component) {
        LocalDateTime now = LocalDateTime.now();
        if (component.getStatus() == null) {
            component.setStatus(ServiceComponent.ServiceStatus.DRAFT);
        }
        component.setCreatedAt(now);
        component.setUpdatedAt(now);
        serviceComponentMapper.insert(component);
    }

    public void update(ServiceComponent component) {
        ServiceComponent existing = serviceComponentMapper.findById(component.getId());
        if (existing != null) {
            component.setStatus(existing.getStatus());
            component.setCreatedAt(existing.getCreatedAt());
            component.setUpdatedAt(LocalDateTime.now());
            serviceComponentMapper.update(component);
        }
    }

    public void delete(Long id) {
        serviceComponentMapper.deleteById(id);
    }

    public void updateStatus(Long id, String status) {
        serviceComponentMapper.updateStatus(id, status);
    }

    public void updateAuthorization(Long id, String authorizedUnits) {
        serviceComponentMapper.updateAuthorization(id, authorizedUnits);
    }

    public void updateConfig(Long id, String configJson) {
        serviceComponentMapper.updateConfig(id, configJson);
    }

    public void updateComplianceStatus(Long id, String complianceStatus) {
        serviceComponentMapper.updateComplianceStatus(id, complianceStatus);
    }

    public void submitComplianceReview(Long id, String complianceStatus,
                                       String complianceApplyUnit, String compliancePurpose,
                                       String complianceDataScope, String complianceRemark,
                                       LocalDateTime complianceSubmitTime) {
        serviceComponentMapper.submitComplianceReview(id, complianceStatus, complianceApplyUnit,
                compliancePurpose, complianceDataScope, complianceRemark, complianceSubmitTime);
    }

    public void updateComplianceResult(Long id, String complianceStatus, String complianceResult) {
        serviceComponentMapper.updateComplianceResult(id, complianceStatus, complianceResult);
    }
}
