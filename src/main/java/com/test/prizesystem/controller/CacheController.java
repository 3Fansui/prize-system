package com.test.prizesystem.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.prizesystem.mapper.PrizeMapper;
import com.test.prizesystem.mapper.TokenMapper;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.entity.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private TokenMapper tokenMapper;

    @Autowired
    private PrizeMapper prizeMapper;

    /**
     * 查看令牌缓存信息
     */
    @GetMapping("/tokens/{activityId}")
    public Map<String, Object> getTokenDetails(@PathVariable Integer activityId) {
        Map<String, Object> result = new HashMap<>();

        // 查询未使用的令牌
        LambdaQueryWrapper<Token> unusedQuery = new LambdaQueryWrapper<>();
        unusedQuery.eq(Token::getActivityId, activityId)
                .eq(Token::getStatus, 0);
        Long unusedCount = tokenMapper.selectCount(unusedQuery);

        // 查询已使用的令牌
        LambdaQueryWrapper<Token> usedQuery = new LambdaQueryWrapper<>();
        usedQuery.eq(Token::getActivityId, activityId)
                .eq(Token::getStatus, 1);
        Long usedCount = tokenMapper.selectCount(usedQuery);

        // 查询部分未使用令牌的详细信息（限制数量以避免返回过多数据）
        List<Token> unusedTokens = tokenMapper.selectList(
                unusedQuery.last("LIMIT 20")
        );

        // 获取这些令牌对应的奖品信息
        List<Map<String, Object>> tokenDetails = new ArrayList<>();
        for (Token token : unusedTokens) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("tokenId", token.getId());
            detail.put("timestamp", token.getTokenTimestamp());

            Prize prize = prizeMapper.selectById(token.getPrizeId());
            if (prize != null) {
                detail.put("prizeId", prize.getId());
                detail.put("prizeName", prize.getName());
                detail.put("prizePrice", prize.getPrice());
            }

            tokenDetails.add(detail);
        }

        // 按奖品汇总未使用令牌数量
        Map<Integer, Integer> prizeTokenCounts = new HashMap<>();
        List<Map<String, Object>> prizeTokenStats = new ArrayList<>();

        // 使用SQL查询每个奖品的令牌数量
        List<Map<String, Object>> prizeStats = tokenMapper.selectPrizeTokenStats(activityId, 0);
        for (Map<String, Object> stat : prizeStats) {
            Integer prizeId = (Integer) stat.get("prizeId");
            Integer count = ((Number) stat.get("tokenCount")).intValue();
            prizeTokenCounts.put(prizeId, count);

            Prize prize = prizeMapper.selectById(prizeId);
            Map<String, Object> prizeStat = new HashMap<>();
            prizeStat.put("prizeId", prizeId);
            prizeStat.put("prizeName", prize != null ? prize.getName() : "未知奖品");
            prizeStat.put("tokenCount", count);
            prizeTokenStats.add(prizeStat);
        }

        // 汇总结果
        result.put("activityId", activityId);
        result.put("totalTokens", unusedCount + usedCount);
        result.put("unusedTokens", unusedCount);
        result.put("usedTokens", usedCount);
        result.put("tokenDetails", tokenDetails);
        result.put("prizeTokenStats", prizeTokenStats);

        return result;
    }
}

