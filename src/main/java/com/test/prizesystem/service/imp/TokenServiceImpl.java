package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.entity.Token;
import com.test.prizesystem.service.TokenService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.TokenQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    @Autowired
    private TokenQueue tokenQueue;
    
    @Autowired
    private RedBlackTreeStorage treeStorage;

    @Override
    public void generateTokens(Activity activity, List<ActivityPrize> activityPrizes) {
        log.info("开始为活动 {} 生成令牌...", activity.getId());

        // 清空令牌队列
        tokenQueue.clear();
        
        long startTime = activity.getStartTime().getTime() / 1000; // 转为秒级时间戳
        long endTime = activity.getEndTime().getTime() / 1000;
        long duration = endTime - startTime;

        // 根据活动类型生成不同的令牌
        // 根据要求，我们只实现一种抽奖方式，这里用时间戳令牌
        generateTimedTokens(activity.getId(), activityPrizes, startTime, duration);
        
        log.info("令牌生成完成，总数: {}", tokenQueue.size());
    }
    
    private void generateTimedTokens(Integer activityId, List<ActivityPrize> activityPrizes, long startTime, long duration) {
        // 计算总令牌数量
        int totalTokens = 0;
        for (ActivityPrize activityPrize : activityPrizes) {
            totalTokens += activityPrize.getAmount();
        }

        // 生成均匀分布的令牌
        long interval = duration / (totalTokens > 0 ? totalTokens : 1);

        for (ActivityPrize activityPrize : activityPrizes) {
            int amount = activityPrize.getAmount();

            for (int i = 0; i < amount; i++) {
                // 生成时间均匀分布的时间戳，加上一些随机性
                long tokenTimestamp = startTime + i * interval + (long)(Math.random() * interval * 0.8);
                
                // 生成令牌并添加到队列
                Token token = tokenQueue.generateToken(activityId, activityPrize.getPrizeId(), tokenTimestamp);
                
                // 也将令牌存储到红黑树中（作为备份）
                treeStorage.save("tokens", token.getTokenTimestamp(), token);
                
                log.debug("生成令牌: 活动ID={}, 奖品ID={}, 时间戳={}",
                        activityId, activityPrize.getPrizeId(), tokenTimestamp);
            }
        }
    }

    @Override
    public Token getAvailableToken(Integer activityId, long timestamp) {
        // 从队列中获取可用令牌
        Token token = tokenQueue.getAvailableToken(timestamp);
        
        if (token != null) {
            log.debug("找到可用令牌: id={}, timestamp={}", token.getId(), token.getTokenTimestamp());
            // 标记令牌为已使用
            if (token.getStatus() == 0) {
                token.setStatus(1);
            }
        }
        
        return token;
    }

    @Override
    public boolean useToken(Long tokenId) {
        // 这个方法现在不需要了，因为我们在getAvailableToken时已经处理了令牌状态
        // 保留方法签名以兼容接口
        return true;
    }

    @Override
    public Map<String, Object> getTokenDetails(Integer activityId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取队列大小
        int queueSize = tokenQueue.size();
        
        result.put("totalTokens", queueSize);
        result.put("activityId", activityId);
        
        return result;
    }
}