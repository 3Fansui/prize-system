package com.test.prizesystem.service.imp;

import com.test.prizesystem.async.EventQueue;
import com.test.prizesystem.async.UserDrawEvent;
import com.test.prizesystem.model.dto.DrawRequest;
import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.Token;
import com.test.prizesystem.model.entity.User;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.TreeNames;
import com.test.prizesystem.model.vo.DrawResponse;
import com.test.prizesystem.model.vo.PrizeVO;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.service.DrawService;
import com.test.prizesystem.service.TokenService;
import com.test.prizesystem.service.UserService;
import com.test.prizesystem.util.PersistenceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抽奖服务实现类
 * <p>
 * 实现抽奖核心业务逻辑，包括用户资格验证、令牌获取、奖品发放等。
 * 主要功能点：
 * <ul>
 *   <li>用户信息缓存管理：系统启动时预热用户数据，提高抽奖效率</li>
 *   <li>抽奖资格验证：包括活动有效性、用户抽奖次数、中奖次数等验证</li>
 *   <li>奖品令牌管理：根据时间戳获取可用令牌，确保奖品分发公平性</li>
 *   <li>中奖事件异步处理：通过队列异步处理中奖记录，提高系统吞吐量</li>
 * </ul>
 * 该服务采用令牌机制代替传统的机率计算，提高了效率和公平性。
 * 令牌直接包含奖品信息，简化了抽奖过程中的数据访问。
 * 
 * @author wu
 * @version 9.0
 */
@Slf4j
@Service
public class DrawServiceImpl implements DrawService {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EventQueue eventQueue;
    
    @Autowired
    private PersistenceManager persistenceManager;

    // 用户信息缓存 - 系统预热时已填充
    private final Map<Integer, User> userCache = new ConcurrentHashMap<>();

