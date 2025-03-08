package com.test.prizesystem.controller;


import com.test.prizesystem.model.dto.DrawRequest;
import com.test.prizesystem.model.vo.DrawResponse;
import com.test.prizesystem.service.DrawService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 抽奖控制器
 * <p>
 * 该控制器处理所有与抽奖相关的请求，包括用户参与抽奖、获取抽奖结果等功能。
 * 抽奖流程由底层服务实现，包括资格验证、奖品发放策略的执行等。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/draw")
@Api(tags = "抽奖接口", description = "处理用户参与抽奖的相关操作")
public class DrawController {

    @Autowired
    private DrawService drawService;


    /**
     * 执行抽奖
     * <p>
     * 处理用户的抽奖请求，包括用户认证、活动验证、抽奖资格检查和奖品发放。
     * 抽奖结果由服务层基于业务规则和概率计算生成。
     * 
     * @param request 抽奖请求对象，包含用户ID、活动ID等信息
     * @return 抽奖结果响应，包含是否中奖、奖品信息等
     */
    @PostMapping
    @ApiOperation(value = "执行抽奖", notes = "用户参与抽奖活动并获取抽奖结果")
    public DrawResponse draw(@ApiParam(value = "抽奖请求参数", required = true) @RequestBody DrawRequest request) {
        log.info("收到抽奖请求: {}", request);
        DrawResponse response = drawService.draw(request);
        log.info("抽奖结果: {}", response);
        return response;
    }
}