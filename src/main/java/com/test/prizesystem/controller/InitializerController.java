package com.test.prizesystem.controller;

import com.test.prizesystem.config.DemoDataInitializer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统初始化控制器
 * <p>
 * 提供手动初始化系统数据的接口
 * </p>
 *
 * @author wu
 */
@RestController
@RequestMapping("/api/system")
@Api(tags = "系统管理", description = "系统初始化和管理接口")
public class InitializerController {

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    /**
     * 初始化演示数据
     * 
     * @return 初始化结果
     */
    @PostMapping("/init-demo-data")
    @ApiOperation(value = "初始化演示数据", notes = "手动初始化系统演示数据，包括活动和奖品")
    public Map<String, Object> initializeDemoData() {
        String result = demoDataInitializer.initializeDemoData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", result);
        
        return response;
    }
}