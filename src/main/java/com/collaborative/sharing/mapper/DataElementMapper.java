package com.collaborative.sharing.mapper;

import com.collaborative.sharing.entity.DataElement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataElementMapper {
    
    /**
     * 查找所有数据要素
     */
    List<DataElement> findAll();
    
    /**
     * 根据ID查找数据要素
     */
    DataElement findById(@Param("id") Long id);
    
    /**
     * 插入数据要素
     */
    int insert(DataElement dataElement);
    
    /**
     * 更新数据要素
     */
    int update(DataElement dataElement);
    
    /**
     * 根据ID删除数据要素
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据状态查找数据要素
     */
    List<DataElement> findByStatus(@Param("status") String status);
    
    /**
     * 根据条件查询数据要素（用于分页）
     */
    List<DataElement> findByConditions(@Param("dataName") String dataName,
                                       @Param("dataType") String dataType,
                                       @Param("dataSubtype") String dataSubtype,
                                       @Param("securityLevel") String securityLevel,
                                       @Param("department") String department,
                                       @Param("status") String status);
}
