package com.collaborative.sharing.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.collaborative.sharing.mapper")
public class MyBatisConfig {
}
