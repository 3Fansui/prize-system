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
 * 负责管理活动奖品令牌的完整生命周期，包括令牌的生成、存储、获取和消费等操作。
 * 主要功能点：
 * <ul>
 *   <li>令牌生成：根据活动时间范围和奖品列表，生成带有时间戳的奖品令牌</li>
 *   <li>时间戳策略：采用随机排序+均匀分布算法，确保奖品在活动期间内分布均衡</li>
 *   <li>令牌获取：根据时间戳获取可用的奖品令牌，确保时序性和公平性</li>
 *   <li>队列管理：为每个活动维护独立的令牌队列，支持并发处理</li>
 * </ul>
 * 令牌直接包含奖品信息，简化了抽奖过程中的数据访问。系统使用红黑树存储令牌相关数据，
 * 组合使用响应式队列，保证了高并发情况下的性能和一致性。
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

    /**
     * 生成活动奖品令牌
     * <p>
     * 根据活动信息和奖品配置，生成活动奖品令牌。整个生成过程分为以下步骤：
     * <ol>
     *   <li>清空当前活动的令牌队列，避免重复生成</li>
     *   <li>获取活动的开始时间和结束时间，计算活动时间范围</li>
     *   <li>收集所有奖品信息，并为每个奖品生成对应数量的令牌数据</li>
     *   <li>对所有令牌数据进行随机排序，打乱奖品顺序</li>
     *   <li>根据活动时间范围和令牌在列表中的位置，计算均匀分布的时间戳</li>
     *   <li>添加小幅度随机偏移，避免完全均匀导致的计算峰值</li>
     *   <li>生成最终的令牌，并添加到对应活动的队列中</li>
     * </ol>
     * 这种令牌生成策略确保了奖品在活动全过程中的均匀分布，并且避免了可预测性。
     * 
     * @param activity 活动信息，包含ID、时间范围等
     * @param activityPrizes 活动关联的奖品列表
     * @author wu
     */
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
     * 用于令牌生成过程的数据类
     * <p>
     * 该内部类存储令牌生成过程中的临时数据，包括奖品信息和用于排序的随机值。
     * 顺序先生成随机值，然后根据随机值排序，最后根据排序位置分配时间戳。
     * 这个过程确保了奖品在时间轴上的随机分布。
     * 
     * @author wu
     */
    private static class TokenData {
        Prize prize;
        double randomValue; // 用于排序的随机值
    }

    /**
     * 获取可用的奖品令牌
     * <p>
     * 根据活动ID和当前时间戳，获取当前可用的奖品令牌。
     * 可用令牌的定义是：令牌的时间戳小于等于当前时间戳的令牌。
     * 这确保了奖品的发放严格按照预定的时间分布进行。
     * <p>
     * 使用时间戳作为筛选条件的好处是：
     * <ul>
     *   <li>奖品分布更加均匀，避免短时间内的中奖峰值</li>
     *   <li>可以控制活动期间奖品发放的节奏</li>
     *   <li>确保所有参与者对同一个时间点的奖品有相同的机会</li>
     * </ul>
     * 
     * @param activityId 活动ID
     * @param timestamp 当前时间戳（秒级）
     * @return 可用的奖品令牌，如果没有则返回null
     * @author wu
     */
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
    


    /**
     * 获取令牌详细信息
     * <p>
     * 获取指定活动的令牌队列详细信息，主要用于管理界面展示和调试。
     * 返回的数据包括：
     * <ul>
     *   <li>总令牌数量：活动中还未被消费的令牌总数</li>
     *   <li>活动ID：当前查询的活动标识</li>
     *   <li>令牌样本：队列中最多20个令牌的详细信息</li>
     * </ul>
     * 每个令牌信息包含活动ID、奖品ID、奖品名称、令牌时间戳和格式化的抽奖时间。
     * 格式化的抽奖时间使用中国时区（UTC+8）进行计算。
     * 
     * @param activityId 活动ID
     * @return 包含令牌队列详细信息的Map
     * @author wu
     */
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
    
    /**
     * 获取令牌队列大小
     * <p>
     * 返回指定活动的令牌队列中剩余的令牌数量。
     * 这个方法主要用于：
     * <ul>
     *   <li>抽奖前判断活动是否还有奖品令牌可用</li>
     *   <li>统计和监控奖品的剩余数量</li>
     *   <li>管理界面展示奖品发放进度</li>
     * </ul>
     * 令牌队列大小为0表示奖品已全部发放完毕。
     * 
     * @param activityId 活动ID
     * @return 令牌队列中剩余的令牌数量
     * @author wu
     */
    @Override
    public int getTokenQueueSize(Integer activityId) {
        // 从指定活动的令牌队列获取大小
        return tokenQueue.size(activityId);
    }
}