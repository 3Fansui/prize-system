package com.test.prizesystem.service;


import com.test.prizesystem.model.entity.UserDrawRecord;

public interface UserDrawRecordService {

    /**
     * 获取用户当天抽奖次数
     */
    int getUserDrawCountToday(Integer userId, Integer activityId);

    /**
     * 添加用户抽奖记录
     */
    boolean addUserDrawRecord(UserDrawRecord record);
}
