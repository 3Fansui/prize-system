package com.test.prizesystem.service;

import com.test.prizesystem.model.entity.User;
import com.test.prizesystem.model.entity.UserPrizeRecord;

import java.util.List;

/**
 * 用户服务接口
 * <p>
 * 提供用户注册、登录和用户信息管理功能。
 * 专门为压测场景设计，管理用户抽奖和中奖配额。
 * 
 * @author MCP生成
 * @version 1.0
 */
public interface UserService {
    
    /**
     * 用户注册（指定ID）
     * @param id 指定的用户ID，为null时自动生成
     * @param username 用户名
     * @param password 密码
     * @param drawQuota 抽奖次数配额
     * @param winQuota 中奖次数配额
     * @return 注册成功的用户信息
     */
    User register(Integer id, String username, String password, Integer drawQuota, Integer winQuota);
    
    /**
     * 用户注册（自动生成ID）
     * @param username 用户名
     * @param password 密码
     * @param drawQuota 抽奖次数配额
     * @param winQuota 中奖次数配额
     * @return 注册成功的用户信息
     */
    User register(String username, String password, Integer drawQuota, Integer winQuota);
    
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录成功的用户信息，登录失败返回null
     */
    User login(String username, String password);
    
    /**
     * 根据ID获取用户
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUser(Integer userId);
    
    /**
     * 尝试进行一次抽奖（检查是否达到上限并增加计数）
     * @param userId 用户ID
     * @return 是否允许抽奖
     */
    boolean tryDraw(Integer userId);
    
    /**
     * 尝试进行一次中奖（检查是否达到上限并增加计数）
     * @param userId 用户ID
     * @return 是否允许中奖
     */
    boolean tryWin(Integer userId);
    
    /**
     * 获取用户剩余抽奖次数
     * @param userId 用户ID
     * @return 剩余次数
     */
    int getRemainingDraws(Integer userId);
    
    /**
     * 获取用户剩余中奖次数
     * @param userId 用户ID
     * @return 剩余次数
     */
    int getRemainingWins(Integer userId);
    
    /**
     * 获取用户中奖记录
     * @param userId 用户ID
     * @param limit 最大返回数量
     * @return 中奖记录列表
     */
    List<UserPrizeRecord> getUserPrizeRecords(Integer userId, int limit);
}