package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.dto.DrawRequest;
import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.Token;
import com.test.prizesystem.model.vo.DrawResponse;
import com.test.prizesystem.model.vo.PrizeVO;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.service.DrawService;
import com.test.prizesystem.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.test.prizesystem.service.UserService;

/**
 * 抽奖服务实现类
 * <p>
 * 实现抽奖逻辑，包括获取活动信息、获取令牌、获取奖品等。
 * 令牌直接包含奖品ID和名称，简化了抽奖过程。
 * 已简化工作流程，移除了统计服务依赖，聚焦于核心抽奖逻辑。
 * 
 * @author MCP生成
 * @version 4.0
 */
@Slf4j
@Service
public class DrawServiceImpl implements DrawService {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private UserService userService;

    // 使用UserService管理用户抽奖限制
    @Override
    public DrawResponse draw(DrawRequest request) {
        Integer userId = request.getUserId();
        Integer activityId = request.getActivityId();

        // 获取活动信息
        Activity activity = activityService.getActivity(activityId);
        if (activity == null) {
            return new DrawResponse(false, "活动不存在");
        }

        if (activity.getStatus() != 1) {
            return new DrawResponse(false, activity.getStatus() == 0 ? "活动未开始" : "活动已结束");
        }
        
        // 1. 先检查用户抽奖资格，不具备资格则直接返回，避免浪费令牌
        // 检查用户抽奖次数限制
        try {
            if (!userService.tryDraw(userId)) {
                return new DrawResponse(false, "抽奖次数已用完");
            }
        } catch (IllegalArgumentException e) {
            log.warn("抽奖错误：{}", e.getMessage());
            return new DrawResponse(false, e.getMessage());
        }
        
        // 2. 接着检查用户中奖资格，这里做预判断，避免浪费令牌
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
            long currentTime = System.currentTimeMillis() / 1000;
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
            
            return new DrawResponse(true, "恭喜中奖", prizeVO);
        } catch (Exception e) {
            log.error("抽奖过程发生异常", e);
            return new DrawResponse(false, "抽奖过程发生错误", null);
        }
    }
}