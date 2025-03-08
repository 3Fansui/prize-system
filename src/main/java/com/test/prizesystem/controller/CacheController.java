package com.test.prizesystem.controller;

import com.test.prizesystem.model.entity.Token;
import com.test.prizesystem.service.PrizeService;
import com.test.prizesystem.service.StatsService;
import com.test.prizesystem.service.TokenService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.TokenQueue;
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
 * 包括访问令牌队列和红黑树存储引擎中的数据。
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
    private PrizeService prizeService;
    
    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    @Autowired
    private TokenQueue tokenQueue;
    
    @Autowired
    private StatsService statsService;

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
     * 获取存储引擎状态
     */
    @GetMapping("/storage")
    @ApiOperation(value = "获取存储引擎状态", notes = "返回当前内存存储引擎的统计信息")
    public Map<String, Object> getStorageStatus() {
        // 构建一个包含各种统计信息的响应
        Map<String, Object> status = new HashMap<>();
        
        status.put("tokenQueueSize", tokenQueue.size());
        
        // 获取各树的大小
        String[] treeNames = {"activities", "prizes", "activity_prizes", "activity_rules", 
                             "user_draw_records", "user_prize_records", "tokens"};
        
        Map<String, Integer> treeSizes = new HashMap<>();
        for (String treeName : treeNames) {
            treeSizes.put(treeName, treeStorage.size(treeName));
        }
        
        status.put("treeSizes", treeSizes);
        
        // 获取奖品剩余数量
        Map<String, Integer> prizeRemaining = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            com.test.prizesystem.model.entity.Prize prize = prizeService.getPrize(i);
            if (prize != null) {
                prizeRemaining.put(prize.getName(), prize.getRemainingAmount());
            }
        }
        
        status.put("prizeRemaining", prizeRemaining);
        status.put("timestamp", System.currentTimeMillis());
        
        return status;
    }
    
    /**
     * 查看指定树的详细内容
     */
    @GetMapping("/tree/{treeName}")
    @ApiOperation(value = "查看树内容", notes = "返回指定红黑树的详细内容，支持可选的数量限制")
    public Map<String, Object> getTreeContent(
            @ApiParam(value = "树名称", required = true, example = "prizes") @PathVariable String treeName,
            @ApiParam(value = "最大返回数量", defaultValue = "10", example = "10") 
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("treeName", treeName);
        result.put("size", treeStorage.size(treeName));
        
        // 获取树中存储的实体类型
        Class<?> entityClass = getEntityClassByTreeName(treeName);
        if (entityClass == null) {
            result.put("error", "未找到对应的实体类型");
            return result;
        }
        
        // 获取树中的样本数据
        try {
            List<?> sampleData = treeStorage.getSampleData(treeName, entityClass, limit);
            result.put("data", sampleData);
        } catch (Exception e) {
            log.error("获取树内容失败: {}", e.getMessage(), e);
            result.put("error", "获取树内容失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取令牌队列内容
     */
    @GetMapping("/token-queue")
    @ApiOperation(value = "查看令牌队列", notes = "返回当前令牌队列中的内容，支持可选的数量限制")
    public Map<String, Object> getTokenQueueContent(
            @ApiParam(value = "最大返回数量", defaultValue = "10", example = "10") 
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("queueSize", tokenQueue.size());
        
        // 获取队列中的令牌
        List<Token> tokens = tokenQueue.getTokens(limit);
        result.put("tokens", tokens);
        
        return result;
    }
    
    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    @ApiOperation(value = "获取统计数据", notes = "返回系统的抽奖统计信息")
    public Map<String, Object> getStats() {
        return statsService.getStats();
    }
    
    /**
     * 根据树名获取对应的实体类
     */
    private Class<?> getEntityClassByTreeName(String treeName) {
        switch (treeName) {
            case "activities":
                return com.test.prizesystem.model.entity.Activity.class;
            case "prizes":
                return com.test.prizesystem.model.entity.Prize.class;
            case "activity_prizes":
                return com.test.prizesystem.model.entity.ActivityPrize.class;
            case "activity_rules":
                return com.test.prizesystem.model.entity.ActivityRule.class;
            case "user_draw_records":
                return com.test.prizesystem.model.entity.UserDrawRecord.class;
            case "user_prize_records":
                return com.test.prizesystem.model.entity.UserPrizeRecord.class;
            case "tokens":
                return com.test.prizesystem.model.entity.Token.class;
            default:
                return null;
        }
    }
}