    /**
     * 执行抽奖操作
     * <p>
     * 根据抽奖请求执行完整的抽奖流程。整个抽奖过程分为以下几个步骤：
     * <ol>
     *   <li>参数检验与活动验证：确保用户ID、活动ID有效，并且活动状态为进行中</li>
     *   <li>用户验证：从缓存中获取用户信息，如果缓存未命中则动态加载</li>
     *   <li>抽奖资格验证：检查用户是否还有抽奖次数，如果用完则直接返回</li>
     *   <li>中奖资格验证：检查用户是否已达到中奖上限，如果达到则直接返回未中奖</li>
     *   <li>检查奖品余量：通过令牌队列大小检查奖品是否还有剩余</li>
     *   <li>获取令牌：根据当前时间戳获取可用的奖品令牌</li>
     *   <li>生成中奖结果：如果获得令牌，则根据令牌信息生成中奖结果</li>
     *   <li>异步处理中奖记录：通过队列异步处理中奖事件，减少响应时间</li>
     * </ol>
     * 整个抽奖过程采用各种验证和预检查策略，确保系统的效率和稳定性。
     * 在出错情况下提供正确的错误信息并筛选异常。
     *
     * @param request 抽奖请求对象，包含用户ID和活动ID
     * @return 抽奖响应对象，包含是否成功、消息和奖品信息
     * @author wu
     */
    @Override
    public DrawResponse draw(DrawRequest request) {
        Integer userId = request.getUserId();
        Integer activityId = request.getActivityId();

        if (userId == null || activityId == null) {
            return new DrawResponse(false, "用户ID或活动ID不能为空");
        }

        // 获取活动信息
        Activity activity = activityService.getActivity(activityId);
        if (activity == null) {
            return new DrawResponse(false, "活动不存在");
        }

        if (activity.getStatus() != 1) {
            return new DrawResponse(false, activity.getStatus() == 0 ? "活动未开始" : "活动已结束");
        }
        
        // 从缓存中获取用户信息，预热时已填充所以不应该未命中
        User user = userCache.get(userId);
        if (user == null) {
            user = userService.getUser(userId);
            if (user == null) {
                return new DrawResponse(false, "用户不存在");
            }
            // 将用户信息放入缓存
            userCache.put(userId, user);
            log.warn("用户 {} 未在预热时加载到缓存，已动态加载", userId);
        }
        
        // 1. 检查用户抽奖资格，不具备资格则直接返回，避免浪费令牌
        try {
            if (!userService.tryDraw(userId)) {
                return new DrawResponse(false, "抽奖次数已用尽");
            }
        } catch (IllegalArgumentException e) {
            log.warn("抽奖错误：{}", e.getMessage());
            return new DrawResponse(false, e.getMessage());
        }
        
        // 2. 检查用户中奖资格，这里做预判断，避免浪费令牌
        boolean canWin = false;
        try {
            canWin = userService.tryWin(userId);
            if (!canWin) {
                log.info("用户 {} 已达到最大中奖上限，不再消费令牌", userId);
                return new DrawResponse(true, "未中奖", null);
            }
        } catch (IllegalArgumentException e) {
            log.warn("检查中奖资格错误：{}", e.getMessage());
            return new DrawResponse(false, e.getMessage());
        }
        
        // 3. 检查令牌队列是否为空
        int tokenCount = tokenService.getTokenQueueSize(activityId);
        if (tokenCount == 0) {
            log.info("活动 {} 的奖品已全部抽完", activityId);
            return new DrawResponse(true, "奖品已抽完", null);
        }

        try {
            // 4. 获取当前时间戳（秒级）并尝试获取可用的令牌
            // 使用UTC+8时区
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
            long currentTime = calendar.getTimeInMillis() / 1000;
            Token token = tokenService.getAvailableToken(activity.getId(), currentTime);
            
            // 5. 如果没有可用令牌，说明当前时间戳还没有可用令牌
            if (token == null) {
                log.info("活动 {} 当前时间的令牌已用完，用户 {} 未中奖", activityId, userId);
                return new DrawResponse(true, "未中奖", null);
            }
            
            // 6. 此时用户一定可以中奖，因为前面已经检查了用户中奖资格
            // 用户中奖，返回奖品信息
            log.info("用户 {} 在活动 {} 中奖，获得奖品: {} (ID: {})",
                    userId, activityId, token.getPrizeName(), token.getPrizeId());
            
            // 构建奖品视图对象返回
            PrizeVO prizeVO = new PrizeVO();
            prizeVO.setId(token.getPrizeId());
            prizeVO.setName(token.getPrizeName());
            
            // 使用异步方式通过双锁阻塞队列发送中奖事件
            UserDrawEvent winEvent = new UserDrawEvent();
            winEvent.setUserId(userId);
            winEvent.setActivityId(activityId);
            winEvent.setTimestamp(Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).getTimeInMillis());
            winEvent.setPrizeId(token.getPrizeId());
            winEvent.setPrizeName(token.getPrizeName());
            
            // 异步发送到队列
            boolean offered = eventQueue.offer(winEvent);
            if (offered) {
                log.info("异步提交中奖事件成功: 用户ID={}, 奖品={}", userId, token.getPrizeName());
            } else {
                log.error("异步提交中奖事件失败: 用户ID={}, 奖品={}", userId, token.getPrizeName());
            }
            
            return new DrawResponse(true, "恭喜中奖", prizeVO);
        } catch (Exception e) {
            log.error("抽奖过程发生异常", e);
            return new DrawResponse(false, "抽奖过程发生错误", null);
        }
    }
    
    /**
     * 预热用户缓存
     * <p>
     * 将用户信息预加载到内存缓存中，以提高抽奖时的数据访问效率。
     * 这个过程通常在活动预热阶段被调用，确保活动开始时系统已加载了全部用户数据。
     * 预热时会对用户数据进行有效性检查，确保只加载有效用户。
     *
     * @param users 需要预热的用户列表
     * @author wu
     */
    @Override
    public void preloadUserCache(List<User> users) {
        if (users == null || users.isEmpty()) {
            log.warn("用户列表为空，无法进行预热");
            return;
        }
        
        int count = 0;
        for (User user : users) {
            if (user != null && user.getId() != null) {
                userCache.put(user.getId(), user);
                count++;
            }
        }
        log.info("已预热 {} 个用户信息到缓存", count);
    }
    
    /**
     * 清除用户缓存
     * <p>
     * 清除内存中的用户缓存信息。可选择清除指定用户的缓存或清除所有用户缓存。
     * 当用户信息发生变化时，可以调用此方法清除缓存，强制下次访问时重新加载。
     * 这是一个内部方法，不对外暴露，避免执行不必要的缓存清除。
     *
     * @param userId 用户ID，如果为null则清除所有用户缓存
     * @author wu
     */
    private void clearUserCache(Integer userId) {
        if (userId == null) {
            userCache.clear();
            log.info("已清除所有用户缓存");
        } else {
            userCache.remove(userId);
            log.info("已清除用户 {} 的缓存", userId);
        }
    }
}