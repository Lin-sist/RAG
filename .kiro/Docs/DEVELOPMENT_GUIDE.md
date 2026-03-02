# 开发规范文档

本文档定义了 Enterprise RAG QA System 项目的开发规范和最佳实践。

---

## 📁 项目结构规范

### 目录结构

```
rag-{module}/
├── src/
│   ├── main/
│   │   ├── java/com/enterprise/rag/{module}/
│   │   │   ├── config/           # 配置类
│   │   │   ├── controller/       # 控制器
│   │   │   ├── service/          # 服务接口
│   │   │   │   └── impl/         # 服务实现
│   │   │   ├── entity/           # 数据库实体
│   │   │   ├── dto/              # 数据传输对象
│   │   │   │   ├── request/      # 请求 DTO
│   │   │   │   └── response/     # 响应 DTO
│   │   │   ├── mapper/           # MyBatis Mapper
│   │   │   ├── exception/        # 异常类
│   │   │   └── constant/         # 常量类
│   │   └── resources/
│   │       ├── mapper/           # Mapper XML
│   │       └── application.yml   # 配置文件
│   └── test/
│       └── java/
│           └── com/enterprise/rag/{module}/
│               ├── unit/         # 单元测试
│               ├── integration/  # 集成测试
│               └── property/     # 属性测试
└── pom.xml
```

### 包命名规则

| 包名 | 用途 | 示例 |
|------|------|------|
| config | Spring 配置类 | `SecurityConfig.java` |
| controller | REST 控制器 | `QAController.java` |
| service | 服务接口 | `RAGService.java` |
| service.impl | 服务实现 | `RAGServiceImpl.java` |
| entity | 数据库实体 | `KnowledgeBase.java` |
| dto | 数据传输对象 | `CreateKBRequest.java` |
| mapper | MyBatis Mapper | `KnowledgeBaseMapper.java` |
| exception | 自定义异常 | `BusinessException.java` |
| constant | 常量定义 | `RedisKeyConstants.java` |

---

## 📝 命名规范

### 类命名

| 类型 | 规则 | 示例 |
|------|------|------|
| 类/接口 | 大驼峰 | `KnowledgeBaseService` |
| 抽象类 | Abstract 前缀 | `AbstractDocumentParser` |
| 实现类 | Impl 后缀 | `RAGServiceImpl` |
| 异常类 | Exception 后缀 | `BusinessException` |
| 测试类 | Test 后缀 | `RAGServiceTest` |
| 配置类 | Config 后缀 | `SecurityConfig` |
| 常量类 | Constants 后缀 | `RedisKeyConstants` |

### 方法命名

| 类型 | 规则 | 示例 |
|------|------|------|
| 获取单个对象 | `get` 前缀 | `getById(Long id)` |
| 获取列表 | `list` 前缀 | `listByKbId(Long kbId)` |
| 查询数量 | `count` 前缀 | `countByStatus(String status)` |
| 新增 | `create` / `save` | `create(Request req)` |
| 修改 | `update` | `update(Long id, Request req)` |
| 删除 | `delete` / `remove` | `delete(Long id)` |
| 判断 | `is` / `has` / `can` | `isExist(String name)` |

### 变量命名

```java
// 普通变量：小驼峰
String userName;
int documentCount;

// 常量：全大写下划线分隔
public static final String TRACE_ID_HEADER = "X-Trace-Id";
public static final int DEFAULT_PAGE_SIZE = 10;

// 集合变量：复数形式或带 List/Map 后缀
List<Document> documents;
Map<String, Object> metadataMap;
```

---

## 💾 数据库规范

### 表命名

- 使用小写字母
- 单词间用下划线分隔
- 使用有意义的英文名称

```sql
-- 正确
CREATE TABLE knowledge_base (...);
CREATE TABLE document_chunk (...);

-- 错误
CREATE TABLE KB (...);
CREATE TABLE doc (...);
```

### 字段命名

| 场景 | 规则 | 示例 |
|------|------|------|
| 主键 | `id` | `id BIGINT AUTO_INCREMENT` |
| 外键 | `{表名}_id` | `kb_id`, `user_id` |
| 创建时间 | `created_at` | `created_at DATETIME` |
| 更新时间 | `updated_at` | `updated_at DATETIME` |
| 逻辑删除 | `deleted` | `deleted TINYINT(1)` |
| 乐观锁 | `version` | `version INT` |

### 索引规范

```sql
-- 主键索引
PRIMARY KEY (`id`)

-- 唯一索引：uk_ 前缀
UNIQUE KEY `uk_username` (`username`)

-- 普通索引：idx_ 前缀
KEY `idx_kb_id` (`kb_id`)

-- 联合索引：按选择性从高到低
KEY `idx_kb_status` (`kb_id`, `status`)
```

---

## 🎯 代码规范

### 控制器规范

