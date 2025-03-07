package com.test.prizesystem.service.imp;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.test.prizesystem.mapper.ActivityPrizeMapper;
import com.test.prizesystem.mapper.PrizeMapper;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.vo.PrizeVO;
import com.test.prizesystem.service.PrizeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class PrizeServiceImpl implements PrizeService {

    @Autowired
    private PrizeMapper prizeMapper;

    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;

    @Override
    public List<Prize> getActivityPrizes(Integer activityId) {
        // 查询活动关联的奖品ID
        LambdaQueryWrapper<ActivityPrize> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActivityPrize::getActivityId, activityId);
        List<ActivityPrize> activityPrizes = activityPrizeMapper.selectList(queryWrapper);

        List<Prize> prizes = new ArrayList<>();
        for (ActivityPrize activityPrize : activityPrizes) {
            Prize prize = prizeMapper.selectById(activityPrize.getPrizeId());
            if (prize != null && prize.getRemainingAmount() > 0) {
                prizes.add(prize);
            }
        }

        return prizes;
    }

    @Override
    public Prize getPrize(Integer prizeId) {
        return prizeMapper.selectById(prizeId);
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
        return prizeMapper.decreaseRemainingAmount(prizeId) > 0;
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