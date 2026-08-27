package com.collaborative.sharing.mapper;

import com.collaborative.sharing.entity.SystemSettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SystemSettingsMapper {
    
    /**
     * 根据设置键查找设置
     */
    SystemSettings findByKey(@Param("settingKey") String settingKey);
    
    /**
     * 插入设置
     */
    int insert(SystemSettings settings);
    
    /**
     * 更新设置
     */
    int update(SystemSettings settings);
    
    /**
     * 根据键更新值
     */
    int updateValueByKey(@Param("settingKey") String settingKey, @Param("settingValue") String settingValue);
}

