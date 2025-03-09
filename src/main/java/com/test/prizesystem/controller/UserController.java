package com.test.prizesystem.controller;

import com.test.prizesystem.model.entity.User;
import com.test.prizesystem.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
     * @param drawQuota 抽奖配额
     * @param winQuota 中奖配额
     * @return 注册结果
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "创建新用户并分配抽奖和中奖配额")
    public Map<String, Object> register(
            @ApiParam(value = "用户名", required = true) @RequestParam String username,
            @ApiParam(value = "密码", required = true) @RequestParam String password,
            @ApiParam(value = "抽奖配额", defaultValue = "100") @RequestParam(required = false) Integer drawQuota,
            @ApiParam(value = "中奖配额", defaultValue = "10") @RequestParam(required = false) Integer winQuota) {
        
        Map<String, Object> result = new HashMap<>();
        User user = userService.register(username, password, drawQuota, winQuota);
        
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
            @ApiParam(value = "用户名", required = true) @RequestParam String username,
            @ApiParam(value = "密码", required = true) @RequestParam String password) {
        
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
     * 获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    @ApiOperation(value = "获取用户信息", notes = "根据用户ID获取用户详细信息")
    public Map<String, Object> getUserInfo(
            @ApiParam(value = "用户ID", required = true) @PathVariable Integer userId) {
        
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
        } else {
            result.put("success", false);
            result.put("message", "用户不存在");
        }
        
        return result;
    }
}