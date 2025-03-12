package com.test.prizesystem.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.lang.reflect.Method;

/**
 * 基于红黑树的存储引擎，替代MySQL数据库
 * key作为索引，值以JSON格式存储
 * 增强版：添加线程安全机制和树内容访问方法
 * <p>
 * 这个组件提供了一个基于内存的、高性能的、线程安全的数据存储解决方案。
 * 主要特点：
 * <ul>
 *   <li>支持多种数据类型的存储（通过命名树）</li>
 *   <li>线程安全的读写操作</li>
 *   <li>基于JSON的序列化和反序列化</li>
 *   <li>高效的查询操作，包括精确查询和范围查询</li>
 *   <li>适合高并发场景使用</li>
 * </ul>
 * <p>
 * 该存储引擎主要用于系统中需要高性能、内存级别访问速度的数据，特别是抽奖活动中的令牌和奖品数据。
 *
 * @version 1.0
 */
@Slf4j
@Component
public class RedBlackTreeStorage {

    // 存储不同类型数据的多个红黑树实例
    private final ConcurrentHashMap<String, TreeData> treesMap = new ConcurrentHashMap<>();

    // 用于JSON序列化和反序列化
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 内部类，包含红黑树及其读写锁
     */
    private static class TreeData {
        final RedBlackTree tree = new RedBlackTree();
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    }

    /**
     * 使用枚举获取指定名称的TreeData，如果不存在则创建
     */
    private TreeData getTreeData(TreeNames treeName) {
        return treesMap.computeIfAbsent(treeName.getTreeName(), k -> new TreeData());
    }

    /**
     * 使用枚举获取指定名称的红黑树
     */
    public RedBlackTree getTree(TreeNames treeName) {
        return getTreeData(treeName).tree;
    }

    /**
     * 使用字符串名称获取TreeData，如果不存在则创建
     */
    private TreeData getTreeData(String treeName) {
        return treesMap.computeIfAbsent(treeName, k -> new TreeData());
    }

    /**
     * 获取指定名称的红黑树
     */
    public RedBlackTree getTree(String treeName) {
        return getTreeData(treeName).tree;
    }

    /**
     * 保存对象到红黑树中（线程安全）
     * @param treeName 红黑树枚举
     * @param key 键
     * @param object 值对象
     */
    public <T> void save(TreeNames treeName, long key, T object) {
        TreeData treeData = getTreeData(treeName);
        treeData.lock.writeLock().lock();
        try {
            String json = objectMapper.writeValueAsString(object);
            treeData.tree.put(key, json);
            log.debug("已保存对象到树 {}, key={}", treeName, key);
        } catch (JsonProcessingException e) {
            log.error("对象序列化失败", e);
            throw new RuntimeException("保存对象失败", e);
        } finally {
            treeData.lock.writeLock().unlock();
        }
    }

    /**
     * 保存对象到红黑树中（线程安全）- 字符串兼容方法
     * @param treeName 红黑树名称
     * @param key 键
     * @param object 值对象
     */
    public <T> void save(String treeName, long key, T object) {
        TreeData treeData = getTreeData(treeName);
        treeData.lock.writeLock().lock();
        try {
            String json = objectMapper.writeValueAsString(object);
            treeData.tree.put(key, json);
            log.debug("已保存对象到树 {}, key={}", treeName, key);
        } catch (JsonProcessingException e) {
            log.error("对象序列化失败", e);
            throw new RuntimeException("保存对象失败", e);
        } finally {
            treeData.lock.writeLock().unlock();
        }
    }

    /**
     * 根据key查找对象（线程安全）
     * @param treeName 红黑树枚举
     * @param key 键
     * @param clazz 对象类型
     * @return 找到的对象，如果不存在则返回null
     */
    public <T> T find(TreeNames treeName, long key, Class<T> clazz) {
        TreeData treeData = getTreeData(treeName);
        treeData.lock.readLock().lock();
        try {
            Object value = treeData.tree.get(key);
            if (value == null) {
                return null;
            }

            return objectMapper.readValue((String) value, clazz);
        } catch (JsonProcessingException e) {
            log.error("对象反序列化失败", e);
            throw new RuntimeException("读取对象失败", e);
        } finally {
            treeData.lock.readLock().unlock();
        }
    }

