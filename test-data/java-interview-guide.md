# Java 后端面试知识点总结

## 1. Java 基础

### 1.1 面向对象编程
- **封装、继承、多态**：面向对象的三大特性
- **抽象类 vs 接口**：
  - 抽象类可以有构造方法，接口不能
  - 一个类只能继承一个抽象类，但可以实现多个接口
  - Java 8 后接口可以有默认方法和静态方法

### 1.2 集合框架
- **ArrayList vs LinkedList**：
  - ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n)
  - LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n)
- **HashMap 原理**：
  - JDK 7：数组 + 链表
  - JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换）
  - 扩容机制：负载因子 0.75，容量翻倍
  - 线程不安全，多线程使用 ConcurrentHashMap

### 1.3 多线程与并发
- **线程池 ThreadPoolExecutor**：
  - 核心线程数 corePoolSize
  - 最大线程数 maximumPoolSize
  - 任务队列 workQueue
  - 拒绝策略 RejectedExecutionHandler
- **synchronized vs Lock**：
  - synchronized 自动释放锁，Lock 需要手动释放
  - Lock 支持更灵活的锁机制（可中断、可超时）
- **volatile 关键字**：
  - 保证可见性：一个线程修改，其他线程立即可见
  - 禁止指令重排序
  - 不保证原子性

## 2. Spring 框架

### 2.1 Spring IOC（控制反转）
- **依赖注入的三种方式**：
  1. 构造器注入（推荐，保证不可变性）
  2. Setter 注入（可选依赖）
  3. 字段注入（不推荐，难以测试）
- **Bean 的生命周期**：
  1. 实例化
  2. 属性赋值
  3. 初始化（@PostConstruct）
  4. 使用
  5. 销毁（@PreDestroy）

### 2.2 Spring AOP（面向切面编程）
- **应用场景**：事务管理、日志记录、权限校验、性能监控
- **核心概念**：
  - Aspect（切面）：横切关注点的模块化
  - Join Point（连接点）：方法执行点
  - Pointcut（切入点）：匹配规则
  - Advice（通知）：@Before、@After、@Around
- **动态代理**：
  - JDK 动态代理：基于接口
  - CGLIB 代理：基于继承

## 3. Spring Boot

### 3.1 自动配置原理
- **@SpringBootApplication** = @Configuration + @EnableAutoConfiguration + @ComponentScan
- **条件注解**：
  - @ConditionalOnClass：类路径存在某个类
  - @ConditionalOnMissingBean：容器中不存在某个 Bean
  - @ConditionalOnProperty：配置文件中存在某个属性

### 3.2 配置文件
- **application.yml vs application.properties**：
  - yml 支持层级结构，可读性更好
  - properties 更简单，兼容性更好
- **多环境配置**：
  - application-dev.yml
  - application-test.yml
  - application-prod.yml
  - 通过 spring.profiles.active 切换

## 4. MyBatis / MyBatis-Plus

### 4.1 MyBatis 核心
- **#{}  vs ${}**：
  - #{} 预编译，防止 SQL 注入
  - ${} 字符串替换，存在注入风险
- **一级缓存 vs 二级缓存**：
  - 一级缓存：SqlSession 级别，默认开启
  - 二级缓存：Mapper 级别，需要手动开启

### 4.2 MyBatis-Plus
- **优势**：
  - 内置通用 CRUD 方法
  - 代码生成器
  - 分页插件
  - 乐观锁、逻辑删除支持

## 5. MySQL 数据库

### 5.1 索引
- **B+ 树索引**：MySQL InnoDB 默认索引结构
- **聚簇索引 vs 非聚簇索引**：
  - 聚簇索引：主键索引，叶子节点存储完整数据
  - 非聚簇索引：辅助索引，叶子节点存储主键值
- **索引失效场景**：
  - 使用函数或表达式
  - 类型转换
  - 最左前缀原则失效
  - 使用 OR 连接

