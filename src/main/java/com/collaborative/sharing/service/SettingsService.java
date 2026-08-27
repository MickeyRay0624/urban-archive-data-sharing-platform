package com.collaborative.sharing.service;

import com.collaborative.sharing.entity.SystemSettings;
import com.collaborative.sharing.entity.User;
import com.collaborative.sharing.mapper.SystemSettingsMapper;
import com.collaborative.sharing.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.collaborative.sharing.util.FileUploadUtil;

import java.time.LocalDateTime;

@Service
public class SettingsService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private SystemSettingsMapper systemSettingsMapper;
    
    /**
     * 修改用户密码
     */
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return false;
        }
        
        // 验证旧密码
        if (!user.getPassword().equals(oldPassword)) {
            return false;
        }
        
        // 更新密码
        user.setPassword(newPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        
        return true;
    }
    
    /**
     * 获取背景图片设置
     */
    public String getBackgroundImage() {
        SystemSettings settings = systemSettingsMapper.findByKey("background_image");
        if (settings != null) {
            String path = settings.getSettingValue();
            // 将Windows路径分隔符转换为URL格式的正斜杠
            if (path != null) {
                path = path.replace("\\", "/");
            }
            return path;
        }
        return null;
    }
    
    /**
     * 上传并设置背景图片
     */
    public String uploadBackgroundImage(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("文件为空");
        }
        
        // 保存文件
        String filePath = FileUploadUtil.saveFile(file, "background");
        
        // 更新或插入设置
        SystemSettings settings = systemSettingsMapper.findByKey("background_image");
        if (settings == null) {
            settings = new SystemSettings();
            settings.setSettingKey("background_image");
            settings.setSettingValue(filePath);
            settings.setCreatedAt(LocalDateTime.now());
            settings.setUpdatedAt(LocalDateTime.now());
            systemSettingsMapper.insert(settings);
        } else {
            // 删除旧的背景图片
            if (settings.getSettingValue() != null) {
                FileUploadUtil.deleteFile(settings.getSettingValue());
            }
            settings.setSettingValue(filePath);
            settings.setUpdatedAt(LocalDateTime.now());
            systemSettingsMapper.update(settings);
        }
        
        return filePath;
    }
}

