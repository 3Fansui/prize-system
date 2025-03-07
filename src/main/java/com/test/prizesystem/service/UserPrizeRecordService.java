package com.test.prizesystem.service;


import com.test.prizesystem.model.entity.UserPrizeRecord;

public interface UserPrizeRecordService {

    /**
     * 获取用户当天中奖次数
     */
    int getUserWinCountToday(Integer userId, Integer activityId);

    /**
     * 添加用户中奖记录
     */
    boolean addUserPrizeRecord(UserPrizeRecord record);
}