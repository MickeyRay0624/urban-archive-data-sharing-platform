package com.collaborative.sharing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web配置类 - 配置静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录的绝对路径
        String uploadPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
        
        // 将 /uploads/** 请求映射到项目根目录下的 uploads 文件夹
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
        
        System.out.println("静态资源映射配置完成: /uploads/** -> " + uploadPath);
    }
}

