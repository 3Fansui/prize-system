package com.test.prizesystem.controller;

import com.test.prizesystem.model.entity.Token;

import com.test.prizesystem.service.TokenService;
import com.test.prizesystem.service.UserService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.TokenQueue;
import com.test.prizesystem.util.TreeNames;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存和系统监控控制器
 * <p>
 * 提供访问系统内部状态、缓存数据和统计信息的接口。
 * 简化版仅保留基本监控功能。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@Api(tags = "缓存管理", description = "查看和管理系统缓存数据")
public class CacheController {

    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    @Autowired
    private TokenQueue tokenQueue;
    
    @Autowired
    private UserService userService;

    /**
     * 查看令牌缓存信息
     */
    @GetMapping("/tokens/{activityId}")
    @ApiOperation(value = "获取活动令牌详情", notes = "返回指定活动的令牌详细信息")
    public Map<String, Object> getTokenDetails(
            @ApiParam(value = "活动ID", required = true, example = "1") @PathVariable Integer activityId) {
        return tokenService.getTokenDetails(activityId);
    }
    
    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    @ApiOperation(value = "获取系统状态", notes = "返回当前系统的统计信息")
    public Map<String, Object> getSystemStatus() {
        // 构建系统状态响应
        Map<String, Object> status = new HashMap<>();
        
        // 假设活动ID为1
        Integer activityId = 1;
        status.put("tokenQueueSize", tokenQueue.size(activityId));
        
        // 获取奖品剩余数量 - 直接从红黑树获取
        Map<String, Integer> prizeRemaining = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            com.test.prizesystem.model.entity.Prize prize = 
                treeStorage.find(TreeNames.PRIZES, i, com.test.prizesystem.model.entity.Prize.class);
            if (prize != null) {
                prizeRemaining.put(prize.getName(), prize.getRemainingAmount());
            }
        }
        
        status.put("prizeRemaining", prizeRemaining);
        status.put("timestamp", System.currentTimeMillis());
        
        // 添加用户数量信息
        status.put("usersCount", treeStorage.size(TreeNames.USERS));
        
        return status;
    }
    
    /**
     * 获取令牌队列内容
     */
    @GetMapping("/token-queue/{activityId}")
    @ApiOperation(value = "查看活动令牌队列", notes = "返回指定活动的令牌队列内容")
    public Map<String, Object> getTokenQueueContent(
            @ApiParam(value = "活动ID", required = true, example = "1") @PathVariable Integer activityId) {
        Map<String, Object> result = new HashMap<>();
        result.put("queueSize", tokenQueue.size(activityId));
        
        // 获取队列中的令牌（限制10个）
        List<Token> tokens = tokenQueue.getTokens(activityId, 10);
        result.put("tokens", tokens);
        
        return result;
    }
}