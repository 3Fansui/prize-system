package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.entity.Token;
import com.test.prizesystem.service.TokenService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.TokenQueue;
import com.test.prizesystem.util.TreeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.TimeZone;
import java.time.ZoneId;

/**
 * 令牌服务实现类
 * <p>
 * 管理令牌的生成、获取和返回等操作。令牌直接包含奖品信息，
 * 简化了抽奖过程中的数据访问。系统为每个活动维护独立的令牌队列，
 * 并使用改进的时间戳生成算法确保奖品分布更加均匀。
 * 
 * @author wu
 * @version 5.0
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    @Autowired
    private TokenQueue tokenQueue;
    
    @Autowired
    private RedBlackTreeStorage treeStorage;

    @Override
    public void generateTokens(Activity activity, List<ActivityPrize> activityPrizes) {
        Integer activityId = activity.getId();
        log.info("开始为活动 {} 生成令牌...", activityId);

        // 清空当前活动的令牌队列
        tokenQueue.clear(activityId);
        
        // 确保使用UTC+8时区处理时间
        TimeZone chinaTimeZone = TimeZone.getTimeZone("Asia/Shanghai");
        
        // 获取活动开始和结束时间（秒级时间戳）
        Calendar calendarStart = Calendar.getInstance(chinaTimeZone);
        calendarStart.setTime(activity.getStartTime());
        long startTime = calendarStart.getTimeInMillis() / 1000;
        
        Calendar calendarEnd = Calendar.getInstance(chinaTimeZone);
        calendarEnd.setTime(activity.getEndTime());
        long endTime = calendarEnd.getTimeInMillis() / 1000;
        
        long duration = endTime - startTime;

        log.info("活动时间范围: {} 秒 (从 {} 到 {})", duration, 
                new Date(startTime * 1000), new Date(endTime * 1000));

        // 收集所有需要生成的令牌数据
        List<TokenData> tokenDataList = new ArrayList<>();
        int totalTokens = 0;
        
        // 第一步：收集所有奖品的令牌数据
        for (ActivityPrize activityPrize : activityPrizes) {
            int amount = activityPrize.getAmount();
            Prize prize = treeStorage.find(TreeNames.PRIZES, activityPrize.getPrizeId(), Prize.class);
            
            if (prize == null) {
                log.warn("未找到奖品信息: ID={}", activityPrize.getPrizeId());
                continue;
            }

            log.info("为活动 {} 的奖品 {} 准备生成 {} 个令牌", activityId, prize.getName(), amount);
            
            for (int i = 0; i < amount; i++) {
                TokenData tokenData = new TokenData();
                tokenData.prize = prize;
                // 临时存储一个随机值，用于后续排序
                tokenData.randomValue = Math.random();
                tokenDataList.add(tokenData);
                totalTokens++;
            }
        }
        
        log.info("活动 {} 总共需生成 {} 个令牌", activityId, totalTokens);
        
        // 第二步：根据随机值排序，确保令牌分布均匀
        Collections.sort(tokenDataList, Comparator.comparingDouble(data -> data.randomValue));
        
        // 第三步：均匀分配时间戳并生成令牌
        for (int i = 0; i < tokenDataList.size(); i++) {
            TokenData data = tokenDataList.get(i);
            
            // 根据令牌在排序后的位置计算时间戳
            // 这样确保了令牌时间戳在整个活动期间内均匀分布
            double position = (double) i / tokenDataList.size();
            long tokenTimestamp = startTime + (long)(position * duration);
            
            // 添加少量随机偏移，避免完全均匀导致的峰值
            long randomJitter = (long)(Math.random() * 20) - 10; // ±10秒的随机偏移
            tokenTimestamp = Math.max(startTime, Math.min(endTime, tokenTimestamp + randomJitter));
            
            // 生成令牌并添加到活动对应的队列
            Token token = tokenQueue.generateToken(activityId, data.prize, tokenTimestamp);
            
            if (log.isDebugEnabled() && i % 100 == 0) { // 减少日志输出
                log.debug("生成令牌[{}/{}]: 活动ID={}, 奖品={}, 时间戳={} ({})",
                        i+1, totalTokens, activityId, token.getPrizeName(), 
                        tokenTimestamp, new Date(tokenTimestamp * 1000));
            }
        }
        
        log.info("活动 {} 的令牌生成完成，总数: {}", activityId, tokenQueue.size(activityId));
    }
    
    /**
     * 用于临时存储令牌生成数据的内部类
     */
    private static class TokenData {
        Prize prize;
        double randomValue; // 用于排序的随机值
    }

    @Override
    public Token getAvailableToken(Integer activityId, long timestamp) {
        // 从指定活动的队列中获取可用令牌（时间戳小于等于当前时间的令牌）
        Token token = tokenQueue.getAvailableToken(activityId, timestamp);
        
        if (token != null) {
            log.debug("找到可用令牌: 活动ID={}, 奖品名称={}, 时间戳={} ({})", 
                    activityId, token.getPrizeName(), 
                    token.getTokenTimestamp(), new Date(token.getTokenTimestamp() * 1000));
        } else {
            log.debug("活动 {} 在时间戳 {} 没有可用令牌", activityId, timestamp);
        }
        
        return token;
    }
    
    @Override
    public void returnToken(Token token) {
        tokenQueue.returnToken(token);
    }

    @Override
    public Map<String, Object> getTokenDetails(Integer activityId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取指定活动的队列大小
        int queueSize = tokenQueue.size(activityId);
        
        result.put("totalTokens", queueSize);
        result.put("activityId", activityId);
        
        // 获取所有令牌进行展示（限前20个令牌）
        List<Token> sampleTokens = tokenQueue.getTokens(activityId, 20);
        List<Map<String, Object>> tokenInfos = new ArrayList<>();
        
        for (Token token : sampleTokens) {
            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("activityId", activityId);
            tokenInfo.put("prizeId", token.getPrizeId());
            tokenInfo.put("prizeName", token.getPrizeName());
            // 将时间戳字段改为更有意义的名称
            tokenInfo.put("tokenTimestamp", token.getTokenTimestamp());
            
            // 添加格式化的日期时间，转换为毫秒级的Date对象
            Date tokenDate = new Date(token.getTokenTimestamp() * 1000);
            
            // 添加可读性更高的格式化字符串（中国时区）
            // 使用UTC+8时区
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
            calendar.setTime(tokenDate);
            String formattedDateTime = String.format("%d-%02d-%02d %02d:%02d:%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND));
            
            tokenInfo.put("drawTime", formattedDateTime);
            tokenInfos.add(tokenInfo);
        }
        
        result.put("tokens", tokenInfos);
        
        return result;
    }
    
    @Override
    public int getTokenQueueSize(Integer activityId) {
        // 从指定活动的令牌队列获取大小
        return tokenQueue.size(activityId);
    }
}