package com.test.prizesystem.controller;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.vo.ActivityInfoVO;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.util.PersistenceManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动管理控制器
 * <p>
 * 提供活动和奖品的管理接口，包括创建、更新和查询。
 * </p>
 *
 * @author MCP
 */
@RestController
@RequestMapping("/api/management")
@Api(tags = "活动管理", description = "活动和奖品管理接口")
public class ActivityManagementController {

    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private PersistenceManager persistenceManager;
    
    /**
     * 创建活动
     */
    @PostMapping("/activities")
    @ApiOperation(value = "创建活动", notes = "创建新活动")
    public Map<String, Object> createActivity(@RequestBody Activity activity) {
        Integer activityId = activityService.createActivity(activity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", activityId);
        response.put("message", "活动创建成功");
        
        return response;
    }
    
    /**
     * 更新活动
     */
    @PutMapping("/activities/{id}")
    @ApiOperation(value = "更新活动", notes = "更新已有活动")
    public Map<String, Object> updateActivity(@PathVariable Integer id, @RequestBody Activity activity) {
        activity.setId(id);
        boolean success = activityService.updateActivity(activity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "活动更新成功" : "活动更新失败");
        
        return response;
    }
    
    /**
     * 获取活动详情
     */
    @GetMapping("/activities/{id}")
    @ApiOperation(value = "获取活动详情", notes = "获取活动详细信息")
    public ActivityInfoVO getActivity(@PathVariable Integer id) {
        return activityService.getActivityInfo(id);
    }
    
    /**
     * 获取所有活动
     */
    @GetMapping("/activities")
    @ApiOperation(value = "获取所有活动", notes = "获取系统中所有活动")
    public List<Activity> getAllActivities() {
        return activityService.getAllActivities();
    }
    
    /**
     * 创建奖品
     */
    @PostMapping("/prizes")
    @ApiOperation(value = "创建奖品", notes = "创建新奖品")
    public Map<String, Object> createPrize(@RequestBody Prize prize) {
        Integer prizeId = activityService.createPrize(prize);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", prizeId);
        response.put("message", "奖品创建成功");
        
        return response;
    }
    
    /**
     * 获取所有奖品
     */
    @GetMapping("/prizes")
    @ApiOperation(value = "获取所有奖品", notes = "获取系统中所有奖品")
    public List<Prize> getAllPrizes() {
        return activityService.getAllPrizes();
    }
    
    /**
     * 关联活动和奖品
     */
    @PostMapping("/activities/{activityId}/prizes")
    @ApiOperation(value = "关联活动和奖品", notes = "将奖品关联到活动")
    public Map<String, Object> associateActivityPrize(
            @PathVariable Integer activityId,
            @RequestBody ActivityPrize activityPrize) {
        
        activityPrize.setActivityId(activityId);
        boolean success = activityService.associateActivityPrize(activityPrize);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "活动奖品关联成功" : "活动奖品关联失败");
        
        return response;
    }
    
    /**
     * 强制执行持久化
     */
    @PostMapping("/force-sync")
    @ApiOperation(value = "强制同步数据", notes = "强制将内存数据同步到磁盘")
    public Map<String, Object> forceSyncData() {
        persistenceManager.forceSyncNow();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "数据同步成功");
        
        return response;
    }
}