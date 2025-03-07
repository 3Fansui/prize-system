package com.test.prizesystem.service.imp;


import com.test.prizesystem.mapper.UserPrizeRecordMapper;
import com.test.prizesystem.model.entity.UserPrizeRecord;
import com.test.prizesystem.service.UserPrizeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserPrizeRecordServiceImpl implements UserPrizeRecordService {

    @Autowired
    private UserPrizeRecordMapper userPrizeRecordMapper;

    @Override
    public int getUserWinCountToday(Integer userId, Integer activityId) {
        return userPrizeRecordMapper.getWinCountToday(userId, activityId);
    }

    @Override
    public boolean addUserPrizeRecord(UserPrizeRecord record) {
        return userPrizeRecordMapper.insert(record) > 0;
    }
}