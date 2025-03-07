package com.test.prizesystem.service;


import com.test.prizesystem.model.dto.DrawRequest;
import com.test.prizesystem.model.vo.DrawResponse;

public interface DrawService {

    /**
     * 执行抽奖
     */
    DrawResponse draw(DrawRequest request);
}
