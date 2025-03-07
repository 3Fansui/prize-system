package com.test.prizesystem.controller;


import com.test.prizesystem.model.dto.DrawRequest;
import com.test.prizesystem.model.vo.DrawResponse;
import com.test.prizesystem.service.DrawService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/draw")
public class DrawController {

    @Autowired
    private DrawService drawService;


    /**
     * 执行抽奖
     */
    @PostMapping
    public DrawResponse draw(@RequestBody DrawRequest request) {
        log.info("收到抽奖请求: {}", request);
        DrawResponse response = drawService.draw(request);
        log.info("抽奖结果: {}", response);
        return response;
    }
}