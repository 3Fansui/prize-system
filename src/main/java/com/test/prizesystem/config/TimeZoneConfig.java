package com.test.prizesystem.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * 全局时区配置
 * <p>
 * 设置系统默认时区为UTC+8（中国标准时间）
 * 确保系统中的所有时间操作都使用同一时区
 * 
 * @version 1.0
 */
@Slf4j
@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void init() {
        TimeZone chinaTimeZone = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(chinaTimeZone);
        log.info("系统时区已设置为: {}", chinaTimeZone.getID());
    }
}
