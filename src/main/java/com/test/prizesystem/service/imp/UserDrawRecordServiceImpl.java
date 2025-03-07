package com.test.prizesystem.service.imp;


import com.test.prizesystem.mapper.UserDrawRecordMapper;
import com.test.prizesystem.model.entity.UserDrawRecord;
import com.test.prizesystem.service.UserDrawRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserDrawRecordServiceImpl implements UserDrawRecordService {

    @Autowired
    private UserDrawRecordMapper userDrawRecordMapper;

    @Override
    public int getUserDrawCountToday(Integer userId, Integer activityId) {
        return userDrawRecordMapper.getDrawCountToday(userId, activityId);
    }

    @Override
    public boolean addUserDrawRecord(UserDrawRecord record) {
        return userDrawRecordMapper.insert(record) > 0;
    }
}