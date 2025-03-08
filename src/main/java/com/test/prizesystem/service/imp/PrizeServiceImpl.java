package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.vo.PrizeVO;
import com.test.prizesystem.service.PrizeService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class PrizeServiceImpl implements PrizeService {

    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    // 使用内存中的Map存储奖品剩余数量，提高性能
    private final ConcurrentHashMap<Integer, AtomicInteger> prizeRemainingMap = new ConcurrentHashMap<>();
    
    // 红黑树存储的名称
    private static final String PRIZE_TREE = "prizes";
    private static final String ACTIVITY_PRIZE_TREE = "activity_prizes";

    @Override
    public List<Prize> getActivityPrizes(Integer activityId) {
        // 这里简化实现，直接从初始化数据中获取
        // 在实际应用中，应该从红黑树中查询
        List<Prize> prizes = new ArrayList<>();
        
        // 我们假设活动ID为1的有3个奖品
        if (activityId == 1) {
            Prize prize1 = getPrize(1);
            Prize prize2 = getPrize(2);
            Prize prize3 = getPrize(3);
            
            if (prize1 != null && getRemainingAmount(1) > 0) prizes.add(prize1);
            if (prize2 != null && getRemainingAmount(2) > 0) prizes.add(prize2);
            if (prize3 != null && getRemainingAmount(3) > 0) prizes.add(prize3);
        }
        
        return prizes;
    }

    @Override
    public Prize getPrize(Integer prizeId) {
        // 从红黑树存储中查询奖品
        Prize prize = treeStorage.find(PRIZE_TREE, prizeId, Prize.class);
        
        // 如果没找到，使用硬编码的演示数据（仅供演示）
        if (prize == null && prizeId <= 3) {
            prize = getDefaultPrize(prizeId);
            // 保存到红黑树
            treeStorage.save(PRIZE_TREE, prizeId, prize);
        }
        
        return prize;
    }
    
    // 获取默认的演示奖品数据
    private Prize getDefaultPrize(Integer prizeId) {
        Prize prize = new Prize();
        prize.setId(prizeId);
        
        switch (prizeId) {
            case 1:
                prize.setName("iPhone 14");
                prize.setTotalAmount(50);
                prize.setRemainingAmount(50);
                prize.setImageUrl("https://example.com/images/iphone14.jpg");
                break;
            case 2:
                prize.setName("AirPods");
                prize.setTotalAmount(100);
                prize.setRemainingAmount(100);
                prize.setImageUrl("https://example.com/images/airpods.jpg");
                break;
            case 3:
                prize.setName("小米手环");
                prize.setTotalAmount(200);
                prize.setRemainingAmount(200);
                prize.setImageUrl("https://example.com/images/miband.jpg");
                break;
        }
        
        // 初始化剩余数量缓存
        prizeRemainingMap.putIfAbsent(prizeId, new AtomicInteger(prize.getRemainingAmount()));
        
        return prize;
    }

    @Override
    public Prize getRandomPrize(Integer activityId) {
        List<Prize> prizes = getActivityPrizes(activityId);
        if (prizes.isEmpty()) {
            return null;
        }

        // 随机选择一个奖品
        return prizes.get(new Random().nextInt(prizes.size()));
    }

    @Override
    public boolean decreasePrizeAmount(Integer prizeId) {
        // 从内存Map中获取或初始化剩余数量
        AtomicInteger remaining = prizeRemainingMap.computeIfAbsent(prizeId, k -> {
            Prize prize = getPrize(k);
            return new AtomicInteger(prize != null ? prize.getRemainingAmount() : 0);
        });
        
        // 原子减少操作
        int newValue = remaining.decrementAndGet();
        
        // 如果减到小于0，恢复并返回失败
        if (newValue < 0) {
            remaining.incrementAndGet();
            return false;
        }
        
        // 更新红黑树中的奖品信息
        Prize prize = getPrize(prizeId);
        if (prize != null) {
            prize.setRemainingAmount(newValue);
            treeStorage.save(PRIZE_TREE, prizeId, prize);
            log.debug("奖品{}剩余数量更新为{}", prizeId, newValue);
        }
        
        return true;
    }
    
    // 获取奖品剩余数量
    private int getRemainingAmount(Integer prizeId) {
        AtomicInteger remaining = prizeRemainingMap.get(prizeId);
        if (remaining != null) {
            return remaining.get();
        }
        
        Prize prize = getPrize(prizeId);
        if (prize != null) {
            AtomicInteger count = new AtomicInteger(prize.getRemainingAmount());
            prizeRemainingMap.put(prizeId, count);
            return count.get();
        }
        
        return 0;
    }

    @Override
    public PrizeVO toPrizeVO(Prize prize) {
        if (prize == null) {
            return null;
        }

        PrizeVO vo = new PrizeVO();
        BeanUtils.copyProperties(prize, vo);
        return vo;
    }
}