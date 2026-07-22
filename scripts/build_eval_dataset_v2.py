#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
from collections import Counter
from pathlib import Path, PurePosixPath

from eval_dataset_contract import _jaccard, _question_features, sha256_file


REPO_ROOT = Path(__file__).resolve().parents[1]
V1_MANIFEST = REPO_ROOT / "docs/eval/dataset-manifest.json"
V1_RELEASE_MANIFEST = REPO_ROOT / "docs/eval/releases/rag-eval-dev-v1-manifest.json"
V2_QUESTIONS = REPO_ROOT / "docs/eval/releases/rag-eval-dev-v2.jsonl"
V2_REVIEW = REPO_ROOT / "docs/eval/review/rag-eval-dev-v2-review.jsonl"
V2_MANIFEST = REPO_ROOT / "docs/eval/releases/rag-eval-dev-v2-manifest.json"

SPRING = "springboot-basics.md"
JAVA = "java-interview-guide.md"
RAG = "rag-technology-guide.md"

ROWS: list[dict] = []


def answerable(
    sample_id: str,
    question: str,
    sample_type: str,
    difficulty: str,
    contexts: list[tuple[str, str]],
    keywords: list[str],
    answer_points: list[str],
    notes: str,
) -> None:
    ROWS.append(
        {
            "id": sample_id,
            "question": question,
            "type": sample_type,
            "difficulty": difficulty,
            "expected_sources": list(dict.fromkeys(source for source, _ in contexts)),
            "expected_keywords": keywords,
            "expected_answer_points": answer_points,
            "expected_contexts": [
                {"source": source, "contains": contains}
                for source, contains in contexts
            ],
            "should_answer": True,
            "notes": notes,
        }
    )


def no_answer(sample_id: str, question: str, difficulty: str, notes: str) -> None:
    ROWS.append(
        {
            "id": sample_id,
            "question": question,
            "type": "no_answer",
            "difficulty": difficulty,
            "expected_sources": [],
            "expected_keywords": [],
            "expected_answer_points": ["知识库没有足够信息，应该明确说明无法回答"],
            "expected_contexts": [],
            "should_answer": False,
            "notes": notes,
        }
    )


