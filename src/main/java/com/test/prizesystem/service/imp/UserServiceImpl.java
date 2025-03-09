package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.entity.User;
import com.test.prizesystem.service.UserService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.TreeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户服务实现类
 * <p>
 * 使用红黑树存储用户信息，提供线程安全的用户管理功能。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    // 用户名到用户ID的映射
    private final ConcurrentHashMap<String, Integer> usernameToId = new ConcurrentHashMap<>();
    
    // 用户ID生成器
    private final AtomicInteger idGenerator = new AtomicInteger(10000);
    
    @PostConstruct
    public void init() {
        // 创建一个默认用户用于测试
        if (treeStorage.size(TreeNames.USERS) == 0) {
            register("test", "test", 100, 10);
            register("admin", "admin", 1000, 100);
            log.info("创建默认用户完成");
        }
    }

    @Override
    public User register(String username, String password, Integer drawQuota, Integer winQuota) {
        // 检查用户名是否已存在
        if (usernameToId.containsKey(username)) {
            log.warn("用户名 {} 已存在", username);
            return null;
        }
        
        // 创建新用户
        User user = new User();
        user.setId(idGenerator.incrementAndGet());
        user.setUsername(username);
        user.setPassword(password);
        user.setDrawQuota(drawQuota != null ? drawQuota : 100);  // 默认抽奖配额
        user.setWinQuota(winQuota != null ? winQuota : 10);      // 默认中奖配额
        user.setDrawCount(0);
        user.setWinCount(0);
        user.setCreateTime(new Date());
        user.setLastLoginTime(new Date());
        
        // 保存用户信息
        treeStorage.save(TreeNames.USERS, user.getId(), user);
        usernameToId.put(username, user.getId());
        
        log.info("用户注册成功: {}, ID: {}", username, user.getId());
        return user;
    }

    @Override
    public User login(String username, String password) {
        Integer userId = usernameToId.get(username);
        if (userId == null) {
            log.warn("用户名 {} 不存在", username);
            return null;
        }
        
        User user = treeStorage.find(TreeNames.USERS, userId, User.class);
        if (user != null && password.equals(user.getPassword())) {
            // 更新最后登录时间
            user.setLastLoginTime(new Date());
            treeStorage.save(TreeNames.USERS, user.getId(), user);
            
            log.info("用户 {} 登录成功", username);
            return user;
        }
        
        log.warn("用户 {} 密码错误", username);
        return null;
    }

    @Override
    public User getUser(Integer userId) {
        if (userId == null) {
            return null;
        }
        return treeStorage.find(TreeNames.USERS, userId, User.class);
    }

    @Override
    public boolean tryDraw(Integer userId) {
        if (userId == null) {
            return false;
        }
        
        User user = getUser(userId);
        if (user == null) {
            log.warn("用户 {} 不存在", userId);
            return false;
        }
        
        // 检查是否达到抽奖次数上限
        synchronized (user) {
            if (user.getRemainingDraws() <= 0) {
                log.debug("用户 {} 抽奖次数已用完", userId);
                return false;
            }
            
            // 增加抽奖次数
            user.setDrawCount(user.getDrawCount() + 1);
            treeStorage.save(TreeNames.USERS, userId, user);
            
            log.debug("用户 {} 抽奖次数增加到 {}", userId, user.getDrawCount());
            return true;
        }
    }

    @Override
    public boolean tryWin(Integer userId) {
        if (userId == null) {
            return false;
        }
        
        User user = getUser(userId);
        if (user == null) {
            log.warn("用户 {} 不存在", userId);
            return false;
        }
        
        // 检查是否达到中奖次数上限
        synchronized (user) {
            if (user.getRemainingWins() <= 0) {
                log.debug("用户 {} 中奖次数已达上限", userId);
                return false;
            }
            
            // 增加中奖次数
            user.setWinCount(user.getWinCount() + 1);
            treeStorage.save(TreeNames.USERS, userId, user);
            
            log.debug("用户 {} 中奖次数增加到 {}", userId, user.getWinCount());
            return true;
        }
    }

    @Override
    public int getRemainingDraws(Integer userId) {
        User user = getUser(userId);
        return user != null ? user.getRemainingDraws() : 0;
    }

    @Override
    public int getRemainingWins(Integer userId) {
        User user = getUser(userId);
        return user != null ? user.getRemainingWins() : 0;
    }
}