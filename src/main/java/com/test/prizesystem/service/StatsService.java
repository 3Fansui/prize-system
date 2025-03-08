package com.test.prizesystem.service;

import java.util.Map;

/**
 * 统计服务接口
 * <p>
 * 提供系统抽奖统计功能，包括抽奖次数、中奖次数等统计数据。
 * 
 * @author MCP生成
 * @version 1.0
 */
public interface StatsService {

    /**
     * 获取统计数据
     *
     * @return 统计数据Map
     */
    Map<String, Object> getStats();
    
    /**
     * 增加抽奖次数统计
     */
    void incrementDrawCount();
    
    /**
     * 增加中奖次数统计
     */
    void incrementWinCount();
}
