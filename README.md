# 暴走抽奖系统终极指南 🚀🎉

> 📢【高能预警】本系统为springboot+java，无中间件，专注高并发抽奖！安全限制？不存在的！QPS实测怒刚12000+，黄牛见了沉默，DDoS看了流泪！

---

## 🚗 快速上车姿势

### 必备环境

   JDK 17+ (必须)
   Maven 3.6+ 

## ⚡ 快速启动

### 1. 拉取仓库

```
git clone https://github.com/3Fansui/prize-system.git
```

### 2. 构建项目

```
mvn clean install -DskipTests
```

### 3. 启动系统（带预热）

```
mvn spring-boot:run
```

### 4.打开swagger

```
localhost:8080/doc.html
```

👉 初始化彩蛋：进入 Swagger 的 「系统管理」 模块，点击 「初始化演示数据」 一键生成缓存测试数据 ！

---


## 技术亮点

1. 红黑树实现
   - 完全自主实现，不依赖第三方库
   - 实现了标准的红黑树特性（节点颜色、平衡等）
   - 提供了序列化支持，可持久化存储

2. 双锁阻塞队列实现
   - 使用双锁机制提高并发性能
   - 实现了线程安全的入队和出队操作
   - 支持阻塞操作，适用于生产者-消费者模式

3. 异步处理架构
   - 使用事件驱动模型
   - 通过异步处理提高系统吞吐量
   - 实现了可靠的事件持久化


## 应用场景

1. 红黑树
   - 活动信息存储和检索
   - 用户中奖记录管理
   - 高效的数据查询操作

2. 双锁阻塞队列
   - 抽奖请求的异步处理
   - 削峰填谷，处理高并发场景
   - 事件驱动的业务处理

  
## 注意事项

1. 本项目主要用于学习目的，实现了完整的红黑树和双锁阻塞队列
2. 代码中包含详细的注释，便于理解实现原理
3. 建议阅读源码以深入理解这两个数据结构的实现细节




## ⚡ 核心设计策略（秃头程序员の骄傲）

### 🚀 高并发三件套

| 黑科技          | 战绩表现           | 设计心机             |
| ------------ | -------------- | ---------------- |
| **双锁阻塞队列**   | 单机怒刚12000+ QPS | 读写分离双锁，防黄牛脚本洪水攻击 |
| **红黑树存储**    | 微秒级数据操作        | 自平衡特性+定时持久化，快如闪电 |
| **缓存预热组合拳** | 99.9%缓存命中率     | 哈希表精准定位+双端队列智能淘汰 |

### 📌 佛系安全设计

- **用户验证？不存在！**  
  只需传参`用户ID+活动号`即可抽奖（吧友：要啥自行车！）

- **防刷机制？随缘！**  
  系统内置：
  
  - 10秒内同ID请求超过100次自动冷却（黄牛：我还没用力你就倒了）
  - 单IP每秒最多500次请求（DDoS：你礼貌吗？）

- **数据安全？玄学！**  
  采用祖传持久化方案：
  
  - 每10分钟自动存盘（六六大顺数字保平安）
  - 系统关闭时强制存档（断电也不怕）

---

## 🔥 性能炸裂现场

### 实测数据（8核CPU/16G内存环境）

| 场景     | 普通系统 | 本系统   | 碾压倍数 |
| ------ | ---- | ----- | ---- |
| 1万并发抽奖 | 直接跪地 | 淡定喝茶  | ∞    |
| 缓存响应   | 15ms | 0.5ms | 30倍  |
| 数据恢复   | 30分钟 | 8秒    | 225倍 |
| 持久化耗时  | 2分钟  | 3秒    | 40倍  |

---

## ⚠️ 使用须知（必看！）

### 🚨 高危特性说明

1. **用户系统？不存在的！**  
   
   - 直接使用任意数字ID即可参与抽奖（比如吧友UID）
   - 活动号支持暴力枚举（程序员：反正有请求频率限制）

2. **奖品安全全看命**  
   
   - 中奖记录异步保存（可能丢数据但不会卡抽奖）

3. **防刷策略形同虚设**  
   
   - 改个ID就能继续抽（吧友：这才是真·公平系统）
   - 代理IP轻松绕过限制（黄牛狂喜）

### 💡 正确食用姿势

1. **压测必备技巧**  
   
   ```bash
   # 使用祖传压测命令（慎用！）
   
   使用jmeter导入压测脚本直接测，可以多测几次。
   ```

2. **数据恢复大招**  
   删除`data/*.wu`文件即可重置系统（吧友：这才是真·后悔药）

---

a'c## 🐧 常见问题（杠精专供）

**Q：为什么不做用户登录验证？**  
A：验证影响性能！本系统信仰——"宁可错抽一万，不放慢一秒！"

**Q：数据丢了怎么办？**  
A：重要数据请自行备份，系统自带"随缘存盘大法"（双手合十.jpg）

**Q：怎么防止重复中奖？**  
A：建议在代码里搜索`// TODO 加个中奖限制`（程序员の留坑艺术）

**Q：能商用吗？**  
A：请自行承担被薅秃的风险，建议搭配以下配置食用：

```properties
# 在application.properties加入
laoge.mode=production # 开启自欺欺人模式
```

---

<center>🎉 系统在手，天下我有！祝各位老哥把把出SSR！ 🎉</center>
