package com.test.prizesystem.controller;


import com.test.prizesystem.mapper.PrizeMapper;
import com.test.prizesystem.mapper.UserDrawRecordMapper;
import com.test.prizesystem.mapper.UserPrizeRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private UserDrawRecordMapper userDrawRecordMapper;

    @Autowired
    private UserPrizeRecordMapper userPrizeRecordMapper;

    @Autowired
    private PrizeMapper prizeMapper;

    @GetMapping
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 获取总抽奖次数
        Long totalDraws = userDrawRecordMapper.selectCount(null);

        // 获取总中奖次数
        Long totalWins = userPrizeRecordMapper.selectCount(null);

        // 计算中奖率
        double winRate = totalDraws > 0 ? (double) totalWins / totalDraws * 100 : 0;

        // 获取各奖品剩余数量
        Map<String, Integer> prizeRemaining = new HashMap<>();
        prizeMapper.selectList(null).forEach(prize ->
                prizeRemaining.put(prize.getName(), prize.getRemainingAmount())
        );

        stats.put("totalDraws", totalDraws);
        stats.put("totalWins", totalWins);
        stats.put("winRate", String.format("%.2f%%", winRate));
        stats.put("prizeRemaining", prizeRemaining);

        return stats;
    }
}