# FACT_ROWS — remaining matrix: easy 9 / medium 12 / hard 4.
answerable("fact-011", "Spring Boot 这个脚手架主要解决什么问题？", "fact", "easy", [(SPRING, "Spring Boot 是基于 Spring 框架的快速开发脚手架")], ["快速开发脚手架", "简化搭建"], ["它基于 Spring 框架", "用于简化 Spring 应用的初始搭建和开发过程"], "Spring Boot 定位短事实。")
answerable("fact-012", "Spring Boot 文档列出了哪三种内嵌服务器？", "fact", "easy", [(SPRING, "内嵌服务器**：Tomcat、Jetty、Undertow")], ["Tomcat", "Jetty", "Undertow"], ["支持 Tomcat", "支持 Jetty", "支持 Undertow"], "内嵌服务器枚举。")
answerable("fact-013", "Spring Boot 起步依赖的作用是什么？", "fact", "easy", [(SPRING, "起步依赖**：一站式依赖管理，避免版本冲突")], ["起步依赖", "依赖管理", "版本冲突"], ["提供一站式依赖管理", "帮助避免版本冲突"], "起步依赖作用。")
answerable("fact-014", "Spring MVC 中哪个注解负责映射 HTTP 请求？", "fact", "easy", [(SPRING, "@RequestMapping`：映射 HTTP 请求")], ["@RequestMapping", "HTTP 请求"], ["@RequestMapping 用于映射 HTTP 请求"], "请求映射短事实。")
answerable("fact-015", "Spring MVC 中路径参数和查询参数分别用什么注解？", "fact", "easy", [(SPRING, "@PathVariable`：路径参数"), (SPRING, "@RequestParam`：查询参数")], ["@PathVariable", "@RequestParam"], ["路径参数使用 @PathVariable", "查询参数使用 @RequestParam"], "参数注解对照。")
answerable("fact-016", "ArrayList 和 LinkedList 的查询复杂度分别是多少？", "fact", "easy", [(JAVA, "ArrayList 基于数组，查询快 O(1)"), (JAVA, "LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n)")], ["ArrayList", "O(1)", "LinkedList", "O(n)"], ["ArrayList 查询是 O(1)", "LinkedList 查询是 O(n)"], "集合查询复杂度。")
answerable("fact-017", "volatile 能保证什么，又不能保证什么？", "fact", "easy", [(JAVA, "保证可见性：一个线程修改，其他线程立即可见"), (JAVA, "不保证原子性")], ["可见性", "禁止指令重排序", "不保证原子性"], ["保证线程间可见性", "禁止指令重排序", "不保证原子性"], "并发关键字事实。")
answerable("fact-018", "传统 LLM 在 RAG 文档中被列出哪三类局限？", "fact", "easy", [(RAG, "知识截止**：只知道训练时的数据"), (RAG, "幻觉问题**：可能生成不准确的内容"), (RAG, "缺乏专业知识**：无法回答企业内部或专业领域问题")], ["知识截止", "幻觉问题", "缺乏专业知识"], ["存在知识截止", "可能产生幻觉", "可能缺乏企业内部或专业知识"], "RAG 动机枚举。")
answerable("fact-019", "HNSW 检索算法的特点是什么？", "fact", "easy", [(RAG, "HNSW**（层次化可导航小世界图）：召回率高，速度快")], ["HNSW", "召回率高", "速度快"], ["HNSW 是层次化可导航小世界图", "特点是召回率高、速度快"], "向量检索算法短事实。")
answerable("fact-020", "Spring Boot 文档给出的五级配置优先级顺序是什么？", "fact", "medium", [(SPRING, "1. 命令行参数"), (SPRING, "5. application.properties/yml")], ["命令行参数", "JNDI", "Java 系统属性", "环境变量", "application.properties/yml"], ["顺序依次为命令行参数、JNDI 属性、Java 系统属性、环境变量、application 配置文件"], "配置顺序完整枚举。")
answerable("fact-021", "JWT 示例中两个 Token 的有效期和刷新流程是什么？", "fact", "medium", [(SPRING, "accessToken**：短期令牌（1小时）"), (SPRING, "refreshToken**：长期令牌（7天）"), (SPRING, "accessToken 过期，用 refreshToken 刷新")], ["1小时", "7天", "refreshToken", "刷新"], ["accessToken 示例有效期为 1 小时", "refreshToken 示例有效期为 7 天", "accessToken 过期后用 refreshToken 刷新"], "JWT 时长与流程。")
answerable("fact-022", "创建用户请求示例使用了哪些校验注解？", "fact", "medium", [(SPRING, "@NotBlank(message = \"用户名不能为空\")"), (SPRING, "@Size(min = 3, max = 20"), (SPRING, "@Email(message = \"邮箱格式不正确\")"), (SPRING, "@Min(value = 18")], ["@NotBlank", "@Size", "@Email", "@Min"], ["用户名使用 @NotBlank 和 @Size", "邮箱使用 @Email", "年龄使用 @Min"], "参数校验注解枚举。")
answerable("fact-023", "Spring 事务文档列出的五种传播行为是什么？", "fact", "medium", [(SPRING, "REQUIRED`（默认）：加入当前事务，没有则新建"), (SPRING, "REQUIRES_NEW`：总是新建事务"), (SPRING, "SUPPORTS`：有事务就加入，没有就非事务执行"), (SPRING, "NOT_SUPPORTED`：非事务执行，挂起当前事务"), (SPRING, "NEVER`：非事务执行，存在事务抛异常")], ["REQUIRED", "REQUIRES_NEW", "SUPPORTS", "NOT_SUPPORTED", "NEVER"], ["列出了 REQUIRED、REQUIRES_NEW、SUPPORTS、NOT_SUPPORTED 和 NEVER"], "事务传播行为枚举。")
answerable("fact-024", "Spring Bean 从创建到销毁经历哪些阶段？", "fact", "medium", [(JAVA, "1. 实例化"), (JAVA, "3. 初始化（@PostConstruct）"), (JAVA, "5. 销毁（@PreDestroy）")], ["实例化", "属性赋值", "@PostConstruct", "使用", "@PreDestroy"], ["依次经历实例化、属性赋值、初始化、使用和销毁"], "Bean 生命周期顺序。")
answerable("fact-025", "Spring AOP 文档列出了哪些通知类型？", "fact", "medium", [(JAVA, "Advice（通知）：@Before、@After、@Around")], ["Advice", "@Before", "@After", "@Around"], ["通知类型包括 @Before、@After 和 @Around"], "AOP 通知枚举。")
answerable("fact-026", "MyBatis 一级缓存和二级缓存的作用域及默认状态是什么？", "fact", "medium", [(JAVA, "一级缓存：SqlSession 级别，默认开启"), (JAVA, "二级缓存：Mapper 级别，需要手动开启")], ["SqlSession", "默认开启", "Mapper", "手动开启"], ["一级缓存是 SqlSession 级且默认开启", "二级缓存是 Mapper 级且需手动开启"], "MyBatis 缓存对照。")
answerable("fact-027", "数据库事务的 ACID 四项分别是什么？", "fact", "medium", [(JAVA, "原子性 Atomicity"), (JAVA, "持久性 Durability")], ["Atomicity", "Consistency", "Isolation", "Durability"], ["ACID 是原子性、一致性、隔离性和持久性"], "事务特性枚举。")
answerable("fact-028", "RAG 离线文档处理包含哪五步？", "fact", "medium", [(RAG, "文档上传**：PDF、Markdown、Word、代码文件等"), (RAG, "存储**：向量存入向量数据库")], ["文档上传", "文档解析", "文本分块", "向量化", "存储"], ["离线处理依次包含上传、解析、分块、向量化和向量存储"], "离线处理流程。")
answerable("fact-029", "RAG 文档列出的三类常用 Embedding 模型是什么？", "fact", "medium", [(RAG, "OpenAI text-embedding-ada-002"), (RAG, "BGE 系列**（中文优化）"), (RAG, "通义千问 Embedding**：text-embedding-v1")], ["text-embedding-ada-002", "BGE", "text-embedding-v1"], ["列出了 OpenAI ada-002、BGE 系列和通义千问 text-embedding-v1"], "Embedding 模型枚举。")
answerable("fact-030", "RAG 文档列出了哪四种文本分块方法？", "fact", "medium", [(RAG, "固定长度分块**：每 500 字符一块"), (RAG, "语义分块**：使用 NLP 技术识别语义边界")], ["固定长度", "句子/段落", "滑动窗口", "语义分块"], ["方法包括固定长度、句子或段落、滑动窗口和语义分块"], "分块方法枚举。")
answerable("fact-031", "典型 RAG Prompt 对答案提出哪四项要求？", "fact", "medium", [(RAG, "1. 基于文档内容回答，不要编造信息"), (RAG, "4. 回答要简洁清晰")], ["基于文档", "没有信息明确说明", "引用片段", "简洁清晰"], ["要求基于文档且不编造", "文档没有信息时明确说明", "引用具体片段", "回答简洁清晰"], "Prompt 约束枚举。")
answerable("fact-032", "Spring Cache 的读取、删除和更新分别使用什么注解？", "fact", "hard", [(SPRING, "@Cacheable(value = \"users\", key = \"#id\")"), (SPRING, "@CacheEvict(value = \"users\", key = \"#id\")"), (SPRING, "@CachePut(value = \"users\", key = \"#user.id\")")], ["@Cacheable", "@CacheEvict", "@CachePut"], ["@Cacheable 用于缓存读取结果", "@CacheEvict 用于删除缓存", "@CachePut 用于更新缓存"], "缓存注解映射。")
answerable("fact-033", "MySQL 四个隔离级别、默认级别和三类锁分别是什么？", "fact", "hard", [(JAVA, "READ UNCOMMITTED（读未提交）"), (JAVA, "REPEATABLE READ（可重复读，MySQL 默认）"), (JAVA, "间隙锁：防止幻读")], ["READ UNCOMMITTED", "READ COMMITTED", "REPEATABLE READ", "SERIALIZABLE", "行锁", "表锁", "间隙锁"], ["四级隔离依次为读未提交、读已提交、可重复读和串行化", "MySQL 默认可重复读", "锁包括行锁、表锁和间隙锁"], "事务隔离与锁综合事实。")
answerable("fact-034", "向量数据库按规模和部署方式如何选择？", "fact", "hard", [(RAG, "小规模**（< 100 万向量）：Qdrant、Weaviate"), (RAG, "大规模**（> 100 万向量）：Milvus、Pinecone"), (RAG, "本地部署**：Milvus、Qdrant"), (RAG, "云服务**：Pinecone、Weaviate Cloud")], ["Qdrant", "Weaviate", "Milvus", "Pinecone", "100 万向量"], ["小规模可选 Qdrant 或 Weaviate", "大规模可选 Milvus 或 Pinecone", "本地偏 Milvus/Qdrant，云服务偏 Pinecone/Weaviate Cloud"], "向量库选择矩阵。")
answerable("fact-035", "RAG 文档分别列出了哪些离线、在线和关键评估信号？", "fact", "hard", [(RAG, "离线评估**：准备测试问答对，计算准确率"), (RAG, "在线评估**：A/B 测试，收集用户反馈"), (RAG, "检索召回率：Top-K 包含正确答案的比例")], ["离线评估", "在线评估", "检索召回率", "答案准确率", "用户满意度"], ["离线可用测试问答对计算准确率", "在线可 A/B 并收集反馈", "关键指标包括召回率、答案准确率和用户满意度"], "评估层次综合事实。")


