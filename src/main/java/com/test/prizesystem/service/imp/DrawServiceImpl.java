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
        
        // 检查用户抽奖次数限制
        if (!userService.tryDraw(userId)) {
            return new DrawResponse(false, "抽奖次数已用完");
        }
        
        // 直接获取活动的令牌队列大小
        int tokenCount = tokenService.getTokenQueueSize(activityId);
        
        // 如果令牌队列为空，直接返回奖品已抽完
        if (tokenCount == 0) {
            log.info("活动 {} 的奖品已全部抽完", activityId);
            return new DrawResponse(true, "奖品已抽完", null);
        }

        try {
            // 获取当前时间戳（秒级）
            long currentTime = System.currentTimeMillis() / 1000;
            
            // 从令牌服务获取当前时间有效的令牌
            Token token = tokenService.getAvailableToken(activity.getId(), currentTime);
            
            if (token != null) {
                // 判断是否可以中奖（预检查，避免奖品浪费）
                boolean canWin = userService.tryWin(userId);
                
                // 如果用户无法中奖（已达到上限），则将令牌放回队列
                if (!canWin) {
                    log.info("用户 {} 已达到最大中奖次数，令牌返回队列", userId);
                    tokenService.returnToken(token);
                    return new DrawResponse(true, "未中奖", null);
                }
                
                // 用户中奖，返回奖品信息
                log.info("用户 {} 在活动 {} 中奖，获得奖品: {} (ID: {})",
                        userId, activityId, token.getPrizeName(), token.getPrizeId());
                
                // 构建奖品视图对象返回
                PrizeVO prizeVO = new PrizeVO();
                prizeVO.setId(token.getPrizeId());
                prizeVO.setName(token.getPrizeName());
                
                return new DrawResponse(true, "恭喜中奖", prizeVO);
            } else {
                log.info("活动 {} 当前无可用令牌，用户 {} 未中奖", activityId, userId);
                return new DrawResponse(true, "未中奖", null);
            }
        } catch (Exception e) {
            log.error("抽奖过程发生异常", e);
            return new DrawResponse(false, "抽奖过程发生错误", null);
        }
    }
}