### 5.2 事务
- **ACID 特性**：
  - 原子性 Atomicity
  - 一致性 Consistency
  - 隔离性 Isolation
  - 持久性 Durability
- **隔离级别**：
  - READ UNCOMMITTED（读未提交）
  - READ COMMITTED（读已提交）
  - REPEATABLE READ（可重复读，MySQL 默认）
  - SERIALIZABLE（串行化）
- **锁机制**：
  - 行锁：锁定某一行
  - 表锁：锁定整个表
  - 间隙锁：防止幻读

## 6. Redis

### 6.1 数据类型
- **String**：缓存、计数器、分布式锁
- **Hash**：对象存储（用户信息）
- **List**：消息队列、列表分页
- **Set**：去重、交集并集差集
- **ZSet**：排行榜、延迟队列

### 6.2 缓存问题
- **缓存穿透**：查询不存在的数据，缓存和数据库都没有
  - 解决：布隆过滤器、缓存空值
- **缓存击穿**：热点 Key 过期，大量请求直达数据库
  - 解决：热点 Key 永不过期、互斥锁
- **缓存雪崩**：大量 Key 同时过期
  - 解决：过期时间加随机值、多级缓存

### 6.3 持久化
- **RDB**：快照，定时备份
- **AOF**：命令日志，实时性更好

## 7. 分布式

### 7.1 分布式锁
- **实现方式**：
  - Redis SETNX + 过期时间
  - Redisson（推荐）
  - Zookeeper
- **注意事项**：
  - 防止死锁（设置过期时间）
  - 防止误删（UUID 标识）
  - 原子性（Lua 脚本）

### 7.2 分布式事务
- **2PC（两阶段提交）**：
  - 准备阶段、提交阶段
  - 缺点：阻塞、单点故障
- **TCC（Try-Confirm-Cancel）**：
  - 业务侵入性强
  - 性能较好
- **Seata**：阿里开源分布式事务框架

## 8. 消息队列

### 8.1 RabbitMQ / Kafka
- **应用场景**：
  - 异步处理：发送邮件、短信
  - 流量削峰：秒杀系统
  - 系统解耦：订单服务和库存服务
- **消息可靠性**：
  - 生产者确认机制
  - 消息持久化
  - 消费者手动 ACK

## 9. 设计模式

### 9.1 常用设计模式
- **单例模式**：Spring Bean 默认单例
- **工厂模式**：BeanFactory
- **代理模式**：AOP
- **模板方法模式**：JdbcTemplate、RestTemplate
- **观察者模式**：Spring 事件监听
- **策略模式**：支付方式选择

## 10. JVM

### 10.1 内存模型
- **堆**：对象实例，GC 主要区域
- **栈**：方法调用，局部变量
- **方法区**：类信息、常量池
- **程序计数器**：当前线程执行的字节码行号

### 10.2 垃圾回收
- **GC 算法**：
  - 标记-清除
  - 复制算法（新生代）
  - 标记-整理（老年代）
- **GC 收集器**：
  - Serial、Parallel、CMS、G1、ZGC

---

## 面试常见问题

### 1. Spring Boot 启动流程？
1. 创建 SpringApplication 对象
2. 运行 run 方法
3. 准备环境（Environment）
4. 创建 ApplicationContext
5. 刷新容器（refresh）
6. 启动完成

### 2. 如何解决跨域问题？
- @CrossOrigin 注解
- CORS 全局配置
- Nginx 反向代理

### 3. 接口性能优化手段？
- 使用缓存（Redis）
- 数据库索引优化
- 异步处理（消息队列）
- 分页查询
- SQL 优化（避免全表扫描）
- 连接池复用

### 4. 如何保证接口幂等性？
- 唯一 ID + Redis
- Token 机制
- 乐观锁（版本号）
- 状态机（订单状态）

### 5. 如何实现限流？
- 计数器
- 滑动窗口
- 令牌桶（Guava RateLimiter）
- 漏桶算法

---

**祝你面试顺利！🎉**
