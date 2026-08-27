package com.collaborative.sharing.mapper;

import com.collaborative.sharing.entity.TodoItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TodoItemMapper {
    
    /**
     * 查找所有待办事项
     */
    List<TodoItem> findAll();
    
    /**
     * 根据ID查找待办事项
     */
    TodoItem findById(@Param("id") Long id);
    
    /**
     * 插入待办事项
     */
    int insert(TodoItem todoItem);
    
    /**
     * 更新待办事项
     */
    int update(TodoItem todoItem);
    
    /**
     * 根据ID删除待办事项
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据状态查找待办事项
     */
    List<TodoItem> findByStatus(@Param("status") String status);
    
    /**
     * 条件查询待办事项（支持分页）
     */
    List<TodoItem> findByConditions(@Param("processName") String processName,
                                    @Param("uploader") String uploader,
                                    @Param("department") String department,
                                    @Param("status") String status);
}
