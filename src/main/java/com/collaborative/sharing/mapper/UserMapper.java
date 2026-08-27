package com.collaborative.sharing.mapper;

import com.collaborative.sharing.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    
    /**
     * 根据用户名查找用户
     */
    User findByUsername(@Param("username") String username);
    
    /**
     * 查找所有用户
     */
    List<User> findAll();
    
    /**
     * 根据ID查找用户
     */
    User findById(@Param("id") Long id);
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 更新用户
     */
    int update(User user);
    
    /**
     * 根据ID删除用户
     */
    int deleteById(@Param("id") Long id);
}