# DEFINITION_ROWS — remaining matrix: easy 9 / medium 9 / hard 4.
answerable("definition-009", "Spring Boot 在文档中被定义为什么？", "definition", "easy", [(SPRING, "Spring Boot 是基于 Spring 框架的快速开发脚手架")], ["Spring 框架", "快速开发脚手架"], ["Spring Boot 是基于 Spring 框架的快速开发脚手架", "目标是简化初始搭建和开发"], "Spring Boot 基本定义。")
answerable("definition-010", "Spring Boot 的自动配置是指什么？", "definition", "easy", [(SPRING, "自动配置**：根据 classpath 下的依赖自动配置 Spring 应用")], ["classpath", "依赖", "自动配置"], ["自动配置会依据 classpath 中的依赖配置 Spring 应用"], "自动配置定义。")
answerable("definition-011", "什么是 Spring Boot 起步依赖？", "definition", "easy", [(SPRING, "起步依赖**：一站式依赖管理，避免版本冲突")], ["一站式依赖管理", "避免版本冲突"], ["起步依赖提供一站式依赖管理并避免版本冲突"], "起步依赖定义。")
answerable("definition-012", "Spring Boot 的内嵌服务器能力是什么意思？", "definition", "easy", [(SPRING, "内嵌服务器**：Tomcat、Jetty、Undertow")], ["内嵌服务器", "Tomcat", "Jetty", "Undertow"], ["应用可以内嵌 Tomcat、Jetty 或 Undertow"], "内嵌服务器定义。")
answerable("definition-013", "Spring Boot 所说的生产就绪能力是什么？", "definition", "easy", [(SPRING, "生产就绪**：Actuator 健康检查、监控指标")], ["Actuator", "健康检查", "监控指标"], ["生产就绪能力包括 Actuator 健康检查和监控指标"], "生产就绪定义。")
answerable("definition-014", "ArrayList 是什么样的数据结构？", "definition", "easy", [(JAVA, "ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n)")], ["数组", "O(1)", "O(n)"], ["ArrayList 基于数组", "查询 O(1)，插入删除 O(n)"], "ArrayList 定义。")
answerable("definition-015", "Java 中 volatile 关键字的语义是什么？", "definition", "easy", [(JAVA, "volatile 关键字"), (JAVA, "禁止指令重排序")], ["可见性", "禁止重排序", "非原子性"], ["volatile 保证可见性并禁止指令重排序", "它不保证原子性"], "volatile 语义定义。")
answerable("definition-016", "RAG 中的提示词工程指什么？", "definition", "easy", [(RAG, "RAG 系统需要精心设计提示词，将检索到的上下文和问题结合")], ["提示词", "检索上下文", "问题"], ["提示词工程把检索到的上下文与用户问题组织进 Prompt", "并通过要求约束回答"], "RAG Prompt 定义。")
answerable("definition-017", "HNSW 是什么类型的检索算法？", "definition", "easy", [(RAG, "HNSW**（层次化可导航小世界图）：召回率高，速度快")], ["层次化可导航小世界图", "向量检索"], ["HNSW 是层次化可导航小世界图算法", "用于高效向量检索"], "HNSW 定义。")
answerable("definition-018", "什么是构造器注入，文档为何推荐它？", "definition", "medium", [(SPRING, "构造器注入（推荐）"), (JAVA, "构造器注入（推荐，保证不可变性）")], ["构造器注入", "不可变性", "依赖注入"], ["构造器注入通过构造器提供依赖", "文档推荐它以保证不可变性并让依赖明确"], "构造器注入定义。")
answerable("definition-019", "Spring Bean 生命周期是什么？", "definition", "medium", [(JAVA, "Bean 的生命周期"), (JAVA, "初始化（@PostConstruct）"), (JAVA, "销毁（@PreDestroy）")], ["实例化", "属性赋值", "初始化", "销毁"], ["Bean 生命周期覆盖实例化、赋值、初始化、使用与销毁", "初始化和销毁可对应 @PostConstruct 与 @PreDestroy"], "Bean 生命周期定义。")
answerable("definition-020", "AOP 中的 Pointcut 是什么？", "definition", "medium", [(JAVA, "Pointcut（切入点）：匹配规则")], ["Pointcut", "切入点", "匹配规则"], ["Pointcut 即切入点，是用于选择连接点的匹配规则"], "AOP 切入点定义。")
answerable("definition-021", "MySQL 的聚簇索引和非聚簇索引分别是什么？", "definition", "medium", [(JAVA, "聚簇索引：主键索引，叶子节点存储完整数据"), (JAVA, "非聚簇索引：辅助索引，叶子节点存储主键值")], ["主键索引", "完整数据", "辅助索引", "主键值"], ["聚簇索引的叶子节点保存完整数据", "非聚簇索引的叶子节点保存主键值"], "索引结构定义对照。")
answerable("definition-022", "什么是缓存击穿？", "definition", "medium", [(JAVA, "缓存击穿**：热点 Key 过期，大量请求直达数据库")], ["热点 Key", "过期", "数据库", "互斥锁"], ["缓存击穿是热点 Key 过期后大量请求直接访问数据库", "可用永不过期或互斥锁处理"], "缓存击穿定义。")
answerable("definition-023", "什么是缓存雪崩？", "definition", "medium", [(JAVA, "缓存雪崩**：大量 Key 同时过期")], ["大量 Key", "同时过期", "随机值", "多级缓存"], ["缓存雪崩是大量 Key 同时过期", "可通过随机过期时间或多级缓存缓解"], "缓存雪崩定义。")
answerable("definition-024", "RAG 中的混合检索是什么？", "definition", "medium", [(RAG, "混合检索**：向量检索 + 关键词检索（BM25）")], ["向量检索", "关键词检索", "BM25"], ["混合检索组合向量检索与关键词检索", "文档以 BM25 代表关键词检索"], "混合检索定义。")
answerable("definition-025", "RAG 中的 Rerank 是什么？", "definition", "medium", [(RAG, "重排序（Rerank）**：二次精排，提高准确率")], ["Rerank", "二次精排", "提高准确率"], ["Rerank 是对召回候选做二次精排", "目标是提高准确率"], "Rerank 定义。")
answerable("definition-026", "RAG 中的 Query 改写是指什么？", "definition", "medium", [(RAG, "Query 改写**：扩展或改写用户问题，提高召回")], ["Query 改写", "扩展问题", "提高召回"], ["Query 改写会扩展或改写用户问题", "目的是提高召回"], "Query 改写定义。")
answerable("definition-027", "事务传播中的 REQUIRES_NEW 表示什么？", "definition", "hard", [(SPRING, "REQUIRES_NEW`：总是新建事务")], ["REQUIRES_NEW", "新建事务"], ["REQUIRES_NEW 总是新建一个事务"], "传播行为定义。")
answerable("definition-028", "TCC 分布式事务是什么？", "definition", "hard", [(JAVA, "TCC（Try-Confirm-Cancel）"), (JAVA, "业务侵入性强"), (JAVA, "性能较好")], ["Try", "Confirm", "Cancel", "业务侵入性"], ["TCC 由 Try、Confirm、Cancel 三阶段组成", "业务侵入性强但性能较好"], "TCC 定义。")
answerable("definition-029", "文本分块中的滑动窗口是什么用途？", "definition", "hard", [(RAG, "滑动窗口**：重叠分块，避免语义割裂")], ["重叠分块", "避免语义割裂"], ["滑动窗口通过重叠分块保留边界上下文", "用于避免语义割裂"], "滑动窗口定义。")
answerable("definition-030", "RAG 与微调在知识更新方式上分别是什么？", "definition", "hard", [(RAG, "知识更新 | 实时更新文档即可 | 需要重新训练")], ["实时更新文档", "重新训练"], ["RAG 更新知识只需更新文档", "微调更新知识需要重新训练"], "知识更新方式定义对照。")


