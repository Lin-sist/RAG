# Spring Boot 核心知识点

## 1. Spring Boot 简介

Spring Boot 是基于 Spring 框架的快速开发脚手架，旨在简化 Spring 应用的初始搭建和开发过程。

### 1.1 核心特性
- **自动配置**：根据 classpath 下的依赖自动配置 Spring 应用
- **起步依赖**：一站式依赖管理，避免版本冲突
- **内嵌服务器**：Tomcat、Jetty、Undertow
- **生产就绪**：Actuator 健康检查、监控指标
- **无代码生成**：不需要 XML 配置

## 2. 核心注解

### 2.1 启动类注解
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

`@SpringBootApplication` 是组合注解，包含：
- `@Configuration`：标记为配置类
- `@EnableAutoConfiguration`：启用自动配置
- `@ComponentScan`：组件扫描

### 2.2 常用注解
- `@RestController`：= @Controller + @ResponseBody
- `@RequestMapping`：映射 HTTP 请求
- `@GetMapping`、`@PostMapping`：简化的请求映射
- `@PathVariable`：路径参数
- `@RequestParam`：查询参数
- `@RequestBody`：JSON 请求体
- `@Autowired`：自动注入依赖

## 3. 配置文件

### 3.1 application.yml 示例
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  redis:
    host: localhost
    port: 6379
    
logging:
  level:
    root: INFO
    com.example: DEBUG
```

### 3.2 配置优先级
1. 命令行参数
2. JNDI 属性
3. Java 系统属性
4. 环境变量
5. application.properties/yml

## 4. 依赖注入

### 4.1 构造器注入（推荐）
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    // Lombok 自动生成构造器，Spring 自动注入
}
```

### 4.2 字段注入（不推荐）
```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    // 难以测试，不够明确
}
```

## 5. 数据访问

### 5.1 JPA 示例
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    private String email;
}

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

### 5.2 MyBatis-Plus 示例
```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 内置 CRUD 方法
}

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    // 继承通用服务方法
}
```

## 6. 异常处理

### 6.1 全局异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception e) {
        log.error("未知错误", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "系统错误"));
    }
}
```

## 7. Spring Security

### 7.1 基本配置
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests()
                .requestMatchers("/auth/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        return http.build();
    }
}
```

### 7.2 JWT 认证
- **accessToken**：短期令牌（1小时）
- **refreshToken**：长期令牌（7天）
- **流程**：
  1. 登录成功，返回两个 Token
  2. 访问接口携带 accessToken
  3. accessToken 过期，用 refreshToken 刷新

## 8. 参数校验

### 8.1 常用注解
```java
public class CreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20")
    private String username;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Min(value = 18, message = "年龄不能小于 18")
    private Integer age;
}

@PostMapping("/users")
public ResponseEntity<ApiResponse> create(@Valid @RequestBody CreateUserRequest request) {
    // @Valid 触发校验
}
```

## 9. 事务管理

### 9.1 声明式事务
```java
@Service
public class OrderService {
    
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        orderRepository.save(order);
        // 如果发生异常，自动回滚
        stockService.deduct(order.getProductId(), order.getQuantity());
    }
}
```

### 9.2 事务传播行为
- `REQUIRED`（默认）：加入当前事务，没有则新建
- `REQUIRES_NEW`：总是新建事务
- `SUPPORTS`：有事务就加入，没有就非事务执行
- `NOT_SUPPORTED`：非事务执行，挂起当前事务
- `NEVER`：非事务执行，存在事务抛异常

## 10. 异步处理

### 10.1 @Async 注解
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

@Service
public class EmailService {
    
    @Async
    public CompletableFuture<String> sendEmail(String to, String subject) {
        // 异步发送邮件
        return CompletableFuture.completedFuture("SUCCESS");
    }
}
```

## 11. 定时任务

### 11.1 @Scheduled 注解
```java
@Component
public class ScheduledTasks {
    
    // 每 5 秒执行一次
    @Scheduled(fixedRate = 5000)
    public void reportCurrentTime() {
        log.info("当前时间：{}", LocalDateTime.now());
    }
    
    // 每天凌晨 2 点执行
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredData() {
        log.info("清理过期数据");
    }
}
```

## 12. 缓存

### 12.1 Spring Cache
```java
@Configuration
@EnableCaching
public class CacheConfig {
    // 配置 Redis 缓存
}

@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User getById(Long id) {
        // 第一次查询数据库，之后从缓存读取
    }
    
    @CacheEvict(value = "users", key = "#id")
    public void deleteById(Long id) {
        // 删除缓存
    }
    
    @CachePut(value = "users", key = "#user.id")
    public User update(User user) {
        // 更新缓存
    }
}
```

## 13. 单元测试

### 13.1 测试示例
```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void testGetUser() throws Exception {
        User user = new User(1L, "admin");
        when(userService.getById(1L)).thenReturn(user);
        
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }
}
```

---

**学习建议**：
1. 多看官方文档：https://spring.io/projects/spring-boot
2. 多写代码，实践是最好的老师
3. 阅读优秀开源项目源码
4. 关注最新技术动态

**祝你学习顺利！🚀**
