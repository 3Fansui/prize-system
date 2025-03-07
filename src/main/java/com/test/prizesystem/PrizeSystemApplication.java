package com.test.prizesystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 启用定时任务
public class PrizeSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrizeSystemApplication.class, args);
    }

}
