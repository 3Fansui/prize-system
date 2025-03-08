package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.service.PrizeService;
import com.test.prizesystem.service.StatsService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统计服务实现类
 * <p>
 * 实现系统抽奖统计功能，维护抽奖次数、中奖次数等统计数据。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    @Autowired
    private PrizeService prizeService;
    
    // 用于统计总抽奖次数和中奖次数
    private final AtomicInteger totalDraws = new AtomicInteger(0);
    private final AtomicInteger totalWins = new AtomicInteger(0);

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 获取总抽奖次数和总中奖次数
        stats.put("totalDraws", totalDraws.get());
        stats.put("totalWins", totalWins.get());

        // 计算中奖率
        double winRate = totalDraws.get() > 0 ? 
                (double) totalWins.get() / totalDraws.get() * 100 : 0;

        // 获取各奖品剩余数量
        Map<String, Integer> prizeRemaining = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            Prize prize = prizeService.getPrize(i);
            if (prize != null) {
                prizeRemaining.put(prize.getName(), prize.getRemainingAmount());
            }
        }

        stats.put("winRate", String.format("%.2f%%", winRate));
        stats.put("prizeRemaining", prizeRemaining);

        return stats;
    }
    
    @Override
    public void incrementDrawCount() {
        totalDraws.incrementAndGet();
    }
    
    @Override
    public void incrementWinCount() {
        totalWins.incrementAndGet();
    }
}
