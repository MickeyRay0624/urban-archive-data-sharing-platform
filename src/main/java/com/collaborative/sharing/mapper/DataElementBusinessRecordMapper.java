package com.collaborative.sharing.mapper;

import com.collaborative.sharing.entity.DataElementBusinessRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataElementBusinessRecordMapper {

    int insert(DataElementBusinessRecord record);

    int batchInsert(@Param("list") List<DataElementBusinessRecord> list);

    DataElementBusinessRecord findById(@Param("id") Long id);

    List<DataElementBusinessRecord> findByBatchId(@Param("batchId") Long batchId);

    List<DataElementBusinessRecord> findByBatchIdAndDataType(@Param("batchId") Long batchId,
                                                             @Param("dataType") String dataType);

    List<DataElementBusinessRecord> findBySubtype(@Param("subtype") String subtype,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);

    List<DataElementBusinessRecord> findBySubtypeAndDataType(@Param("dataType") String dataType,
                                                             @Param("subtype") String subtype,
                                                             @Param("offset") int offset,
                                                             @Param("limit") int limit);

    int countBySubtype(@Param("subtype") String subtype);

    int countBySubtypeAndDataType(@Param("dataType") String dataType,
                                  @Param("subtype") String subtype);

    List<DataElementBusinessRecord> findBySubtypeAndKeyword(@Param("subtype") String subtype,
                                                            @Param("keyword") String keyword,
                                                            @Param("offset") int offset,
                                                            @Param("limit") int limit);

    List<DataElementBusinessRecord> findBySubtypeAndKeywordAndDataType(@Param("dataType") String dataType,
                                                                       @Param("subtype") String subtype,
                                                                       @Param("keyword") String keyword,
                                                                       @Param("offset") int offset,
                                                                       @Param("limit") int limit);

    int countBySubtypeAndKeyword(@Param("subtype") String subtype,
                                  @Param("keyword") String keyword);

    int countBySubtypeAndKeywordAndDataType(@Param("dataType") String dataType,
                                            @Param("subtype") String subtype,
                                            @Param("keyword") String keyword);

    List<DataElementBusinessRecord> findByConditions(@Param("dataSubtype") String dataSubtype,
                                                      @Param("keyword") String keyword,
                                                      @Param("batchId") Long batchId,
                                                      @Param("uploadTimeStart") String uploadTimeStart,
                                                      @Param("uploadTimeEnd") String uploadTimeEnd,
                                                      @Param("offset") int offset,
                                                      @Param("limit") int limit);

    int countByConditions(@Param("dataSubtype") String dataSubtype,
                          @Param("keyword") String keyword,
                          @Param("batchId") Long batchId,
                          @Param("uploadTimeStart") String uploadTimeStart,
                          @Param("uploadTimeEnd") String uploadTimeEnd);

    int deleteById(@Param("id") Long id);

    int deleteByBatchId(@Param("batchId") Long batchId);

    int countByBatchId(@Param("batchId") Long batchId);

    int countByBatchIdAndDataType(@Param("batchId") Long batchId,
                                  @Param("dataType") String dataType);

    /** 获取每个子类的统计信息（用于汇总列表） */
    List<SubtypeSummary> getSubtypeSummaries();

    /** 获取指定数据一级类型下每个子类的统计信息 */
    List<SubtypeSummary> getSubtypeSummariesByType(@Param("dataType") String dataType);

    /** 获取所有记录 */
    List<DataElementBusinessRecord> findAllForExport();

    /** 获取指定数据一级类型的导出记录 */
    List<DataElementBusinessRecord> findAllForExportByType(@Param("dataType") String dataType);
}