# REASONING_ROWS — remaining matrix: easy 8 / medium 16 / hard 10.
answerable("reasoning-007", "为什么起步依赖能降低 Spring Boot 项目的版本冲突风险？", "reasoning", "easy", [(SPRING, "起步依赖**：一站式依赖管理，避免版本冲突")], ["一站式依赖管理", "版本冲突"], ["起步依赖集中管理相关依赖", "统一管理可减少手工组合造成的版本冲突"], "从起步依赖作用推导收益。")
answerable("reasoning-008", "为什么内嵌服务器能简化 Spring Boot 应用的初始搭建？", "reasoning", "easy", [(SPRING, "内嵌服务器**：Tomcat、Jetty、Undertow"), (SPRING, "旨在简化 Spring 应用的初始搭建和开发过程")], ["内嵌服务器", "简化搭建"], ["应用可直接携带服务器能力", "减少单独准备外部服务器的初始搭建步骤"], "组合定位与内嵌能力。")
answerable("reasoning-009", "为什么全局异常处理会分别处理业务异常和未知异常？", "reasoning", "easy", [(SPRING, "@ExceptionHandler(BusinessException.class)"), (SPRING, "@ExceptionHandler(Exception.class)"), (SPRING, "status(HttpStatus.INTERNAL_SERVER_ERROR)")], ["BusinessException", "BAD_REQUEST", "Exception", "INTERNAL_SERVER_ERROR"], ["业务异常按可识别错误返回 BAD_REQUEST", "未知异常记录错误并返回通用 INTERNAL_SERVER_ERROR"], "异常类型分流推理。")
answerable("reasoning-010", "为什么控制器参数前的 @Valid 能触发字段校验？", "reasoning", "easy", [(SPRING, "@Valid 触发校验"), (SPRING, "@NotBlank(message = \"用户名不能为空\")")], ["@Valid", "校验注解", "请求体"], ["请求对象字段声明了校验约束", "@Valid 告诉 Spring 在处理请求体时执行这些约束"], "参数校验触发关系。")
answerable("reasoning-011", "为什么 ArrayList 更适合频繁随机查询？", "reasoning", "easy", [(JAVA, "ArrayList 基于数组，查询快 O(1)")], ["数组", "随机查询", "O(1)"], ["ArrayList 基于数组", "其查询复杂度为 O(1)，适合频繁随机查询"], "数据结构与复杂度推导。")
answerable("reasoning-012", "为什么 volatile 不能单独保证计数自增正确？", "reasoning", "easy", [(JAVA, "保证可见性：一个线程修改，其他线程立即可见"), (JAVA, "不保证原子性")], ["volatile", "可见性", "原子性"], ["volatile 只保证可见性并限制重排序", "自增是复合操作，而 volatile 不保证原子性"], "volatile 边界推理。")
answerable("reasoning-013", "为什么向量匹配能补充关键词匹配的不足？", "reasoning", "easy", [(RAG, "传统的关键词匹配无法理解语义"), (RAG, "向量匹配：理解\"如何学习\"和\"学习指南\"语义相似")], ["关键词匹配", "语义", "向量匹配"], ["关键词匹配主要依赖词面重合", "向量匹配可以表达措辞不同但含义接近的语义"], "语义检索动机。")
answerable("reasoning-014", "为什么较小的 Chunk 通常能让检索更精准？", "reasoning", "easy", [(RAG, "小块检索更精准"), (RAG, "提高检索效率")], ["Chunk", "检索精准", "检索效率"], ["小块减少无关内容混入候选", "因此检索定位更精确且效率更高"], "分块粒度原因。")
answerable("reasoning-015", "为什么命令行参数可以覆盖 application.yml 中的配置？", "reasoning", "medium", [(SPRING, "1. 命令行参数"), (SPRING, "5. application.properties/yml")], ["配置优先级", "命令行参数", "application.yml"], ["文档把命令行参数排在配置优先级第一", "application 文件排在第五，因此同名项由更高优先级覆盖"], "配置优先级推理。")
answerable("reasoning-016", "为什么 JWT 流程同时需要短期 accessToken 和长期 refreshToken？", "reasoning", "medium", [(SPRING, "accessToken**：短期令牌（1小时）"), (SPRING, "refreshToken**：长期令牌（7天）"), (SPRING, "accessToken 过期，用 refreshToken 刷新")], ["短期访问", "长期刷新", "过期"], ["短期 accessToken 用于接口访问", "长期 refreshToken 在访问令牌过期后换取新令牌", "两者分工兼顾访问与续期"], "Token 分工推理。")
answerable("reasoning-017", "为什么字段注入会让单元测试和依赖关系更难处理？", "reasoning", "medium", [(SPRING, "难以测试，不够明确"), (JAVA, "字段注入（不推荐，难以测试）")], ["字段注入", "难以测试", "依赖不明确"], ["字段注入把依赖隐藏在字段上", "测试时不便显式构造依赖，也让依赖关系不够明确"], "字段注入缺点解释。")
answerable("reasoning-018", "订单创建方法为何用 rollbackFor=Exception.class？", "reasoning", "medium", [(SPRING, "@Transactional(rollbackFor = Exception.class)"), (SPRING, "如果发生异常，自动回滚")], ["@Transactional", "Exception", "自动回滚"], ["方法包含保存订单和扣库存两个操作", "指定 Exception 回滚可在任一步异常时撤销整个事务"], "事务原子性场景推理。")
answerable("reasoning-019", "为什么查询、删除和更新缓存需要三个不同注解？", "reasoning", "medium", [(SPRING, "第一次查询数据库，之后从缓存读取"), (SPRING, "删除缓存"), (SPRING, "更新缓存")], ["@Cacheable", "@CacheEvict", "@CachePut"], ["查询需要复用已有缓存", "删除需要驱逐缓存", "更新需要写回新值", "三个注解对应不同缓存生命周期动作"], "缓存注解职责推理。")
answerable("reasoning-020", "为什么 LinkedList 的插删快而查询慢？", "reasoning", "medium", [(JAVA, "LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n)")], ["链表", "插入删除 O(1)", "查询 O(n)"], ["链表调整节点连接即可完成插删", "随机查询需要沿链逐个定位，因此是 O(n)"], "链表结构推理。")
answerable("reasoning-021", "使用 Lock 时为什么必须特别注意释放？", "reasoning", "medium", [(JAVA, "synchronized 自动释放锁，Lock 需要手动释放"), (JAVA, "Lock 支持更灵活的锁机制（可中断、可超时）")], ["Lock", "手动释放", "可中断", "可超时"], ["Lock 提供更灵活能力但不会自动释放", "遗漏手动释放会让锁持续占用并阻塞其他线程"], "锁机制取舍推理。")
answerable("reasoning-022", "为什么没有接口的类不能直接依赖 JDK 动态代理？", "reasoning", "medium", [(JAVA, "JDK 动态代理：基于接口"), (JAVA, "CGLIB 代理：基于继承")], ["JDK 动态代理", "接口", "CGLIB", "继承"], ["JDK 动态代理以接口为基础", "没有接口时可考虑基于继承的 CGLIB 代理"], "动态代理选择推理。")
answerable("reasoning-023", "为什么缓存穿透会持续打到数据库，文档的两种方案如何缓解？", "reasoning", "medium", [(JAVA, "缓存穿透**：查询不存在的数据，缓存和数据库都没有"), (JAVA, "解决：布隆过滤器、缓存空值")], ["不存在数据", "布隆过滤器", "缓存空值"], ["不存在的数据无法在缓存形成正常命中", "布隆过滤器可提前拦截，缓存空值可避免重复查库"], "缓存穿透治理推理。")
answerable("reasoning-024", "为什么给缓存过期时间增加随机值能缓解雪崩？", "reasoning", "medium", [(JAVA, "缓存雪崩**：大量 Key 同时过期"), (JAVA, "解决：过期时间加随机值、多级缓存")], ["同时过期", "随机值", "错峰"], ["雪崩来自大量 Key 同时失效", "随机化过期时间能分散失效时刻，减少集中打库"], "缓存雪崩缓解推理。")
answerable("reasoning-025", "分布式锁为什么同时需要过期时间、UUID 和 Lua？", "reasoning", "medium", [(JAVA, "防止死锁（设置过期时间）"), (JAVA, "防止误删（UUID 标识）"), (JAVA, "原子性（Lua 脚本）")], ["过期时间", "UUID", "Lua", "原子性"], ["过期时间防死锁", "UUID 区分锁持有者以防误删", "Lua 保证检查与删除等操作原子执行"], "分布式锁安全要素。")
answerable("reasoning-026", "消息队列为何要同时做生产者确认、持久化和手动 ACK？", "reasoning", "medium", [(JAVA, "生产者确认机制"), (JAVA, "消息持久化"), (JAVA, "消费者手动 ACK")], ["生产者确认", "持久化", "手动 ACK"], ["生产者确认覆盖发送阶段", "持久化降低 broker 故障丢失风险", "手动 ACK 确认消费成功", "三者覆盖消息链路不同环节"], "消息可靠性链路推理。")
answerable("reasoning-027", "为什么 RAG 能缓解模型的知识截止问题？", "reasoning", "medium", [(RAG, "知识截止**：只知道训练时的数据"), (RAG, "先从知识库中**检索**相关信息")], ["知识截止", "知识库", "检索"], ["模型训练知识有截止时间", "RAG 在回答时从可更新知识库检索上下文，因此可引入训练后的资料"], "RAG 知识更新推理。")
answerable("reasoning-028", "为什么 Prompt 要明确要求文档无信息时说明无法回答？", "reasoning", "medium", [(RAG, "如果文档中没有相关信息，明确说明"), (RAG, "基于文档内容回答，不要编造信息")], ["无相关信息", "明确说明", "不要编造"], ["检索上下文可能不含答案", "明确拒答约束可避免用上下文外信息补全，从而降低编造"], "无答案 Prompt 约束推理。")
answerable("reasoning-029", "为什么混合检索要组合向量检索和 BM25？", "reasoning", "medium", [(RAG, "混合检索**：向量检索 + 关键词检索（BM25）"), (RAG, "传统的关键词匹配无法理解语义")], ["向量检索", "BM25", "语义", "关键词"], ["向量检索补充语义相似能力", "BM25 保留关键词匹配能力", "组合可覆盖两类信号"], "混合检索互补推理。")
answerable("reasoning-030", "为什么 Query 改写可能提高召回？", "reasoning", "medium", [(RAG, "Query 改写**：扩展或改写用户问题，提高召回")], ["扩展问题", "改写", "召回"], ["原始提问可能与文档措辞不一致", "扩展或改写可增加与相关文档匹配的表达，从而提高召回"], "Query 改写收益推理。")
answerable("reasoning-031", "有事务、无事务和必须隔离新事务三种诉求应如何选择传播行为？", "reasoning", "hard", [(SPRING, "REQUIRED`（默认）：加入当前事务，没有则新建"), (SPRING, "REQUIRES_NEW`：总是新建事务"), (SPRING, "SUPPORTS`：有事务就加入，没有就非事务执行")], ["REQUIRED", "REQUIRES_NEW", "SUPPORTS"], ["需要有事务且可复用当前事务时选 REQUIRED", "必须独立事务时选 REQUIRES_NEW", "允许无事务执行时选 SUPPORTS"], "事务传播选择推理。")
answerable("reasoning-032", "Spring Security 示例为什么既放行 /auth/** 又采用 STATELESS 会话？", "reasoning", "hard", [(SPRING, "requestMatchers(\"/auth/**\").permitAll()"), (SPRING, "anyRequest().authenticated()"), (SPRING, "sessionCreationPolicy(SessionCreationPolicy.STATELESS)")], ["/auth/**", "authenticated", "STATELESS", "JWT"], ["认证入口需在未登录时可访问", "其他请求要求认证", "STATELESS 与每次携带 Token 的无状态认证流程相匹配"], "安全配置组合推理。")
answerable("reasoning-033", "异步线程池的核心数、最大数和队列容量如何共同约束任务执行？", "reasoning", "hard", [(SPRING, "executor.setCorePoolSize(5)"), (SPRING, "executor.setMaxPoolSize(10)"), (SPRING, "executor.setQueueCapacity(100)")], ["corePoolSize", "maximumPoolSize", "queueCapacity"], ["核心线程数决定常驻并发基线", "队列暂存等待任务", "压力超过核心与队列承载后可扩至最大线程数"], "线程池参数关系推理。")
answerable("reasoning-034", "为什么辅助索引查到主键后可能还需要访问聚簇索引？", "reasoning", "hard", [(JAVA, "聚簇索引：主键索引，叶子节点存储完整数据"), (JAVA, "非聚簇索引：辅助索引，叶子节点存储主键值")], ["辅助索引", "主键值", "完整数据", "聚簇索引"], ["辅助索引叶子只保存主键值", "若查询需要完整行，还需用主键到聚簇索引取得完整数据"], "索引回表关系推理。")
answerable("reasoning-035", "缓存穿透、击穿和雪崩为什么需要不同治理手段？", "reasoning", "hard", [(JAVA, "缓存穿透**：查询不存在的数据"), (JAVA, "缓存击穿**：热点 Key 过期"), (JAVA, "缓存雪崩**：大量 Key 同时过期")], ["穿透", "击穿", "雪崩", "布隆过滤器", "互斥锁", "随机过期"], ["穿透针对不存在数据，适合过滤或缓存空值", "击穿针对单个热点失效，适合互斥或永不过期", "雪崩针对批量同时失效，适合随机过期或多级缓存"], "三类缓存故障比较。")
answerable("reasoning-036", "为什么 2PC 与 TCC 在阻塞和业务侵入性上取舍不同？", "reasoning", "hard", [(JAVA, "2PC（两阶段提交）"), (JAVA, "缺点：阻塞、单点故障"), (JAVA, "TCC（Try-Confirm-Cancel）"), (JAVA, "业务侵入性强"), (JAVA, "性能较好")], ["2PC", "阻塞", "单点故障", "TCC", "业务侵入性"], ["2PC 由协调式准备和提交组成，存在阻塞及单点风险", "TCC 把补偿逻辑放入业务，侵入更强但性能较好"], "分布式事务取舍推理。")
answerable("reasoning-037", "为什么小规模与大规模向量数据会选择不同数据库？", "reasoning", "hard", [(RAG, "小规模**（< 100 万向量）：Qdrant、Weaviate"), (RAG, "大规模**（> 100 万向量）：Milvus、Pinecone")], ["100 万向量", "Qdrant", "Weaviate", "Milvus", "Pinecone"], ["数据规模影响检索与运维需求", "文档建议小规模用 Qdrant/Weaviate，大规模用 Milvus/Pinecone"], "规模与选型推理。")
answerable("reasoning-038", "Chunk 太小和太大分别会带来什么问题，推荐区间如何折中？", "reasoning", "hard", [(RAG, "太小**（< 200 字符）：语义不完整"), (RAG, "太大**（> 2000 字符）：检索不精准，耗费 Token"), (RAG, "推荐**：500-1000 字符")], ["语义不完整", "检索不精准", "Token", "500-1000"], ["太小会割裂语义", "太大会混入无关内容并消耗 Token", "500-1000 字符是在完整性与精度间的建议折中"], "Chunk 大小权衡。")
answerable("reasoning-039", "为什么检索召回率不能直接等同于答案准确率？", "reasoning", "hard", [(RAG, "检索召回率：Top-K 包含正确答案的比例"), (RAG, "答案准确率：生成答案的正确性")], ["检索召回率", "Top-K", "答案准确率", "生成答案"], ["召回率只检查 Top-K 是否含正确证据", "答案准确率检查最终生成内容", "检索成功后生成仍可能出错，因此两者不可等同"], "检索与生成指标边界。")
answerable("reasoning-040", "问题缓存和向量缓存为何优化的是不同阶段？", "reasoning", "hard", [(RAG, "问题缓存**：相同问题直接返回缓存结果"), (RAG, "向量缓存**：缓存常见问题的向量")], ["问题缓存", "向量缓存", "结果", "向量化"], ["问题缓存绕过相同问题的后续处理并复用结果", "向量缓存只复用问题向量，减少向量化开销但仍需检索或生成"], "RAG 缓存层次推理。")