    /**
     * 根据key查找对象（线程安全）- 字符串兼容方法
     * @param treeName 红黑树名称
     * @param key 键
     * @param clazz 对象类型
     * @return 找到的对象，如果不存在则返回null
     */
    public <T> T find(String treeName, long key, Class<T> clazz) {
        TreeData treeData = getTreeData(treeName);
        treeData.lock.readLock().lock();
        try {
            Object value = treeData.tree.get(key);
            if (value == null) {
                return null;
            }

            return objectMapper.readValue((String) value, clazz);
        } catch (JsonProcessingException e) {
            log.error("对象反序列化失败", e);
            throw new RuntimeException("读取对象失败", e);
        } finally {
            treeData.lock.readLock().unlock();
        }
    }

    /**
     * 不大于指定key的最大节点（线程安全）
     */
    public <T> T findBefore(TreeNames treeName, long timestamp, Class<T> clazz) {
        TreeData treeData = getTreeData(treeName);
        treeData.lock.readLock().lock();
        try {
            Object value = treeData.tree.findTokenBefore(timestamp);
            if (value == null) {
                return null;
            }

            return objectMapper.readValue((String) value, clazz);
        } catch (JsonProcessingException e) {
            log.error("对象反序列化失败", e);
            throw new RuntimeException("读取对象失败", e);
        } finally {
            treeData.lock.readLock().unlock();
        }
    }

    /**
     * 不大于指定key的最大节点（线程安全）- 字符串兼容方法
     */
    public <T> T findBefore(String treeName, long timestamp, Class<T> clazz) {
        TreeData treeData = getTreeData(treeName);
        treeData.lock.readLock().lock();
        try {
            Object value = treeData.tree.findTokenBefore(timestamp);
            if (value == null) {
                return null;
            }

            return objectMapper.readValue((String) value, clazz);
        } catch (JsonProcessingException e) {
            log.error("对象反序列化失败", e);
            throw new RuntimeException("读取对象失败", e);
        } finally {
            treeData.lock.readLock().unlock();
        }
    }

    /**
     * 删除对象（线程安全）
     * @param treeName 红黑树枚举
     * @param key 键
     */
    public void remove(TreeNames treeName, long key) {
        TreeData treeData = getTreeData(treeName);
        treeData.lock.writeLock().lock();
        try {
            treeData.tree.remove(key);
        } finally {
            treeData.lock.writeLock().unlock();
        }
    }

    /**
     * 删除对象（线程安全）- 字符串兼容方法
     * @param treeName 红黑树名称
     * @param key 键
     */
    public void remove(String treeName, long key) {
        TreeData treeData = getTreeData(treeName);
        treeData.lock.writeLock().lock();
        try {
            treeData.tree.remove(key);
        } finally {
            treeData.lock.writeLock().unlock();
        }
    }

    /**
     * 获取树中的节点数量（线程安全）
     */
    public int size(TreeNames treeName) {
        TreeData treeData = getTreeData(treeName);
        treeData.lock.readLock().lock();
        try {
            return treeData.tree.size();
        } finally {
            treeData.lock.readLock().unlock();
        }
    }

    /**
     * 获取树中的节点数量（线程安全）- 字符串兼容方法
     */
    public int size(String treeName) {
        TreeData treeData = getTreeData(treeName);
        treeData.lock.readLock().lock();
        try {
            return treeData.tree.size();
        } finally {
            treeData.lock.readLock().unlock();
        }
    }

    /**
     * 清空指定的树（线程安全）
     */
    public void clear(TreeNames treeName) {
        treesMap.remove(treeName.getTreeName());
    }

    /**
     * 清空指定的树（线程安全）- 字符串兼容方法
     */
    public void clear(String treeName) {
        treesMap.remove(treeName);
    }

    /**
     * 清空所有树
     */
    public void clearAll() {
        treesMap.clear();
    }

    /**
     * 获取树中数据的样本（用于调试）
     * @param treeName 树枚举
     * @param clazz 对象类型
     * @param limit 最大返回数量
     * @return 对象列表
     */
    public <T> List<T> getSampleData(TreeNames treeName, Class<T> clazz, int limit) {
        TreeData treeData = getTreeData(treeName);
        List<T> result = new ArrayList<>();

        treeData.lock.readLock().lock();
        try {
            // 通过递归获取树中的节点
            collectNodes(treeData.tree.root, result, clazz, limit);
        } finally {
            treeData.lock.readLock().unlock();
        }

        return result;
    }

