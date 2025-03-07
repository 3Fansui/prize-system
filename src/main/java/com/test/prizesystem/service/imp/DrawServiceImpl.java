package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.dto.DrawRequest;
import com.test.prizesystem.model.entity.*;
import com.test.prizesystem.model.vo.DrawResponse;
import com.test.prizesystem.service.*;
import com.test.prizesystem.util.AsyncProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Random;

@Slf4j
@Service
public class DrawServiceImpl implements DrawService {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private PrizeService prizeService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserDrawRecordService userDrawRecordService;

    @Autowired
    private UserPrizeRecordService userPrizeRecordService;

    @Autowired
    private AsyncProcessor asyncProcessor;

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

        // 获取活动规则
        ActivityRule rule = activityService.getActivityRule(activityId, 0); // 默认用户级别为0

        // 检查用户抽奖次数
        int drawCount = userDrawRecordService.getUserDrawCountToday(userId, activityId);
        if (rule != null && rule.getMaxDrawsDaily() > 0 && drawCount >= rule.getMaxDrawsDaily()) {
            return new DrawResponse(false, "今日抽奖次数已用完");
        }

        // 检查用户中奖次数
        int winCount = userPrizeRecordService.getUserWinCountToday(userId, activityId);
        if (rule != null && rule.getMaxWinsDaily() > 0 && winCount >= rule.getMaxWinsDaily()) {
            return new DrawResponse(false, "今日中奖次数已达上限");
        }

        // 记录抽奖行为（异步）
        asyncProcessor.submitTask(() -> {
            UserDrawRecord record = new UserDrawRecord();
            record.setUserId(userId);
            record.setActivityId(activityId);
            record.setDrawTime(new Date());
            userDrawRecordService.addUserDrawRecord(record);
        });

        // 根据活动类型执行不同的抽奖逻辑
        Prize prize = null;
        switch (activity.getType()) {
            case 1: // 概率型
                prize = doProbabilityDraw(userId, activity);
                break;
            case 2: // 固定时间型
                prize = doTimedDraw(userId, activity);
                break;
            case 3: // 先到先得型
                prize = doFirstComeDraw(userId, activity);
                break;
            default:
                return new DrawResponse(false, "未知活动类型");
        }

// 处理抽奖结果
        if (prize != null) {
            log.info("用户 {} 在活动 {} 中奖，获得奖品: {} (ID: {})",
                    userId, activityId, prize.getName(), prize.getId());

            // 减少奖品数量
            boolean decreased = prizeService.decreasePrizeAmount(prize.getId());
            if (!decreased) {
                log.warn("奖品 {} 数量减少失败，可能已抽完", prize.getId());
                return new DrawResponse(true, "未中奖", null);
            }

            // 记录中奖信息（异步）
            final Prize finalPrize = prize;
            asyncProcessor.submitTask(() -> {
                log.info("异步记录中奖信息 - 用户: {}, 活动: {}, 奖品: {}",
                        userId, activityId, finalPrize.getName());

                UserPrizeRecord record = new UserPrizeRecord();
                record.setUserId(userId);
                record.setActivityId(activityId);
                record.setPrizeId(finalPrize.getId());
                record.setWinTime(new Date());
                userPrizeRecordService.addUserPrizeRecord(record);

                log.info("中奖记录保存成功 - 用户: {}, 活动: {}, 奖品: {}",
                        userId, activityId, finalPrize.getName());
            });

            return new DrawResponse(true, "恭喜中奖", prizeService.toPrizeVO(prize));
        } else {
            log.info("用户 {} 在活动 {} 未中奖", userId, activityId);
            return new DrawResponse(true, "未中奖", null);
        }
    }

    // 概率型抽奖
    private Prize doProbabilityDraw(Integer userId, Activity activity) {
        int probability = activity.getProbability() != null ? activity.getProbability() : 0;
        // 根据概率决定是否中奖
        if (new Random().nextInt(100) < probability) {
            // 随机选择一个奖品
            return prizeService.getRandomPrize(activity.getId());
        }
        return null;
    }

    // 固定时间型抽奖
    private Prize doTimedDraw(Integer userId, Activity activity) {
        // 获取当前时间戳
        long currentTime = System.currentTimeMillis();
        // 从令牌管理器获取当前时间有效的令牌
        Token token = tokenService.getAvailableToken(activity.getId(), currentTime);
        if (token != null) {
            return prizeService.getPrize(token.getPrizeId());
        }
        return null;
    }

    // 先到先得型抽奖
    private Prize doFirstComeDraw(Integer userId, Activity activity) {
        // 直接获取下一个令牌
        Token token = tokenService.getNextToken(activity.getId());
        if (token != null) {
            return prizeService.getPrize(token.getPrizeId());
        }
        return null;
    }
}