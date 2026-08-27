package com.collaborative.sharing.mapper;

import com.collaborative.sharing.entity.ServiceComponent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ServiceComponentMapper {
    
    List<ServiceComponent> findAll();
    
    ServiceComponent findById(@Param("id") Long id);
    
    int insert(ServiceComponent serviceComponent);
    
    int update(ServiceComponent serviceComponent);
    
    int deleteById(@Param("id") Long id);
    
    List<ServiceComponent> findByStatus(@Param("status") String status);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateAuthorization(@Param("id") Long id, @Param("authorizedUnits") String authorizedUnits);

    int updateConfig(@Param("id") Long id, @Param("configJson") String configJson);

    int updateComplianceStatus(@Param("id") Long id, @Param("complianceStatus") String complianceStatus);

    int submitComplianceReview(@Param("id") Long id,
                               @Param("complianceStatus") String complianceStatus,
                               @Param("complianceApplyUnit") String complianceApplyUnit,
                               @Param("compliancePurpose") String compliancePurpose,
                               @Param("complianceDataScope") String complianceDataScope,
                               @Param("complianceRemark") String complianceRemark,
                               @Param("complianceSubmitTime") LocalDateTime complianceSubmitTime);

    int updateComplianceResult(@Param("id") Long id,
                                @Param("complianceStatus") String complianceStatus,
                                @Param("complianceResult") String complianceResult);
}