    /**
     * 获取树中数据的样本（用于调试）- 字符串兼容方法
     * @param treeName 树名称
     * @param clazz 对象类型
     * @param limit 最大返回数量
     * @return 对象列表
     */
    public <T> List<T> getSampleData(String treeName, Class<T> clazz, int limit) {
        TreeData treeData = getTreeData(treeName);
        List<T> result = new ArrayList<>();

        treeData.lock.readLock().lock();
        try {
            // 通过递归获取树中的节点
            collectNodes(treeData.tree.root, result, clazz, limit);
        } finally {
            treeData.lock.readLock().unlock();
        }

        return result;
    }

    /**
     * 递归收集红黑树中的节点
     */
    private <T> void collectNodes(RedBlackTree.Node node, List<T> result, Class<T> clazz, int limit) {
        if (node == null || result.size() >= limit) {
            return;
        }

        // 中序遍历：左-根-右
        collectNodes(node.left, result, clazz, limit);

        if (result.size() < limit) {
            try {
                T obj = objectMapper.readValue((String) node.value, clazz);
                result.add(obj);
            } catch (JsonProcessingException e) {
                log.error("解析节点数据失败: {}", e.getMessage());
            }
        }

        collectNodes(node.right, result, clazz, limit);
    }

    /**
     * 获取用户中奖记录
     * @param userId 用户ID
     * @param limit 限制返回数量
     * @return 中奖记录列表
     */
    public <T> List<T> getUserPrizeRecords(Integer userId, Class<T> clazz, int limit) {
        return getSampleDataByUser(TreeNames.USER_DRAW_RECORDS, userId, clazz, limit);
    }

    /**
     * 获取活动中奖记录
     * @param activityId 活动ID
     * @param limit 限制返回数量
     * @return 中奖记录列表
     */
    public <T> List<T> getActivityPrizeRecords(Integer activityId, Class<T> clazz, int limit) {
        return getSampleDataByActivity(TreeNames.USER_DRAW_RECORDS, activityId, clazz, limit);
    }

    /**
     * 根据用户ID查询数据
     * @param treeName 树名称
     * @param userId 用户ID
     * @param clazz 返回对象类型
     * @param limit 限制数量
     * @return 符合条件的对象列表
     */
    private <T> List<T> getSampleDataByUser(TreeNames treeName, Integer userId, Class<T> clazz, int limit) {
        List<T> result = new ArrayList<>();
        TreeData treeData = getTreeData(treeName);
        
        treeData.lock.readLock().lock();
        try {
            RedBlackTree tree = treeData.tree;
            List<Long> keys = tree.keySet();
            
            for (Long key : keys) {
                try {
                    Object value = tree.get(key);
                    if (value == null) {
                        continue;
                    }
                    
                    T object = objectMapper.readValue((String) value, clazz);
                    
                    // 通过反射获取用户ID并比较
                    Method getUserIdMethod = clazz.getMethod("getUserId");
                    Integer objectUserId = (Integer) getUserIdMethod.invoke(object);
                    
                    if (userId.equals(objectUserId)) {
                        result.add(object);
                        if (result.size() >= limit) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("获取用户数据时出错：{}", e.getMessage());
                }
            }
        } finally {
            treeData.lock.readLock().unlock();
        }
        
        return result;
    }
    
    /**
     * 根据活动ID查询数据
     * @param treeName 树名称
     * @param activityId 活动ID
     * @param clazz 返回对象类型
     * @param limit 限制数量
     * @return 符合条件的对象列表
     */
    private <T> List<T> getSampleDataByActivity(TreeNames treeName, Integer activityId, Class<T> clazz, int limit) {
        List<T> result = new ArrayList<>();
        TreeData treeData = getTreeData(treeName);
        
        treeData.lock.readLock().lock();
        try {
            RedBlackTree tree = treeData.tree;
            List<Long> keys = tree.keySet();
            
            for (Long key : keys) {
                try {
                    Object value = tree.get(key);
                    if (value == null) {
                        continue;
                    }
                    
                    T object = objectMapper.readValue((String) value, clazz);
                    
                    // 通过反射获取活动ID并比较
                    Method getActivityIdMethod = clazz.getMethod("getActivityId");
                    Integer objectActivityId = (Integer) getActivityIdMethod.invoke(object);
                    
                    if (activityId.equals(objectActivityId)) {
                        result.add(object);
                        if (result.size() >= limit) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("获取活动数据时出错：{}", e.getMessage());
                }
            }
        } finally {
            treeData.lock.readLock().unlock();
        }
        
        return result;
    }
}