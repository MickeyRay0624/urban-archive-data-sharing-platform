package com.collaborative.sharing.mapper;

import com.collaborative.sharing.entity.DataElementUploadBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataElementUploadBatchMapper {

    int insert(DataElementUploadBatch batch);

    int update(DataElementUploadBatch batch);

    DataElementUploadBatch findById(@Param("id") Long id);

    DataElementUploadBatch findByFileHash(@Param("fileHash") String fileHash);

    DataElementUploadBatch findByFileHashAndDataType(@Param("fileHash") String fileHash,
                                                     @Param("dataType") String dataType);

    List<DataElementUploadBatch> findAll();

    List<DataElementUploadBatch> findRecent(@Param("limit") int limit);

    List<DataElementUploadBatch> findRecentByType(@Param("dataType") String dataType,
                                                  @Param("limit") int limit);

    List<DataElementUploadBatch> findByPage(@Param("keyword") String keyword,
                                             @Param("uploader") String uploader,
                                             @Param("startTime") String startTime,
                                             @Param("endTime") String endTime,
                                             @Param("subtype") String subtype,
                                             @Param("isDuplicate") Boolean isDuplicate,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    List<DataElementUploadBatch> findByPageByType(@Param("dataType") String dataType,
                                                   @Param("keyword") String keyword,
                                                   @Param("uploader") String uploader,
                                                   @Param("startTime") String startTime,
                                                   @Param("endTime") String endTime,
                                                   @Param("subtype") String subtype,
                                                   @Param("isDuplicate") Boolean isDuplicate,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);

    int countByPage(@Param("keyword") String keyword,
                    @Param("uploader") String uploader,
                    @Param("startTime") String startTime,
                    @Param("endTime") String endTime,
                    @Param("subtype") String subtype,
                    @Param("isDuplicate") Boolean isDuplicate);

    int countByPageByType(@Param("dataType") String dataType,
                          @Param("keyword") String keyword,
                          @Param("uploader") String uploader,
                          @Param("startTime") String startTime,
                          @Param("endTime") String endTime,
                          @Param("subtype") String subtype,
                          @Param("isDuplicate") Boolean isDuplicate);

    int deleteById(@Param("id") Long id);
}
