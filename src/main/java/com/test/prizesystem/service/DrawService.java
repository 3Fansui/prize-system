package com.test.prizesystem.service;


import com.test.prizesystem.model.dto.DrawRequest;
import com.test.prizesystem.model.entity.User;
import com.test.prizesystem.model.vo.DrawResponse;

import java.util.List;

public interface DrawService {

    /**
     * 执行抽奖
     * @param request 抽奖请求
     * @return 抽奖响应
     */
    DrawResponse draw(DrawRequest request);
    
    /**
     * 预热用户缓存
     * @param users 需要预热的用户列表
     */
    void preloadUserCache(List<User> users);
}