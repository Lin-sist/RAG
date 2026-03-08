# Spring Framework 完整技术手册


## 第 1 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 1.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 1.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 2 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 2.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 2.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 3 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 3.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 3.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 4 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 4.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 4.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 5 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 5.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 5.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 6 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 6.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 6.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 7 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 7.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 7.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 8 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 8.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 8.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 9 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 9.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 9.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 10 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 10.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 10.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 11 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 11.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 11.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 12 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 12.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 12.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 13 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 13.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 13.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 14 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 14.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 14.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 15 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 15.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 15.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 16 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 16.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 16.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 17 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 17.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 17.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 18 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 18.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 18.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 19 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 19.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 19.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。

## 第 20 章：Spring Boot 核心概念深入分析

Spring Boot 提供了强大的自动配置机制，通过 `@EnableAutoConfiguration` 注解，框架会根据classpath中的依赖自动配置Bean。例如，当classpath中存在 `spring-boot-starter-web` 时，Spring Boot 会自动配置 `DispatcherServlet`、`ErrorMvcAutoConfiguration` 等组件。

自动配置的核心是 `spring.factories` 文件（Spring Boot 2.x）或 `AutoConfiguration.imports` 文件（Spring Boot 3.x），这些文件列出了所有候选的自动配置类。每个自动配置类通常使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解来决定是否生效。

### 20.1 依赖注入原理

`@Autowired` 注解的工作流程：首先，Spring 容器在启动时会扫描所有标注了 `@Component`、`@Service`、`@Repository`、`@Controller` 等注解的类，并将它们注册为 Bean 定义。然后，当容器创建 Bean 实例时，会检查其构造函数、setter 方法或字段上是否有 `@Autowired` 注解，如果有，则从容器中查找匹配类型的 Bean 进行注入。

### 20.2 AOP 面向切面编程

Spring AOP 使用代理模式实现，支持 JDK 动态代理和 CGLIB 代理两种方式。当目标类实现了接口时，默认使用 JDK 动态代理；否则使用 CGLIB 生成目标类的子类作为代理。`@Aspect` 注解定义切面，`@Pointcut` 定义切点表达式，`@Before`、`@After`、`@Around` 定义通知类型。
