package com.test.prizesystem.service;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.Token;

import java.util.List;
import java.util.Map;

/**
 * 令牌服务接口
 * <p>
 * 提供令牌生成、获取和管理的服务。
 * 
 * @author MCP生成
 * @version 2.0
 */
public interface TokenService {

    /**
     * 生成令牌
     * 
     * @param activity 活动对象
     * @param activityPrizes 活动奖品关联列表
     */
    void generateTokens(Activity activity, List<ActivityPrize> activityPrizes);

    /**
     * 获取可用令牌
     * 
     * @param activityId 活动ID
     * @param timestamp 当前时间戳
     * @return 可用的令牌，如果没有则返回null
     */
    Token getAvailableToken(Integer activityId, long timestamp);

    /**
     * 将未使用的令牌返回队列
     * 
     * @param token 需要返回的令牌
     */
    void returnToken(Token token);

    /**
     * 获取活动的令牌队列详细信息
     * 
     * @param activityId 活动ID
     * @return 令牌队列信息
     */
    Map<String, Object> getTokenDetails(Integer activityId);
    
    /**
     * 获取活动的令牌队列大小
     * 
     * @param activityId 活动ID
     * @return 令牌队列大小
     */
    int getTokenQueueSize(Integer activityId);
}