```java
@Slf4j
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库 CRUD 接口")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情")
    public ResponseEntity<ApiResponse<KnowledgeBaseDTO>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        
        log.info("获取知识库详情: id={}", id);
        
        KnowledgeBaseDTO kb = knowledgeBaseService.getById(id)
                .orElseThrow(() -> new BusinessException("KB_001", "知识库不存在"));
        
        return ResponseEntity.ok(ApiResponse.success(kb));
    }
}
```

**要点**：
- 使用 `@RequiredArgsConstructor` 进行构造器注入
- 使用 `@Operation` 添加 Swagger 文档
- 记录关键日志
- 统一返回 `ApiResponse` 包装

### 服务规范

```java
public interface KnowledgeBaseService {
    
    /**
     * 根据 ID 获取知识库
     *
     * @param id 知识库 ID
     * @return 知识库 DTO，如果不存在返回 Optional.empty()
     */
    Optional<KnowledgeBaseDTO> getById(Long id);
    
    /**
     * 创建知识库
     *
     * @param request 创建请求
     * @param ownerId 所有者 ID
     * @return 创建后的知识库 DTO
     */
    KnowledgeBaseDTO create(CreateKnowledgeBaseRequest request, Long ownerId);
}
```

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final VectorStore vectorStore;

    @Override
    public Optional<KnowledgeBaseDTO> getById(Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        return Optional.ofNullable(kb).map(this::toDTO);
    }

    @Override
    @Transactional
    public KnowledgeBaseDTO create(CreateKnowledgeBaseRequest request, Long ownerId) {
        // 1. 验证名称唯一性
        if (existsByName(request.getName())) {
            throw new BusinessException("KB_002", "知识库名称已存在");
        }
        
        // 2. 创建实体
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setOwnerId(ownerId);
        
        // 3. 保存到数据库
        knowledgeBaseMapper.insert(kb);
        
        // 4. 创建向量集合
        String collectionName = "kb_" + kb.getId();
        vectorStore.createCollection(collectionName, 1536);
        
        // 5. 更新向量集合名称
        kb.setVectorCollection(collectionName);
        knowledgeBaseMapper.updateById(kb);
        
        log.info("知识库创建成功: id={}, name={}", kb.getId(), kb.getName());
        
        return toDTO(kb);
    }
    
    private KnowledgeBaseDTO toDTO(KnowledgeBase entity) {
        // 转换逻辑
    }
}
```

**要点**：
- 类级别添加 `@Transactional(readOnly = true)`
- 写操作方法添加 `@Transactional`
- 使用 `Optional` 处理可能为空的返回值
- 复杂操作分步骤，添加注释说明

### DTO 规范

使用 Java Record（Java 17+）：

```java
// 请求 DTO
public record CreateKnowledgeBaseRequest(
    @NotBlank(message = "名称不能为空")
    @Size(min = 2, max = 100, message = "名称长度需在2-100字符之间")
    String name,
    
    @Size(max = 500, message = "描述不能超过500字符")
    String description,
    
    Boolean isPublic
) {
    // 提供默认值
    public CreateKnowledgeBaseRequest {
        if (isPublic == null) {
            isPublic = false;
        }
    }
}

