package com.test.prizesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 抽奖系统应用程序入口类
 * <p>
 * 该系统是一个高性能的抗压抽奖系统，使用红黑树作为内存存储引擎，支持活动管理、抽奖、数据统计等功能。
 * 本系统排除了数据库自动配置，使用内存存储来提高系统的并发处理能力。
 * 系统集成了Swagger接口文档，并启用了定时任务功能来处理各种计划任务。
 * <p>
 * 主要功能模块：
 * <ul>
 *   <li>活动管理：创建、编辑、预热活动</li>
 *   <li>奖品管理：管理各种奖品及其概率配置</li>
 *   <li>抽奖引擎：高性能并发抽奖服务</li>
 *   <li>数据统计：用户参与和中奖统计</li>
 *   <li>缓存管理：内存数据管理与监控</li>
 * </ul>
 * 
 * @author MCP生成
 * @version 1.0
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableScheduling // 启用定时任务
public class PrizeSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrizeSystemApplication.class, args);
    }

}
