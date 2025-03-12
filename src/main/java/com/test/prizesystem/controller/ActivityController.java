package com.test.prizesystem.controller;

import com.test.prizesystem.model.vo.ActivityInfoVO;
import com.test.prizesystem.service.ActivityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动控制器
 * <p>
 * 提供活动信息查询相关的API接口，包括活动基本信息和奖品信息。
 * 
 * @author wu
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/activity")
@Api(tags = "活动接口", description = "提供活动信息查询相关接口")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    /**
     * 获取活动详细信息
     * <p>
     * 返回活动基本信息及其关联的奖品列表和数量
     * 
     * @param activityId 活动ID
     * @return 活动详细信息，包括ID、名称和奖品信息
     */
    @GetMapping("/{activityId}")
    @ApiOperation(value = "获取活动信息", notes = "返回活动的详细信息，包括ID、名称、奖品数量等")
    public ResponseEntity<ActivityInfoVO> getActivityInfo(
            @ApiParam(value = "活动ID", required = true, example = "1")
            @PathVariable Integer activityId) {
        
        log.info("收到查询活动信息请求，活动ID: {}", activityId);
        ActivityInfoVO activityInfo = activityService.getActivityInfo(activityId);
        
        if (activityInfo == null) {
            log.warn("未找到活动: {}", activityId);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(activityInfo);
    }
}