package com.test.prizesystem.util;

import com.test.prizesystem.model.persistence.PersistentData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 持久化管理器
 */
@Slf4j
@Component
public class PersistenceManager {
    private static final String DATA_DIR = "data";
    private static final String DATA_FILE = "prize_system.wu"; // 修正为与错误日志匹配的实际文件名
    private static final long SYNC_INTERVAL = 10 * 60 * 1000; // 10分钟同步一次

    @Autowired
    private RedBlackTreeStorage treeStorage;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean hasChanges = new AtomicBoolean(false);
    private Timer syncTimer;

    @PostConstruct
    public void init() {
        createDataDirectoryIfNeeded();
        loadFromDisk();
        startSyncTask();
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveToDisk));
    }

    @PreDestroy
    public void destroy() {
        if (syncTimer != null) {
            syncTimer.cancel();
        }
        saveToDisk();
    }

    private void createDataDirectoryIfNeeded() {
        Path dirPath = Paths.get(DATA_DIR);
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
                log.info("创建数据目录: {}", dirPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("创建数据目录失败", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadFromDisk() {
        File dataFile = new File(DATA_DIR, DATA_FILE);
        if (!dataFile.exists()) {
            log.info("数据文件不存在，将创建新的数据存储: {}", dataFile.getAbsolutePath());
            initialized.set(true);
            return;
        }

        try (FileInputStream fis = new FileInputStream(dataFile);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            PersistentData data = (PersistentData) ois.readObject();

            try {
                // 先清除所有现有树
                Method clearAllMethod = RedBlackTreeStorage.class.getDeclaredMethod("clearAll");
                clearAllMethod.setAccessible(true);
                clearAllMethod.invoke(treeStorage);

                // 获取访问TreesMap的字段
                Field treesMapField = RedBlackTreeStorage.class.getDeclaredField("treesMap");
                treesMapField.setAccessible(true);
                Map<String, Object> storageTreesMap = (Map<String, Object>) treesMapField.get(treeStorage);

                // 获取TreeData类
                Class<?> treeDataClass = Class.forName("com.test.prizesystem.util.RedBlackTreeStorage$TreeData");

                // 遍历要恢复的树
                for (Map.Entry<String, RedBlackTree> entry : data.getTrees().entrySet()) {
                    try {
                        // 因为TreeData是静态内部类，使用无参构造函数
                        Constructor<?> constructor = treeDataClass.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        Object treeData = constructor.newInstance();

                        // 设置树字段
                        Field treeField = treeDataClass.getDeclaredField("tree");
                        treeField.setAccessible(true);
                        treeField.set(treeData, entry.getValue());

                        // 设置锁字段
                        Field lockField = treeDataClass.getDeclaredField("lock");
                        lockField.setAccessible(true);
                        lockField.set(treeData, new ReentrantReadWriteLock());

                        // 添加到映射
                        storageTreesMap.put(entry.getKey(), treeData);
                        log.debug("成功恢复树: {}", entry.getKey());
                    } catch (Exception e) {
                        log.error("恢复树 {} 失败", entry.getKey(), e);
                    }
                }

                log.info("成功从{}加载数据，恢复时间: {}", dataFile.getAbsolutePath(), new Date(data.getTimestamp()));
            } catch (Exception e) {
                log.error("恢复数据失败", e);
            }

            initialized.set(true);
        } catch (Exception e) {
            log.error("从磁盘加载数据失败", e);
            initialized.set(true); // 即使加载失败也标记为已初始化
        }
    }

    public synchronized void saveToDisk() {
        if (!hasChanges.get() && new File(DATA_DIR, DATA_FILE).exists()) {
            log.debug("没有数据变更，跳过保存");
            return;
        }

        // 确保数据目录存在
        File dataDir = new File(DATA_DIR);
        ensureDirectoryExists(dataDir);

        // 构建持久化数据模型
        PersistentData data = new PersistentData();
        data.setTimestamp(System.currentTimeMillis());

        // 从RedBlackTreeStorage提取树数据
        Map<String, RedBlackTree> trees = extractTrees();
        
        if (trees.isEmpty()) {
            log.warn("没有可持久化的树数据，跳过保存");
            return;
        }
        
        data.setTrees(trees);

        // 尝试在进行文件操作前释放任何可能的文件锁
        System.gc();
        try {
            Thread.sleep(200); // 短暂等待GC完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 保存到临时文件，然后替换
        File dataFile = new File(DATA_DIR, DATA_FILE);
        File tempFile = new File(DATA_DIR, DATA_FILE + ".tmp");

        // 确保临时文件不存在或可写入
        if (tempFile.exists()) {
            tempFile.delete();
        }

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(data);
            oos.flush();
            
            // 确保文件已经完全写入磁盘
            fos.getFD().sync();

            // 关闭流以释放文件锁
            oos.close();
            fos.close();

            // 使用安全文件替换方法
            boolean success = safeReplaceFile(tempFile, dataFile);
            
            if (success) {
                log.info("成功将数据同步到磁盘, 时间: {}", new Date(data.getTimestamp()));
                hasChanges.set(false);
            } else {
                log.error("无法替换临时文件为正式文件");
            }
        } catch (Exception e) {
            log.error("保存数据到磁盘失败", e);
        }
    }
    
    /**
     * 安全地将临时文件替换为目标文件
     * 采用多种策略确保操作成功
     */
    private boolean safeReplaceFile(File tempFile, File targetFile) {
        if (!tempFile.exists() || tempFile.length() <= 0) {
            log.error("临时文件不存在或为空: {}", tempFile.getAbsolutePath());
            return false;
        }

        log.debug("尝试将临时文件 {} 替换为目标文件 {}", tempFile.getAbsolutePath(), targetFile.getAbsolutePath());
        
        // 尝试策略1: 使用Files.move进行原子替换
        try {
            Files.move(tempFile.toPath(), targetFile.toPath(), 
                      StandardCopyOption.REPLACE_EXISTING, 
                      StandardCopyOption.ATOMIC_MOVE);
            log.debug("使用原子移动成功替换文件");
            return true;
        } catch (IOException e) {
            log.warn("原子移动失败: {}, 尝试备用方法", e.getMessage());
        }
        
        // 尝试策略2: 使用Files.copy然后删除临时文件
        try {
            // 首先确保目标文件未被锁定
            if (targetFile.exists()) {
                System.gc(); // 尝试释放可能的文件句柄
                try {
                    Thread.sleep(100); // 短暂延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            Files.copy(tempFile.toPath(), targetFile.toPath(), 
                      StandardCopyOption.REPLACE_EXISTING);
            
            // 成功复制后删除临时文件
            if (tempFile.delete()) {
                log.debug("使用复制+删除成功替换文件");
                return true;
            } else {
                log.warn("复制成功但无法删除临时文件: {}", tempFile.getAbsolutePath());
                // 复制成功就算成功，即使临时文件没删除
                return true;
            }
        } catch (IOException e) {
            log.error("文件复制失败: {}", e.getMessage());
        }
        
        // 尝试策略3: 重试机制
        int maxRetries = 3;
        int retryDelay = 500; // 毫秒
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                // 短暂等待以释放可能的文件锁
                Thread.sleep(retryDelay);
                
                // 再次尝试复制文件
                Files.copy(tempFile.toPath(), targetFile.toPath(), 
                          StandardCopyOption.REPLACE_EXISTING);
                
                // 尝试删除源文件
                if (tempFile.delete()) {
                    log.info("在第{}次重试后成功替换文件", i + 1);
                } else {
                    log.warn("重试成功复制文件但无法删除源文件");
                }
                
                return true;
            } catch (IOException | InterruptedException e) {
                log.warn("第{}次重试失败: {}", i + 1, e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // 尝试策略4: 直接使用Java的renameTo方法
        if (tempFile.renameTo(targetFile)) {
            log.debug("使用renameTo成功替换文件");
            return true;
        } else {
            log.error("所有文件替换方法都失败");
            return false;
        }
    }
    
    /**
     * 确保目录存在，不存在则创建
     */
    private boolean ensureDirectoryExists(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            return true;
        }
        
        try {
            Files.createDirectories(directory.toPath());
            log.info("创建目录: {}", directory.getAbsolutePath());
            return true;
        } catch (IOException e) {
            log.error("创建目录失败: {}", directory.getAbsolutePath(), e);
            return false;
        }
    }

    private void startSyncTask() {
        syncTimer = new Timer("DataSyncTimer", true);
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    saveToDisk();
                } catch (Exception e) {
                    log.error("定时同步任务异常", e);
                }
            }
        }, SYNC_INTERVAL, SYNC_INTERVAL);
        log.info("已启动数据同步定时任务，间隔时间：{}分钟", SYNC_INTERVAL / 60000);
    }

    public void markDataChanged() {
        hasChanges.set(true);
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public void forceSyncNow() {
        saveToDisk();
    }

    /**
     * 从存储中提取所有树的快照
     * @return 树名称到树实例的映射
     */
    private Map<String, RedBlackTree> extractTrees() {
        Map<String, RedBlackTree> result = new HashMap<>();

        // 只持久化标准枚举中定义的树
        try {
            // 获取访问内部变量的字段
            Field treesMapField = RedBlackTreeStorage.class.getDeclaredField("treesMap");
            treesMapField.setAccessible(true);
            Map<String, Object> treesMap = (Map<String, Object>) treesMapField.get(treeStorage);

            // 获取TreeData类
            Class<?> treeDataClass = Class.forName("com.test.prizesystem.util.RedBlackTreeStorage$TreeData");
            Field treeField = treeDataClass.getDeclaredField("tree");

            // 仅处理我们关心的标准树
            for (TreeNames treeName : TreeNames.values()) {
                String key = treeName.getTreeName();
                Object treeDataObj = treesMap.get(key);
                
                if (treeDataObj != null) {
                    try {
                        treeField.setAccessible(true);
                        RedBlackTree tree = (RedBlackTree) treeField.get(treeDataObj);
                        result.put(key, tree);
                        log.debug("已提取树: {}, 大小: {}", key, tree.size());
                    } catch (Exception e) {
                        log.warn("无法提取树：{}", key, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("无法访问RedBlackTreeStorage内部字段", e);
        }

        return result;
    }
}