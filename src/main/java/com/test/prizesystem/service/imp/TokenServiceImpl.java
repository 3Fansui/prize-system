package com.test.prizesystem.service.imp;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.prizesystem.mapper.PrizeMapper;
import com.test.prizesystem.mapper.TokenMapper;
import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.entity.Token;
import com.test.prizesystem.service.TokenService;
import com.test.prizesystem.util.RedBlackTree;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    @Autowired
    private TokenMapper tokenMapper;

    @Autowired
    private PrizeMapper prizeMapper;

    // 将生成的令牌保存到红黑树中，用于内存中快速查找
    private RedBlackTree tokenTree = new RedBlackTree();

    @Override
    @Transactional
    public void generateTokens(Activity activity, List<ActivityPrize> activityPrizes) {
        log.info("开始为活动 {} 生成令牌...", activity.getId());

        // 清除旧的令牌
        tokenMapper.delete(null);

        // 创建新的红黑树实例
        RedBlackTree tokenTree = new RedBlackTree();
        log.info("创建新的红黑树实例");

        long startTime = activity.getStartTime().getTime();
        long endTime = activity.getEndTime().getTime();
        long duration = endTime - startTime;

        // 根据活动类型生成不同的令牌
        if (activity.getType() == 2) { // 固定时间型
            log.info("开始生成固定时间型令牌...");
            generateTimedTokens(activity.getId(), activityPrizes, startTime, duration);
        } else if (activity.getType() == 3) { // 先到先得型
            log.info("开始生成先到先得型令牌...");
            generateFCFSTokens(activity.getId(), activityPrizes, startTime);
        }

        log.info("令牌生成完成并已加入红黑树，总数: {}", tokenTree.size());
    }

    @Override
    public Token getAvailableToken(Integer activityId, long timestamp) {
        // 使用红黑树查找令牌
        Object tokenKey = tokenTree.findTokenBefore(timestamp);
        if (tokenKey == null) {
            // 如果内存中没找到，从数据库查询
            return tokenMapper.getAvailableToken(activityId, timestamp);
        }

        // 解析tokenKey获取令牌ID
        String tokenKeyStr = (String) tokenKey;
        String[] parts = tokenKeyStr.split("_");
        Long tokenId = Long.parseLong(parts[0]);

        // 获取令牌信息
        Token token = tokenMapper.selectById(tokenId);

        // 标记令牌为已使用
        if (token != null && token.getStatus() == 0) {
            useToken(token.getId());
        }

        return token;
    }

    @Override
    public Token getNextToken(Integer activityId) {
        // 从红黑树中获取第一个令牌
        Object tokenKey = tokenTree.findFirstToken();
        if (tokenKey == null) {
            // 如果内存中没找到，从数据库查询
            return tokenMapper.getNextToken(activityId);
        }

        // 解析tokenKey获取令牌ID
        String tokenKeyStr = (String) tokenKey;
        String[] parts = tokenKeyStr.split("_");
        Long tokenId = Long.parseLong(parts[0]);

        // 获取令牌信息
        Token token = tokenMapper.selectById(tokenId);

        // 标记令牌为已使用
        if (token != null && token.getStatus() == 0) {
            useToken(token.getId());
        }

        return token;
    }

    @Override
    public boolean useToken(Long tokenId) {
        // 更新令牌状态为已使用
        return tokenMapper.updateTokenStatus(tokenId) > 0;
    }

    private void generateTimedTokens(Integer activityId, List<ActivityPrize> activityPrizes, long startTime, long duration) {
        // 计算总令牌数量
        int totalTokens = 0;
        for (ActivityPrize activityPrize : activityPrizes) {
            Prize prize = prizeMapper.selectById(activityPrize.getPrizeId());
            if (prize != null) {
                totalTokens += Math.min(activityPrize.getAmount(), prize.getRemainingAmount());
            }
        }

        // 生成均匀分布的令牌
        long interval = duration / (totalTokens > 0 ? totalTokens : 1);
        List<Token> tokens = new ArrayList<>();

        for (ActivityPrize activityPrize : activityPrizes) {
            Prize prize = prizeMapper.selectById(activityPrize.getPrizeId());
            if (prize == null || prize.getRemainingAmount() <= 0) {
                continue;
            }

            int amount = Math.min(activityPrize.getAmount(), prize.getRemainingAmount());

            for (int i = 0; i < amount; i++) {
                // 生成时间均匀分布的时间戳
                long tokenTimestamp = startTime + i * interval + new Random().nextInt((int) (interval * 0.8));

                Token token = new Token();
                token.setActivityId(activityId);
                token.setPrizeId(prize.getId());
                token.setTokenTimestamp(tokenTimestamp);
                token.setStatus(0);

                tokens.add(token);

                // 将令牌存入红黑树
                tokenTree.put(tokenTimestamp, token.getId() + "_" + token.getPrizeId());

                // 为每个令牌打印日志
                log.info("生成令牌: 奖品[{}] {}, 时间戳: {}, 时间: {}",
                        prize.getId(),
                        prize.getName(),
                        tokenTimestamp,
                        new Date(tokenTimestamp));
            }
            log.info("奖品 [{}] {} 令牌生成完成，共 {} 个",
                    prize.getId(),
                    prize.getName(),
                    amount);

        }

        // 批量插入数据库
        for (Token token : tokens) {
            tokenMapper.insert(token);
        }
    }

    private void generateFCFSTokens(Integer activityId, List<ActivityPrize> activityPrizes, long startTime) {
        List<Token> tokens = new ArrayList<>();

        for (ActivityPrize activityPrize : activityPrizes) {
            Prize prize = prizeMapper.selectById(activityPrize.getPrizeId());
            if (prize == null || prize.getRemainingAmount() <= 0) {
                continue;
            }

            int amount = Math.min(activityPrize.getAmount(), prize.getRemainingAmount());

            for (int i = 0; i < amount; i++) {
                // 先到先得的令牌时间戳都是一样的，但是会按照插入顺序排序
                Token token = new Token();
                token.setActivityId(activityId);
                token.setPrizeId(prize.getId());
                token.setTokenTimestamp(startTime);
                token.setStatus(0);

                tokens.add(token);

                // 将令牌存入红黑树
                tokenTree.put(startTime + i, token.getId() + "_" + token.getPrizeId());
            }
        }

        // 批量插入数据库
        for (Token token : tokens) {
            tokenMapper.insert(token);
        }
    }

    @Override
    public Map<String, Object> getTokenDetails(Integer activityId) {
        Map<String, Object> result = new HashMap<>();

        // 获取数据库中的所有令牌信息
        LambdaQueryWrapper<Token> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Token::getActivityId, activityId);
        List<Token> allTokens = tokenMapper.selectList(queryWrapper);

        // 按状态分类令牌
        List<Token> unusedTokens = allTokens.stream()
                .filter(token -> token.getStatus() == 0)
                .collect(Collectors.toList());

        List<Token> usedTokens = allTokens.stream()
                .filter(token -> token.getStatus() == 1)
                .collect(Collectors.toList());

        // 获取每个令牌对应的奖品信息
        Map<Integer, Prize> prizeMap = new HashMap<>();
        unusedTokens.forEach(token -> {
            Prize prize = prizeMapper.selectById(token.getPrizeId());
            if (prize != null) {
                prizeMap.put(token.getId().intValue(), prize);
            }
        });

        // 汇总信息
        result.put("totalTokenCount", allTokens.size());
        result.put("unusedTokenCount", unusedTokens.size());
        result.put("usedTokenCount", usedTokens.size());

        // 令牌明细（仅显示未使用的令牌，避免数据过多）
        List<Map<String, Object>> tokenDetails = new ArrayList<>();
        for (Token token : unusedTokens) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("tokenId", token.getId());
            detail.put("timestamp", token.getTokenTimestamp());
            detail.put("prizeId", token.getPrizeId());

            Prize prize = prizeMap.get(token.getId().intValue());
            if (prize != null) {
                detail.put("prizeName", prize.getName());
                detail.put("prizePrice", prize.getPrice());
            }

            tokenDetails.add(detail);
        }

        result.put("tokens", tokenDetails);

        // 红黑树中的令牌情况（如果有访问方法）
        try {
            // 这里应该访问红黑树，但RedBlackTree类可能需要修改以支持遍历
            // 暂时返回一个占位符
            result.put("redBlackTreeTokens", "需要修改RedBlackTree类以支持遍历");
        } catch (Exception e) {
            log.error("获取红黑树令牌信息失败", e);
            result.put("redBlackTreeError", e.getMessage());
        }

        return result;
    }
}
