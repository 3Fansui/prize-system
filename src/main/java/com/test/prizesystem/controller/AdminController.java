package com.test.prizesystem.controller;


import com.test.prizesystem.config.DemoDataInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private DemoDataInitializer dataInitializer;

    @PostMapping("/init-demo-data")
    public String initDemoData() {
        try {
            dataInitializer.run();
            return "演示数据初始化成功";
        } catch (Exception e) {
            return "演示数据初始化失败: " + e.getMessage();
        }
    }
}