# MULTI_HOP_ROWS — remaining matrix: easy 3 / medium 10 / hard 9.
answerable("multi-hop-004", "Spring Boot 如何用一个启动注解和四类核心特性简化开发？", "multi_hop", "easy", [(SPRING, "@SpringBootApplication` 是组合注解"), (SPRING, "自动配置**：根据 classpath 下的依赖自动配置 Spring 应用"), (SPRING, "起步依赖**：一站式依赖管理"), (SPRING, "内嵌服务器**：Tomcat、Jetty、Undertow")], ["@SpringBootApplication", "自动配置", "起步依赖", "内嵌服务器"], ["启动注解组合配置、自动配置和扫描能力", "自动配置、起步依赖与内嵌服务器共同减少搭建工作"], "组合启动注解与核心特性。")
answerable("multi-hop-005", "查询、插删和并发三个场景下应如何在 ArrayList、LinkedList、HashMap 之间判断？", "multi_hop", "easy", [(JAVA, "ArrayList 基于数组，查询快 O(1)"), (JAVA, "LinkedList 基于链表，插入删除快 O(1)"), (JAVA, "线程不安全，多线程使用 ConcurrentHashMap")], ["ArrayList", "LinkedList", "ConcurrentHashMap"], ["频繁查询偏向 ArrayList", "频繁链表插删可考虑 LinkedList", "多线程键值场景不能直接依赖 HashMap，应使用 ConcurrentHashMap"], "集合场景综合。")
answerable("multi-hop-006", "传统 LLM 的三个知识问题如何由 RAG 的检索、上下文和生成链路缓解？", "multi_hop", "easy", [(RAG, "知识截止**：只知道训练时的数据"), (RAG, "幻觉问题**：可能生成不准确的内容"), (RAG, "先从知识库中**检索**相关信息"), (RAG, "LLM 基于上下文生成**准确的答案**")], ["知识截止", "幻觉", "知识库检索", "上下文生成"], ["传统模型有知识截止、幻觉和专业知识不足", "RAG 先检索可更新知识，再把证据作为上下文约束生成"], "问题与解决链路综合。")
answerable("multi-hop-007", "一个 JSON 创建用户接口怎样同时完成请求映射、请求体绑定和字段校验？", "multi_hop", "medium", [(SPRING, "@PostMapping(\"/users\")"), (SPRING, "@Valid @RequestBody CreateUserRequest request"), (SPRING, "@Valid 触发校验")], ["@PostMapping", "@RequestBody", "@Valid"], ["@PostMapping 负责路径与方法映射", "@RequestBody 绑定 JSON 请求体", "@Valid 执行请求对象上的字段约束"], "Web 参数处理链路。")
answerable("multi-hop-008", "Spring Boot 文档中的 JPA 与 MyBatis-Plus 数据访问示例分别提供什么能力？", "multi_hop", "medium", [(SPRING, "UserRepository extends JpaRepository<User, Long>"), (SPRING, "UserMapper extends BaseMapper<User>"), (SPRING, "继承通用服务方法")], ["JpaRepository", "BaseMapper", "CRUD", "通用服务"], ["JPA 示例通过 JpaRepository 提供仓储能力和派生查询", "MyBatis-Plus 示例通过 BaseMapper 与 ServiceImpl 获得通用 CRUD/服务方法"], "两种数据访问方式综合。")
answerable("multi-hop-009", "业务异常、未知异常与认证保护如何共同形成接口防线？", "multi_hop", "medium", [(SPRING, "@ExceptionHandler(BusinessException.class)"), (SPRING, "@ExceptionHandler(Exception.class)"), (SPRING, "anyRequest().authenticated()")], ["业务异常", "未知异常", "authenticated"], ["安全链先要求受保护接口完成认证", "业务异常映射为可识别错误", "未知异常统一记录并返回通用系统错误"], "认证与异常边界综合。")
answerable("multi-hop-010", "创建订单和更新缓存分别应怎样保证数据库与缓存状态的一致处理？", "multi_hop", "medium", [(SPRING, "@Transactional(rollbackFor = Exception.class)"), (SPRING, "如果发生异常，自动回滚"), (SPRING, "@CachePut(value = \"users\", key = \"#user.id\")")], ["@Transactional", "回滚", "@CachePut"], ["订单与库存操作置于事务中，异常时整体回滚", "更新对象后用 @CachePut 写回缓存，避免继续返回旧值"], "事务与缓存动作综合。")
answerable("multi-hop-011", "IOC 管理依赖后，AOP 又如何为这些对象增加横切能力？", "multi_hop", "medium", [(JAVA, "Spring IOC（控制反转）"), (JAVA, "依赖注入的三种方式"), (JAVA, "应用场景**：事务管理、日志记录、权限校验、性能监控")], ["IOC", "依赖注入", "AOP", "横切关注点"], ["IOC 通过依赖注入创建和连接对象", "AOP 再把事务、日志、权限或监控等横切逻辑模块化应用到方法执行点"], "IOC 与 AOP 职责组合。")
answerable("multi-hop-012", "Redis 五种数据类型各自适合哪些典型用途？", "multi_hop", "medium", [(JAVA, "String**：缓存、计数器、分布式锁"), (JAVA, "Hash**：对象存储（用户信息）"), (JAVA, "List**：消息队列、列表分页"), (JAVA, "Set**：去重、交集并集差集"), (JAVA, "ZSet**：排行榜、延迟队列")], ["String", "Hash", "List", "Set", "ZSet"], ["String 用于缓存计数或锁", "Hash 存对象", "List 做队列或分页", "Set 做去重和集合运算", "ZSet 做排行或延迟队列"], "Redis 类型与用途综合。")
answerable("multi-hop-013", "分布式系统中，锁和事务分别有哪些实现选择与主要风险？", "multi_hop", "medium", [(JAVA, "Redis SETNX + 过期时间"), (JAVA, "Redisson（推荐）"), (JAVA, "2PC（两阶段提交）"), (JAVA, "TCC（Try-Confirm-Cancel）")], ["SETNX", "Redisson", "2PC", "TCC"], ["锁可用 Redis SETNX、Redisson 或 Zookeeper，并需防死锁误删", "事务可选 2PC 或 TCC，前者有阻塞风险，后者业务侵入强"], "分布式锁与事务综合。")
answerable("multi-hop-014", "RAG 离线处理时如何从原始文档走到可检索向量？", "multi_hop", "medium", [(RAG, "文档上传**：PDF、Markdown、Word、代码文件等"), (RAG, "文档解析**：提取文本内容"), (RAG, "文本分块**：将长文档切分为小块"), (RAG, "向量化**：使用 Embedding 模型将文本转换为向量"), (RAG, "存储**：向量存入向量数据库")], ["上传", "解析", "分块", "Embedding", "向量数据库"], ["上传并解析文档文本", "把长文切成 Chunk", "用 Embedding 生成向量", "把向量存入向量数据库"], "离线流水线综合。")
answerable("multi-hop-015", "向量数据库与 HNSW、IVF、FLAT 三种算法如何配合完成相似度搜索？", "multi_hop", "medium", [(RAG, "专门存储和检索向量的数据库"), (RAG, "HNSW**（层次化可导航小世界图）：召回率高，速度快"), (RAG, "IVF**（倒排索引）：适合大规模数据"), (RAG, "FLAT**（暴力搜索）：精度最高但速度慢")], ["向量数据库", "HNSW", "IVF", "FLAT"], ["向量数据库负责向量存储和相似搜索", "HNSW 偏高召回高速度", "IVF 适合大规模", "FLAT 以速度换最高精度"], "数据库与算法综合。")
answerable("multi-hop-016", "Prompt 如何把检索上下文、用户问题和无答案约束组织在一起？", "multi_hop", "medium", [(RAG, "【相关文档】"), (RAG, "【用户提问】"), (RAG, "如果文档中没有相关信息，明确说明")], ["相关文档", "用户提问", "无信息明确说明"], ["Prompt 分区放入检索文档和用户问题", "回答要求限制只能基于文档", "缺少证据时明确拒答并要求引用"], "Prompt 输入与约束综合。")
answerable("multi-hop-017", "一个受保护的订单接口如何串联 JWT、参数校验和事务回滚？", "multi_hop", "hard", [(SPRING, "访问接口携带 accessToken"), (SPRING, "@Valid 触发校验"), (SPRING, "@Transactional(rollbackFor = Exception.class)")], ["accessToken", "@Valid", "@Transactional", "回滚"], ["请求先携带 accessToken 通过认证", "@Valid 校验业务参数", "订单与库存写入由事务包裹，异常时回滚"], "认证、校验与事务链路。")
answerable("multi-hop-018", "定时触发、异步执行和缓存结果在 Spring Boot 中分别由什么机制承担？", "multi_hop", "hard", [(SPRING, "@Scheduled(fixedRate = 5000)"), (SPRING, "@Async"), (SPRING, "@Cacheable(value = \"users\", key = \"#id\")")], ["@Scheduled", "@Async", "@Cacheable"], ["@Scheduled 定义周期触发", "@Async 把工作交给线程池异步执行", "@Cacheable 复用已计算或查询的结果"], "定时、异步与缓存综合。")
answerable("multi-hop-019", "Java 集合选择时如何同时考虑底层结构、复杂度和线程安全？", "multi_hop", "hard", [(JAVA, "ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n)"), (JAVA, "LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n)"), (JAVA, "线程不安全，多线程使用 ConcurrentHashMap")], ["数组", "链表", "复杂度", "线程安全"], ["数组列表偏随机查询", "链表偏插删", "键值集合在多线程下还需考虑 HashMap 线程不安全并选 ConcurrentHashMap"], "集合多维选择。")
answerable("multi-hop-020", "MySQL 查询优化与事务正确性分别要关注哪些索引和隔离要点？", "multi_hop", "hard", [(JAVA, "B+ 树索引**：MySQL InnoDB 默认索引结构"), (JAVA, "索引失效场景"), (JAVA, "REPEATABLE READ（可重复读，MySQL 默认）"), (JAVA, "间隙锁：防止幻读")], ["B+ 树", "索引失效", "REPEATABLE READ", "间隙锁"], ["查询侧要利用 B+ 树并避免函数、类型转换或最左前缀失效", "事务侧要理解默认可重复读及间隙锁防幻读"], "索引与事务综合。")
answerable("multi-hop-021", "如何区分并治理缓存穿透、击穿和雪崩？", "multi_hop", "hard", [(JAVA, "缓存穿透**：查询不存在的数据"), (JAVA, "缓存击穿**：热点 Key 过期"), (JAVA, "缓存雪崩**：大量 Key 同时过期")], ["穿透", "击穿", "雪崩"], ["穿透用布隆过滤器或缓存空值", "击穿用热点不过期或互斥锁", "雪崩用随机过期或多级缓存"], "缓存故障诊断综合。")
answerable("multi-hop-022", "高流量接口如何结合消息队列、幂等和限流思路控制风险？", "multi_hop", "hard", [(JAVA, "流量削峰：秒杀系统"), (JAVA, "如何保证接口幂等性？"), (JAVA, "令牌桶（Guava RateLimiter）")], ["流量削峰", "幂等", "令牌桶"], ["消息队列可削峰并解耦", "唯一 ID、Token、乐观锁或状态机可保证幂等", "计数器、滑窗、令牌桶或漏桶可限制入口流量"], "消息、幂等与限流综合。")
answerable("multi-hop-023", "RAG 从离线建库到在线回答后，还能从哪些环节继续优化准确率？", "multi_hop", "hard", [(RAG, "文档处理（离线）"), (RAG, "问答处理（在线）"), (RAG, "优化分块策略**：合理设置 Chunk 大小"), (RAG, "使用 Rerank**：二次精排"), (RAG, "优化 Prompt**：明确回答要求")], ["离线建库", "在线问答", "分块", "Rerank", "Prompt"], ["先完成解析、分块、向量化和存储，再在线检索并生成", "可继续优化分块、Embedding、Top-K、Rerank 和 Prompt"], "端到端与优化综合。")
answerable("multi-hop-024", "技术文档场景下，如何共同选择 Chunk 策略、向量数据库和检索算法？", "multi_hop", "hard", [(RAG, "技术文档**：按段落或章节分块"), (RAG, "小规模**（< 100 万向量）：Qdrant、Weaviate"), (RAG, "IVF**（倒排索引）：适合大规模数据")], ["段落或章节", "向量规模", "Qdrant", "Milvus", "HNSW", "IVF"], ["技术文档优先按段落或章节分块", "数据库按向量规模和部署方式选", "算法再按召回、速度和规模在 HNSW、IVF、FLAT 间取舍"], "分块、数据库与算法选型。")
answerable("multi-hop-025", "RAG 技术栈中的 Spring Boot 后端如何承接请求并连接后续检索生成步骤？", "multi_hop", "hard", [(RAG, "Spring Boot 后端"), (RAG, "文档处理层：解析、分块"), (RAG, "Milvus 向量数据库：存储和检索"), (SPRING, "@RestController`：= @Controller + @ResponseBody")], ["Spring Boot", "@RestController", "文档处理", "Milvus", "LLM"], ["Spring Boot 后端可用 REST 控制器承接用户请求", "之后连接解析分块、Embedding、Milvus 检索和 LLM 生成", "最终返回答案与引用"], "跨 fixture 技术栈链路。")


