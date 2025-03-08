package com.test.prizesystem.service.imp;

import com.test.prizesystem.service.StatsService;
import com.test.prizesystem.model.dto.DrawRequest;
import com.test.prizesystem.model.entity.*;
import com.test.prizesystem.model.vo.DrawResponse;
import com.test.prizesystem.service.*;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.WinRecordProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

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
    private WinRecordProcessor winRecordProcessor;
    
    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    @Autowired
    private StatsService statsService;

    @Override
    public DrawResponse draw(DrawRequest request) {
        Integer userId = request.getUserId();
        Integer activityId = request.getActivityId();

        // 增加抽奖统计计数
        statsService.incrementDrawCount();

        // 获取活动信息
        Activity activity = activityService.getActivity(activityId);
        if (activity == null) {
            return new DrawResponse(false, "活动不存在");
        }

        if (activity.getStatus() != 1) {
            return new DrawResponse(false, activity.getStatus() == 0 ? "活动未开始" : "活动已结束");
        }

        // 获取活动规则
        ActivityRule rule = activityService.getActivityRule(activityId, 0);

        // 检查用户抽奖次数
        int drawCount = userDrawRecordService.getUserDrawCountToday(userId, activityId);
        if (rule != null && rule.getMaxDrawsDaily() > 0 && drawCount >= rule.getMaxDrawsDaily()) {
            return new DrawResponse(false, "今日抽奖次数已用完");
        }

        // 记录抽奖行为
        UserDrawRecord record = new UserDrawRecord();
        record.setUserId(userId);
        record.setActivityId(activityId);
        record.setDrawTime(new Date());
        
        // 将抽奖记录保存到红黑树
        long recordKey = userId * 1000000L + System.currentTimeMillis() % 1000000L;
        treeStorage.save("user_draw_records", recordKey, record);
        
        // 增加用户抽奖次数记录
        userDrawRecordService.addUserDrawRecord(record);

        // 执行固定时间型抽奖逻辑（根据要求，只保留一种抽奖方式）
        Prize prize = doTimedDraw(userId, activity);

        // 处理抽奖结果
        if (prize != null) {
            log.info("用户 {} 在活动 {} 中奖，获得奖品: {} (ID: {})",
                    userId, activityId, prize.getName(), prize.getId());

            // 增加中奖统计计数
            statsService.incrementWinCount();

            // 减少奖品数量
            boolean decreased = prizeService.decreasePrizeAmount(prize.getId());
            if (!decreased) {
                log.warn("奖品 {} 数量减少失败，可能已抽完", prize.getId());
                return new DrawResponse(true, "未中奖", null);
            }

            // 记录中奖信息（异步）
            UserPrizeRecord winRecord = new UserPrizeRecord();
            winRecord.setUserId(userId);
            winRecord.setActivityId(activityId);
            winRecord.setPrizeId(prize.getId());
            winRecord.setWinTime(new Date());
            
            // 将中奖记录提交到处理队列
            winRecordProcessor.submitRecord(winRecord);

            return new DrawResponse(true, "恭喜中奖", prizeService.toPrizeVO(prize));
        } else {
            log.info("用户 {} 在活动 {} 未中奖", userId, activityId);
            return new DrawResponse(true, "未中奖", null);
        }
    }

    // 固定时间型抽奖
    private Prize doTimedDraw(Integer userId, Activity activity) {
        try {
            // 获取当前时间戳（秒级）
            long currentTime = System.currentTimeMillis() / 1000;
            
            // 从令牌服务获取当前时间有效的令牌
            Token token = tokenService.getAvailableToken(activity.getId(), currentTime);
            
            if (token != null) {
                return prizeService.getPrize(token.getPrizeId());
            }
        } catch (Exception e) {
            log.error("抽奖过程发生异常", e);
        }
        
        return null;
    }
}