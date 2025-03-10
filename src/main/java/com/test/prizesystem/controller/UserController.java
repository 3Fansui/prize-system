package com.test.prizesystem.controller;

import com.test.prizesystem.model.entity.User;
import com.test.prizesystem.model.entity.UserPrizeRecord;
import com.test.prizesystem.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 * <p>
 * 提供用户注册、登录和用户信息查询API。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Api(tags = "用户接口", description = "用户注册、登录和信息查询")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     * 
     * @param username 用户名
     * @param password 密码
     * @param id 指定用户ID
     * @param drawQuota 抽奖配额
     * @param winQuota 中奖配额
     * @return 注册结果
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "创建新用户并分配抽奖和中奖配额")
    public Map<String, Object> register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) Integer drawQuota,
            @RequestParam(required = false) Integer winQuota) {
        
        Map<String, Object> result = new HashMap<>();
        User user = userService.register(id, username, password, drawQuota, winQuota);
        
        if (user != null) {
            result.put("success", true);
            result.put("userId", user.getId());
            result.put("message", "注册成功");
            result.put("drawQuota", user.getDrawQuota());
            result.put("winQuota", user.getWinQuota());
        } else {
            result.put("success", false);
            result.put("message", "用户名已存在");
        }
        
        return result;
    }
    
    /**
     * 用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "验证用户身份并返回用户信息")
    public Map<String, Object> login(
            @RequestParam String username,
            @RequestParam String password) {
        
        Map<String, Object> result = new HashMap<>();
        User user = userService.login(username, password);
        
        if (user != null) {
            result.put("success", true);
            result.put("userId", user.getId());
            result.put("username", user.getUsername());
            result.put("message", "登录成功");
            result.put("drawQuota", user.getDrawQuota());
            result.put("drawCount", user.getDrawCount());
            result.put("remainingDraws", user.getRemainingDraws());
            result.put("winQuota", user.getWinQuota());
            result.put("winCount", user.getWinCount());
            result.put("remainingWins", user.getRemainingWins());
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        
        return result;
    }
    
    /**
     * 获取用户信息及中奖记录
     * 
     * @param userId 用户ID
     * @param prizeLimit 中奖记录数量限制
     * @return 用户信息及中奖记录
     */
    @GetMapping("/{userId}")
    @ApiOperation(value = "获取用户信息", notes = "返回用户详细信息及中奖记录")
    public Map<String, Object> getUserInfo(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "10") int prizeLimit) {
        
        Map<String, Object> result = new HashMap<>();
        User user = userService.getUser(userId);
        
        if (user != null) {
            result.put("success", true);
            result.put("userId", user.getId());
            result.put("username", user.getUsername());
            result.put("drawQuota", user.getDrawQuota());
            result.put("drawCount", user.getDrawCount());
            result.put("remainingDraws", user.getRemainingDraws());
            result.put("winQuota", user.getWinQuota());
            result.put("winCount", user.getWinCount());
            result.put("remainingWins", user.getRemainingWins());
            result.put("lastLoginTime", user.getLastLoginTime());
            
            // 获取用户中奖记录
            List<UserPrizeRecord> records = userService.getUserPrizeRecords(userId, prizeLimit);
            result.put("prizeRecords", records);
            result.put("prizeCount", records.size());
        } else {
            result.put("success", false);
            result.put("message", "用户不存在");
        }
        
        return result;
    }
    
    /**
     * 批量创建测试用户
     * 
     * @param startId 起始ID
     * @param count 用户数量
     * @param usernamePrefix 用户名前缀
     * @param password 密码
     * @param drawQuota 抽奖配额
     * @param winQuota 中奖配额
     * @return 创建结果
     */
    @PostMapping("/batchCreate")
    @ApiOperation(value = "批量创建用户", notes = "批量创建用户用于压测")
    public Map<String, Object> batchCreateUsers(
            @RequestParam Integer startId,
            @RequestParam Integer count,
            @RequestParam(required = false) String usernamePrefix,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) Integer drawQuota,
            @RequestParam(required = false) Integer winQuota) {
        
        Map<String, Object> result = new HashMap<>();
        List<Integer> createdIds = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            int id = startId + i;
            String username = usernamePrefix + id;
            
            User user = userService.register(id, username, password, drawQuota, winQuota);
            if (user != null) {
                createdIds.add(user.getId());
            }
        }
        
        result.put("success", true);
        result.put("message", String.format("成功创建 %d 个用户", createdIds.size()));
        result.put("createdIds", createdIds);
        
        return result;
    }
}