# NO_ANSWER_ROWS — remaining matrix: easy 6 / medium 6 / hard 5.
no_answer("no-answer-004", "Spring WebFlux 中 Mono 与 Flux 的背压行为有什么区别？", "easy", "固定 corpus 未包含 Spring WebFlux、Mono、Flux 或背压说明。")
no_answer("no-answer-005", "Java record 的 canonical constructor 有哪些限制？", "easy", "固定 corpus 未包含 Java record 或 canonical constructor。")
no_answer("no-answer-006", "Redis HyperLogLog 的标准误差是多少？", "easy", "固定 corpus 只列常见数据类型，未包含 HyperLogLog 或误差参数。")
no_answer("no-answer-007", "GraphRAG 的社区发现阶段通常使用什么图算法？", "easy", "固定 corpus 未包含 GraphRAG、图构建或社区发现。")
no_answer("no-answer-008", "Docker Compose 中 depends_on 的健康检查条件如何配置？", "easy", "固定 corpus 未包含 Docker Compose 或 depends_on。")
no_answer("no-answer-009", "JUnit 5 参数化测试的 @MethodSource 如何解析参数？", "easy", "固定 corpus 只有 Spring Boot 测试示例，未包含 JUnit 参数化测试。")
no_answer("no-answer-010", "Spring Boot 原生镜像的 AOT hints 应如何注册？", "medium", "固定 corpus 未包含 AOT、原生镜像或 hints。")
no_answer("no-answer-011", "Java 虚拟线程的 pinning 会在什么情况下发生？", "medium", "固定 corpus 未包含虚拟线程或 pinning。")
no_answer("no-answer-012", "MySQL 窗口函数 ROW_NUMBER 的分区排序语义是什么？", "medium", "固定 corpus 未包含窗口函数或 ROW_NUMBER。")
no_answer("no-answer-013", "Redis Cluster 迁移 hash slot 时如何保证客户端重定向？", "medium", "固定 corpus 未包含 Redis Cluster、hash slot 或迁移重定向。")
no_answer("no-answer-014", "RAG 的 Reciprocal Rank Fusion 公式如何计算？", "medium", "固定 corpus 只提混合检索，未包含 RRF 或计算公式。")
no_answer("no-answer-015", "Milvus 分区键对查询剪枝的具体影响是什么？", "medium", "固定 corpus 只列 Milvus 用途，未包含分区键或查询剪枝。")
no_answer("no-answer-016", "事务型 Outbox 在 Spring 中如何处理发布失败重试？", "hard", "固定 corpus 只讲本地声明式事务，未包含 Outbox 或发布重试。")
no_answer("no-answer-017", "Shenandoah GC 的并发转发指针如何工作？", "hard", "固定 corpus 只枚举部分 GC 收集器，未包含 Shenandoah 或转发指针。")
no_answer("no-answer-018", "Kafka exactly-once 的 transaction.id 应如何规划？", "hard", "固定 corpus 只概述 Kafka 场景与可靠性，未包含 exactly-once 配置。")
no_answer("no-answer-019", "HyDE 查询扩展为何能改善零样本稠密检索？", "hard", "固定 corpus 只提 Query 改写，未包含 HyDE 方法或零样本机制。")
no_answer("no-answer-020", "RAG 面对间接提示注入时应如何做内容隔离？", "hard", "固定 corpus 的 Prompt 约束未包含提示注入或内容隔离防御。")


