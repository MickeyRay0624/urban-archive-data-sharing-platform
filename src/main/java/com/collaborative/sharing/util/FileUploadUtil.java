package com.collaborative.sharing.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传工具类
 */
public class FileUploadUtil {
    
    // 上传文件根目录 - 使用项目根目录下的uploads文件夹
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
    
    // 允许上传的文件类型
    private static final String[] ALLOWED_EXTENSIONS = {
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
        ".txt", ".jpg", ".jpeg", ".png", ".gif", ".zip", ".rar"
    };
    
    /**
     * 保存上传的文件
     * @param file 上传的文件
     * @param subDir 子目录（如：todo、data）
     * @return 文件保存的相对路径
     */
    public static String saveFile(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("文件为空");
        }
        
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IOException("文件名为空");
        }
        
        // 验证文件类型
        if (!isAllowedExtension(originalFilename)) {
            throw new IOException("不支持的文件类型");
        }
        
        // 生成新文件名（使用UUID防止重名）
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + extension;
        
        // 创建日期子目录
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        // 创建完整路径（用于文件系统）
        String fullDirPath = UPLOAD_DIR + subDir + File.separator + dateDir;
        File uploadDir = new File(fullDirPath);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new IOException("无法创建上传目录: " + fullDirPath);
            }
        }
        
        // 保存文件
        File destFile = new File(uploadDir, newFilename);
        file.transferTo(destFile);
        
        System.out.println("文件上传成功: " + destFile.getAbsolutePath());
        
        // 返回URL格式的相对路径（使用正斜杠）
        String relativePath = subDir + "/" + dateDir + "/" + newFilename;
        return relativePath;
    }
    
    /**
     * 删除文件
     * @param relativePath 文件相对路径
     */
    public static void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return;
        }
        
        try {
            File file = new File(UPLOAD_DIR + relativePath);
            if (file.exists()) {
                file.delete();
                System.out.println("文件删除成功: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("文件删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证文件扩展名是否允许
     */
    private static boolean isAllowedExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取文件的绝对路径
     */
    public static String getAbsolutePath(String relativePath) {
        return UPLOAD_DIR + relativePath;
    }
}

