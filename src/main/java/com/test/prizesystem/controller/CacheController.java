package com.test.prizesystem.controller;

import com.test.prizesystem.model.entity.Token;
import com.test.prizesystem.model.entity.User;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.entity.UserPrizeRecord;
import com.test.prizesystem.model.entity.UserDrawRecord;
import com.test.prizesystem.service.TokenService;
import com.test.prizesystem.service.UserService;
import com.test.prizesystem.util.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@Api(tags = "缓存管理")
public class CacheController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RedBlackTreeStorage treeStorage;

    @Autowired
    private TokenQueue tokenQueue;

    @Autowired
    private UserService userService;

    @Autowired
    private PersistenceManager persistenceManager;

    /**
     * 查看活动令牌信息
     */
    @GetMapping("/tokens/{activityId}")
    @ApiOperation(value = "查看活动令牌信息", notes = "返回活动的令牌数量及详细信息")
    public Map<String, Object> getTokenDetails(
            @ApiParam(value = "活动ID", required = true, example = "1") @PathVariable Integer activityId) {
        return tokenService.getTokenDetails(activityId);
    }
    
    /**
     * 获取系统状态
     * 包括令牌队列大小、持久化初始化状态及各红黑树内容
     */
    @GetMapping("/status")
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        
        // 从TokenService获取令牌队列大小
        if (tokenService != null) {
            // 获取默认活动ID的令牌队列大小
            Integer defaultActivityId = 1;
            result.put("tokenQueueSize", tokenService.getTokenQueueSize(defaultActivityId));
        }
        
        // 持久化初始化状态
        if (persistenceManager != null) {
            try {
                Field initializedField = PersistenceManager.class.getDeclaredField("initialized");
                initializedField.setAccessible(true);
                AtomicBoolean initialized = (AtomicBoolean) initializedField.get(persistenceManager);
                result.put("persistenceInitialized", initialized.get());
            } catch (Exception e) {
                log.error("无法获取持久化初始化状态", e);
                result.put("persistenceInitialized", false);
            }
        }
        
        // 将每个红黑树的状态添加到结果中
        Map<String, Object> treesStatus = new HashMap<>();
        for (TreeNames treeName : TreeNames.values()) {
            try {
                Map<String, Object> treeStatus = new HashMap<>();
                int size = treeStorage.size(treeName);
                treeStatus.put("size", size);
                
                // 为USERS和PRIZES树添加详细内容
                if (treeName == TreeNames.USERS) {
                    // 用户信息不展示详细内容，只展示数量
                    treeStatus.put("content", new HashMap<>());
                } else if (treeName == TreeNames.PRIZES) {
                    // 展示奖品的基本信息
                    Map<String, Object> prizes = new HashMap<>();
                    List<Prize> prizeList = treeStorage.getSampleData(treeName, Prize.class, 100);
                    for (Prize prize : prizeList) {
                        Map<String, Object> prizeInfo = new HashMap<>();
                        prizeInfo.put("id", prize.getId());
                        prizeInfo.put("name", prize.getName());
                        prizeInfo.put("remainingAmount", prize.getRemainingAmount());
                        prizes.put(prize.getId().toString(), prizeInfo);
                    }
                    treeStatus.put("content", prizes);
                } else {
                    // 对于其他树，只展示键列表
                    com.test.prizesystem.util.RedBlackTree tree = treeStorage.getTree(treeName);
                    List<Long> keys = new ArrayList<>();
                    if (tree != null) {
                        // 将树中的键添加到列表中（最多100个）
                        for (Long key : tree.keySet()) {
                            keys.add(key);
                            if (keys.size() >= 100) break;
                        }
                    }
                    Map<String, Object> content = new HashMap<>();
                    content.put("keys", keys);
                    treeStatus.put("content", content);
                }
                
                treesStatus.put(treeName.toString().toUpperCase(), treeStatus);
            } catch (Exception e) {
                log.error("获取树状态失败: {}", treeName, e);
            }
        }
        result.put("trees", treesStatus);
        
        return result;
    }
    
    /**
     * 获取用户抽奖记录相关的树
     */
    private Map<String, Object> getUserDrawRecordTrees() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 使用反射获取内部存储的所有树
            Field treesMapField = RedBlackTreeStorage.class.getDeclaredField("treesMap");
            treesMapField.setAccessible(true);
            Map<String, Object> treesMap = (Map<String, Object>) treesMapField.get(treeStorage);
            
            // 查找所有用户抽奖记录相关的树
            for (String key : treesMap.keySet()) {
                if (key.startsWith("user_draw_records:")) {
                    String userId = key.substring("user_draw_records:".length());
                    int size = treeStorage.size(key);
                    
                    Map<String, Object> recordsInfo = new HashMap<>();
                    recordsInfo.put("size", size);
                    
                    // 获取具体记录
                    Map<String, Object> records = new HashMap<>();
                    for (int i = 1; i <= Math.min(size, 10); i++) {
                        UserDrawRecord record = treeStorage.find(key, i, UserDrawRecord.class);
                        if (record != null) {
                            records.put(String.valueOf(i), mapUserDrawRecordInfo(record));
                        }
                    }
                    recordsInfo.put("records", records);
                    result.put(userId, recordsInfo);
                }
            }
        } catch (Exception e) {
            log.error("获取用户抽奖记录树时出错", e);
        }
        return result;
    }
    
    /**
     * 将用户信息转换为简洁的Map
     */
    private Map<String, Object> mapUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("drawQuota", user.getDrawQuota());
        info.put("drawCount", user.getDrawCount());
        info.put("winQuota", user.getWinQuota());
        info.put("winCount", user.getWinCount());
        return info;
    }
    
    /**
     * 将奖品信息转换为简洁的Map
     */
    private Map<String, Object> mapPrizeInfo(Prize prize) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", prize.getId());
        info.put("name", prize.getName());
        info.put("remainingAmount", prize.getRemainingAmount());
        return info;
    }
    
    /**
     * 将用户奖品记录转换为简洁的Map
     */
    private Map<String, Object> mapUserPrizeRecordInfo(UserPrizeRecord record) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", record.getId());
        info.put("userId", record.getUserId());
        info.put("prizeId", record.getPrizeId());
        info.put("winTime", record.getWinTime());
        return info;
    }
    
    /**
     * 将用户抽奖记录转换为简洁的Map
     */
    private Map<String, Object> mapUserDrawRecordInfo(UserDrawRecord record) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", record.getId());
        info.put("userId", record.getUserId());
        info.put("activityId", record.getActivityId());
        info.put("drawTime", record.getDrawTime());
        return info;
    }

    /**
     * 清除系统缓存
     */
    @PostMapping("/clear")
    @ApiOperation(value = "清除系统缓存", notes = "清除系统缓存数据，小心使用此功能")
    public Map<String, Object> clearCache() {
        // 首先将所有数据持久化
        persistenceManager.forceSyncNow();

        // 清除红黑树缓存
        for (TreeNames treeName : TreeNames.values()) {
            treeStorage.clear(treeName);
        }

        // 清除令牌队列
        tokenQueue.clearAll();

        log.warn("已清除所有系统缓存");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "缓存清除成功，系统将从持久化数据还原");

        return result;
    }

    /**
     * 手动触发数据加载
     */
    @PostMapping("/reload")
    @ApiOperation(value = "重新加载数据", notes = "从持久化文件重新加载数据")
    public Map<String, Object> reloadData() {
        persistenceManager.loadFromDisk();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "数据重新加载成功");

        return result;
    }
}
