package com.test.prizesystem.controller;


import com.test.prizesystem.config.DemoDataInitializer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理控制器
 * <p>
 * 提供系统管理功能，如数据初始化、系统配置等。
 * 
 * @author MCP生成
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin")
@Api(tags = "管理接口", description = "系统管理相关功能")
public class AdminController {

    @Autowired
    private DemoDataInitializer dataInitializer;

    /**
     * 初始化演示数据
     * <p>
     * 测试环境下初始化活动、奖品等演示数据。
     * 
     * @return 初始化结果消息
     */
    @PostMapping("/init-demo-data")
    @ApiOperation(value = "初始化演示数据", notes = "初始化系统测试所需的活动、奖品等数据")
    public String initDemoData() {
        try {
            dataInitializer.run();
            return "演示数据初始化成功";
        } catch (Exception e) {
            return "演示数据初始化失败: " + e.getMessage();
        }
    }
}