// 响应 DTO
public record KnowledgeBaseDTO(
    Long id,
    String name,
    String description,
    Long ownerId,
    String vectorCollection,
    Integer documentCount,
    Boolean isPublic,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

### 异常处理

```java
// 自定义业务异常
public class BusinessException extends RuntimeException {
    private final String code;
    private final String message;
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}

// 全局异常处理
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", message));
    }
}
```

---

## 📋 日志规范

### 日志级别

| 级别 | 使用场景 |
|------|---------|
| ERROR | 系统错误、异常、需要立即关注的问题 |
| WARN | 潜在问题、可恢复的错误 |
| INFO | 关键业务流程、重要操作 |
| DEBUG | 详细信息、调试用 |
| TRACE | 更详细的信息 |

### 日志格式

```java
// 方法入口
log.info("创建知识库请求: name={}, ownerId={}", request.getName(), ownerId);

// 方法出口
log.info("知识库创建成功: id={}", kb.getId());

// 异常日志
log.error("创建知识库失败: name={}", request.getName(), e);

// 调试日志
log.debug("检索到 {} 个相关文档", contexts.size());
```

### 敏感信息脱敏

```java
// 不要这样做
log.info("用户登录: username={}, password={}", username, password);

// 正确做法
log.info("用户登录: username={}", username);

// 对敏感信息脱敏
log.info("用户信息: email={}", maskEmail(email));
```

---

## ✅ 测试规范

### 测试分类

```
tests/
├── unit/           # 单元测试（快速，无外部依赖）
├── integration/    # 集成测试（需要数据库、Redis等）
└── property/       # 属性测试（jqwik）
```

### 单元测试

```java
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {
    
    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    
    @Mock
    private VectorStore vectorStore;
    
    @InjectMocks
    private KnowledgeBaseServiceImpl knowledgeBaseService;
    
    @Test
    @DisplayName("创建知识库 - 成功场景")
    void create_ShouldSucceed_WhenValidRequest() {
        // Given
        var request = new CreateKnowledgeBaseRequest("测试知识库", "描述", false);
        Long ownerId = 1L;
        
        when(knowledgeBaseMapper.existsByName(anyString())).thenReturn(false);
        when(knowledgeBaseMapper.insert(any())).thenReturn(1);
        
        // When
        var result = knowledgeBaseService.create(request, ownerId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("测试知识库");
        verify(vectorStore).createCollection(anyString(), eq(1536));
    }
    
    @Test
    @DisplayName("创建知识库 - 名称重复应抛出异常")
    void create_ShouldThrowException_WhenNameExists() {
        // Given
        var request = new CreateKnowledgeBaseRequest("已存在的名称", "描述", false);
        when(knowledgeBaseMapper.existsByName("已存在的名称")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> knowledgeBaseService.create(request, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("名称已存在");
    }
}
```

### 属性测试 (jqwik)

```java
class JwtTokenProviderPropertyTest {
    
    @Property
    void generatedTokenShouldBeValid(
            @ForAll @StringLength(min = 1, max = 50) String username,
            @ForAll @LongRange(min = 1) Long userId) {
        
        JwtTokenProvider provider = createProvider();
        UserPrincipal user = new UserPrincipal(userId, username, List.of("USER"));
        
        String token = provider.generateAccessToken(user);
        
        assertThat(provider.isTokenValid(token)).isTrue();
        assertThat(provider.getUsername(token)).isEqualTo(username);
    }
}
```

### 测试命名

```java
// 格式：method_ShouldExpectedBehavior_WhenCondition
void create_ShouldSucceed_WhenValidRequest()
void create_ShouldThrowException_WhenNameExists()
void ask_ShouldReturnCachedResult_WhenCacheHit()
```

---

## 🔄 Git 规范

### 分支命名

| 分支类型 | 命名格式 | 示例 |
|---------|---------|------|
| 功能分支 | `feature/{描述}` | `feature/add-feedback-api` |
| 修复分支 | `fix/{描述}` | `fix/jwt-token-validation` |
| 热修复 | `hotfix/{描述}` | `hotfix/security-patch` |
| 发布分支 | `release/{版本}` | `release/1.0.0` |

### 提交信息

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type**：
- `feat`: 新功能
- `fix`: 修复 Bug
- `docs`: 文档更新
- `style`: 代码格式（不影响逻辑）
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**：
```
feat(qa): 添加流式问答接口

- 实现 SSE 流式响应
- 添加 askStream 方法
- 更新 API 文档

Closes #123
```

---

## 📚 注释规范

### 类注释

```java
/**
 * 知识库服务实现类
 * <p>
 * 提供知识库的 CRUD 操作，包括：
 * <ul>
 *   <li>创建知识库并初始化向量集合</li>
 *   <li>删除知识库及关联资源</li>
 *   <li>知识库统计信息查询</li>
 * </ul>
 *
 * @author 开发者
 * @since 1.0.0
 * @see KnowledgeBaseService
 */
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {
}
```

### 方法注释

```java
/**
 * 创建知识库
 * <p>
 * 创建新的知识库并初始化对应的向量集合。
 *
 * @param request 创建请求，包含名称、描述等信息
 * @param ownerId 所有者用户 ID
 * @return 创建后的知识库 DTO
 * @throws BusinessException 当知识库名称已存在时抛出 KB_002 错误
 */
@Override
@Transactional
public KnowledgeBaseDTO create(CreateKnowledgeBaseRequest request, Long ownerId) {
    // ...
}
```

### 代码注释

```java
// 1. 验证名称唯一性
if (existsByName(request.getName())) {
    throw new BusinessException("KB_002", "知识库名称已存在");
}

// TODO: 后续需要添加名称敏感词过滤
// FIXME: 在高并发场景下可能存在竞态条件
```

---

## 🛠️ 工具配置

### IDE 配置 (IntelliJ IDEA)

1. **代码风格**
   - File → Settings → Editor → Code Style → Java
   - 导入项目的 `.editorconfig` 文件

2. **保存时自动格式化**
   - Settings → Tools → Actions on Save
   - 勾选 "Reformat code" 和 "Optimize imports"

3. **Lombok 插件**
   - 确保安装 Lombok 插件
   - Settings → Build → Compiler → Annotation Processors → Enable

### Maven 配置

```xml
<plugins>
    <!-- 代码格式检查 -->
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>3.3.0</version>
    </plugin>
    
    <!-- 测试覆盖率 -->
    <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.11</version>
    </plugin>
</plugins>
```

---

## ✨ 最佳实践

### Do's ✅

- 使用 `Optional` 避免 `null` 返回值
- 使用 `@RequiredArgsConstructor` 进行依赖注入
- 使用 Record 定义不可变的 DTO
- 编写有意义的单元测试
- 及时更新文档和注释
- 使用日志记录关键操作

### Don'ts ❌

- 不要在控制器中编写业务逻辑
- 不要硬编码配置值（使用 `@Value` 或配置类）
- 不要捕获异常后忽略（至少记录日志）
- 不要在日志中打印敏感信息
- 不要提交未经测试的代码
- 不要使用 `System.out.println`（使用日志框架）

