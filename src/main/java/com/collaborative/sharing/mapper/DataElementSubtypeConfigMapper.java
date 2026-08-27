package com.collaborative.sharing.mapper;

import com.collaborative.sharing.entity.DataElementSubtypeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DataElementSubtypeConfigMapper {

    List<DataElementSubtypeConfig> findAll();

    List<DataElementSubtypeConfig> findVisible();

    List<DataElementSubtypeConfig> findVisibleByType(@Param("dataType") String dataType);

    DataElementSubtypeConfig findById(@Param("id") Long id);

    DataElementSubtypeConfig findBySubtype(@Param("dataSubtype") String dataSubtype);

    DataElementSubtypeConfig findByTypeAndSubtype(@Param("dataType") String dataType,
                                                  @Param("dataSubtype") String dataSubtype);

    int insert(DataElementSubtypeConfig config);

    int update(DataElementSubtypeConfig config);

    int deleteById(@Param("id") Long id);

    int hideById(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    int updateSelfCheckStatus(@Param("dataSubtype") String dataSubtype,
                              @Param("selfCheckStatus") String selfCheckStatus,
                              @Param("complianceResult") String complianceResult,
                              @Param("selfCheckTime") LocalDateTime selfCheckTime,
                              @Param("updatedAt") LocalDateTime updatedAt);

    int updateSelfCheckStatusByType(@Param("dataType") String dataType,
                                    @Param("dataSubtype") String dataSubtype,
                                    @Param("selfCheckStatus") String selfCheckStatus,
                                    @Param("complianceResult") String complianceResult,
                                    @Param("selfCheckTime") LocalDateTime selfCheckTime,
                                    @Param("updatedAt") LocalDateTime updatedAt);

    int updateSecurityCheckStatus(@Param("dataSubtype") String dataSubtype,
                                  @Param("securityCheckStatus") String securityCheckStatus,
                                  @Param("complianceResult") String complianceResult,
                                  @Param("securityCheckTime") LocalDateTime securityCheckTime,
                                  @Param("updatedAt") LocalDateTime updatedAt);

    int updateSecurityCheckStatusByType(@Param("dataType") String dataType,
                                        @Param("dataSubtype") String dataSubtype,
                                        @Param("securityCheckStatus") String securityCheckStatus,
                                        @Param("complianceResult") String complianceResult,
                                        @Param("securityCheckTime") LocalDateTime securityCheckTime,
                                        @Param("updatedAt") LocalDateTime updatedAt);

    int resetComplianceStatusForSubtypes(@Param("subtypes") List<String> subtypes,
                                         @Param("complianceResult") String complianceResult,
                                         @Param("updatedAt") LocalDateTime updatedAt);

    int resetComplianceStatusForSubtypesByType(@Param("dataType") String dataType,
                                               @Param("subtypes") List<String> subtypes,
                                               @Param("complianceResult") String complianceResult,
                                               @Param("updatedAt") LocalDateTime updatedAt);

    Integer findMaxSortOrder();

    Integer findMaxSortOrderByType(@Param("dataType") String dataType);
}