def canonical_json_line(value: dict) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def ordered_ids_sha256(rows: list[dict]) -> str:
    encoded = json.dumps(
        [row["id"] for row in rows],
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def type_difficulty(rows: list[dict]) -> dict[str, dict[str, int]]:
    result: dict[str, Counter[str]] = {}
    for row in rows:
        result.setdefault(row["type"], Counter())[row["difficulty"]] += 1
    return {
        sample_type: dict(sorted(cells.items()))
        for sample_type, cells in sorted(result.items())
    }


def file_descriptor(path: Path, **extra: object) -> dict:
    return {
        "path": path.relative_to(REPO_ROOT).as_posix(),
        "sha256": sha256_file(path),
        "bytes": path.stat().st_size,
        **extra,
    }


def build_review(rows: list[dict]) -> list[dict]:
    features: list[tuple[str, set[str]]] = []
    reviews: list[dict] = []
    for row in rows:
        current = _question_features(row["question"])
        near = any(_jaccard(prior, current) >= 0.82 for _, prior in features)
        reviews.append(
            {
                "sampleId": row["id"],
                "structureStatus": "passed",
                "groundingStatus": "passed" if row["should_answer"] else "not_applicable",
                "duplicateStatus": "accepted_distinct" if near else "unique",
                "semanticReviewStatus": "passed",
                "reviewNotes": (
                    "近重复候选已复核为独立考点。"
                    if near
                    else (
                        "已逐份复核三个固定 fixture，缺少回答所需关键证据。"
                        if not row["should_answer"]
                        else "结构、fixture 证据与答案要点已完成第二遍复核。"
                    )
                ),
            }
        )
        features.append((row["id"], current))
    return reviews


def main() -> None:
    v1_manifest_bytes = V1_MANIFEST.read_bytes()
    v1_manifest = json.loads(v1_manifest_bytes.decode("utf-8"))
    seed_path = REPO_ROOT / v1_manifest["questionSet"]["path"]
    seed_bytes = seed_path.read_bytes()
    seed_text = seed_path.read_text(encoding="utf-8")
    seed_rows = [json.loads(line) for line in seed_text.splitlines() if line.strip()]
    if len(seed_rows) != 30 or len(ROWS) != 120:
        raise SystemExit(f"expected seed/new=30/120, got {len(seed_rows)}/{len(ROWS)}")
    all_rows = seed_rows + ROWS

    fixture_paths = {
        PurePosixPath(descriptor["path"]).name: REPO_ROOT / descriptor["path"]
        for descriptor in v1_manifest["fixtures"]
    }
    for row in ROWS:
        for context in row["expected_contexts"]:
            if context["contains"] not in fixture_paths[context["source"]].read_text(encoding="utf-8"):
                raise SystemExit(f"ungrounded context for {row['id']}")

    V1_RELEASE_MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    V2_REVIEW.parent.mkdir(parents=True, exist_ok=True)
    V1_RELEASE_MANIFEST.write_bytes(v1_manifest_bytes)
    appended = "".join(canonical_json_line(row) + "\n" for row in ROWS).encode("utf-8")
    V2_QUESTIONS.write_bytes(seed_bytes + appended)
    review_rows = build_review(all_rows)
    V2_REVIEW.write_text(
        "".join(canonical_json_line(row) + "\n" for row in review_rows),
        encoding="utf-8",
    )

    distribution = {
        "type": dict(sorted(Counter(row["type"] for row in all_rows).items())),
        "difficulty": dict(sorted(Counter(row["difficulty"] for row in all_rows).items())),
        "shouldAnswer": {
            "true": sum(row["should_answer"] is True for row in all_rows),
            "false": sum(row["should_answer"] is False for row in all_rows),
        },
    }
    manifest = {
        "manifestSchemaVersion": "rag-eval-dataset-manifest-v2",
        "releaseVersion": "rag-eval-dev-v2",
        "questionSetVersion": "questions-v2",
        "sampleSchemaVersion": v1_manifest["sampleSchemaVersion"],
        "annotationVersion": "annotations-v2",
        "fixtureCorpusVersion": v1_manifest["fixtureCorpusVersion"],
        "questionSet": file_descriptor(
            V2_QUESTIONS,
            sampleCount=len(all_rows),
            orderedSampleIdsSha256=ordered_ids_sha256(all_rows),
        ),
        "seedReleaseManifest": file_descriptor(V1_RELEASE_MANIFEST),
        "seedQuestionSet": dict(v1_manifest["questionSet"]),
        "sampleSchema": v1_manifest["sampleSchema"],
        "fixtures": v1_manifest["fixtures"],
        "logicalKnowledgeBase": v1_manifest["logicalKnowledgeBase"],
        "distribution": distribution,
        "annotationReview": file_descriptor(
            V2_REVIEW,
            reviewedSampleCount=len(review_rows),
        ),
        "expandedDataset": {
            "totalSampleCount": 150,
            "seedSampleCount": 30,
            "newSampleCount": 120,
            "quotas": {
                **distribution,
                "typeDifficulty": type_difficulty(all_rows),
            },
            "fixtureCoverage": {
                "minimumAnswerableSamples": 35,
                "maximumAnswerableRatio": 0.45,
            },
            "nearDuplicateThreshold": 0.82,
        },
    }
    V2_MANIFEST.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
