package com.enterprise.rag.admin.profile;

import com.enterprise.rag.admin.controller.ProfileController;
import com.enterprise.rag.admin.profile.mapper.ProfileMapper;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.MultipartConfigElement;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.*;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual HTTP bytes -> Tomcat multipart parser -> controller -> transactional mapper -> isolated H2. */
class ProfileHttpTest {
    static AnnotationConfigServletWebServerApplicationContext app;
    static int port;
    static final ObjectMapper JSON = new ObjectMapper();
    @BeforeAll static void start() {
        app = new AnnotationConfigServletWebServerApplicationContext(App.class);
        port = app.getWebServer().getPort();
    }
    @AfterAll static void stop() { if (app != null) app.close(); }
    @BeforeEach void reset() throws Exception {
        try (var c = app.getBean(JdbcDataSource.class).getConnection(); var s = c.createStatement()) {
            s.execute("UPDATE `user` SET display_name=NULL,email=NULL,avatar_data=NULL,version=0");
        }
    }
    HttpResponse<byte[]> send(String method, String path, byte[] body, String type) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10))
                .header("X-Synthetic-Identity", "profile-http-only");
        if (type != null) request.header("Content-Type", type);
        return HttpClient.newHttpClient().send(request.method(method, HttpRequest.BodyPublishers.ofByteArray(body)).build(), HttpResponse.BodyHandlers.ofByteArray());
    }
    byte[] multipart(String metadata, byte[] avatar) throws Exception {
        var out = new ByteArrayOutputStream();
        out.write(("--profile-test\r\nContent-Disposition: form-data; name=\"profile\"; filename=\"blob\"\r\nContent-Type: application/json\r\n\r\n" + metadata + "\r\n").getBytes(StandardCharsets.UTF_8));
        if (avatar != null) {
            out.write("--profile-test\r\nContent-Disposition: form-data; name=\"avatar\"; filename=\"avatar.png\"\r\nContent-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(avatar); out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.write("--profile-test--\r\n".getBytes(StandardCharsets.UTF_8)); return out.toByteArray();
    }
    @Test void realMultipartPutImmediatelyPersistsNameAndAvatar() throws Exception {
        String input = "{\"displayName\":\"Lin synthetic\",\"email\":null,\"expectedRevision\":0,\"avatarAction\":\"REPLACE\"}";
        var response = send("PUT", "/api/account/profile", multipart(input, ProfileContractTest.png()), "multipart/form-data; boundary=profile-test");
        assertEquals(200, response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
        var saved = JSON.readTree(response.body()).path("data");
        assertEquals("Lin synthetic", saved.path("displayName").asText()); assertTrue(saved.path("hasAvatar").asBoolean());
        var current = JSON.readTree(send("GET", "/api/account/profile", new byte[0], null).body()).path("data");
        assertEquals(saved, current);
        var avatar = send("GET", "/api/account/profile/avatar", new byte[0], null);
        assertEquals(200, avatar.statusCode()); assertEquals("image/png", avatar.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(256, javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(avatar.body())).getWidth());
    }
    @Test void realMultipartPutWithoutAvatarImmediatelyPersistsName() throws Exception {
        String input = "{\"displayName\":\"Lin synthetic\",\"email\":null,\"expectedRevision\":0,\"avatarAction\":\"KEEP\"}";
        var response = send("PUT", "/api/account/profile", multipart(input, null), "multipart/form-data; boundary=profile-test");
        assertEquals(200, response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
        assertEquals("Lin synthetic", JSON.readTree(send("GET", "/api/account/profile", new byte[0], null).body()).path("data").path("displayName").asText());
    }
    @Test void productionMapperConfigurationAcceptsAvatarBytes() throws Exception {
        var identity = UserPrincipal.builder().id(41L).tenantId(901L).username("synthetic-account").enabled(true).roles(Set.of("USER")).build();
        byte[] image = ProfileContractTest.png();
        assertDoesNotThrow(() -> app.getBean(ProfileService.class).save(identity, new ProfileInput("Synthetic nickname",null,0,"REPLACE"),image));
    }
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableTransactionManagement
    @Import({ProfileController.class, ProfileService.class, CurrentUserService.class, AvatarNormalizer.class, GlobalExceptionHandler.class})
    static class App {
        @Bean TomcatServletWebServerFactory webServerFactory() throws Exception {
            var server = new TomcatServletWebServerFactory(0); server.setAddress(InetAddress.getLoopbackAddress()); return server;
        }
        @Bean DispatcherServlet dispatcherServlet() { return new DispatcherServlet(); }
        @Bean ServletRegistrationBean<DispatcherServlet> dispatcherRegistration(DispatcherServlet servlet) {
            var registration = new ServletRegistrationBean<>(servlet, "/");
            registration.setMultipartConfig(new MultipartConfigElement(System.getProperty("java.io.tmpdir"), 3*1024*1024, 4*1024*1024, 0)); return registration;
        }
        @Bean StandardServletMultipartResolver multipartResolver() { return new StandardServletMultipartResolver(); }
        @Bean ObjectMapper objectMapper() { return JSON; }
        @Bean WebMvcConfigurer principalResolver() { return new WebMvcConfigurer() {
            @Override public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) { resolvers.add(new AuthenticationPrincipalArgumentResolver()); }
        }; }
        @Bean JdbcDataSource dataSource() throws Exception {
            var db = new JdbcDataSource(); db.setURL("jdbc:h2:mem:http_profile_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
            try (var c=db.getConnection();var s=c.createStatement()) {
                s.execute("CREATE TABLE tenant(id BIGINT PRIMARY KEY,enabled INT,deleted INT)");
                s.execute("CREATE TABLE `user`(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(50),email VARCHAR(100) UNIQUE,display_name VARCHAR(50),avatar_data BLOB,version INT DEFAULT 0,enabled INT,deleted INT,updated_at TIMESTAMP)");
                s.execute("INSERT INTO tenant VALUES(901,1,0)"); s.execute("INSERT INTO `user`(id,tenant_id,username,enabled,deleted) VALUES(41,901,'synthetic-account',1,0)");
            } return db;
        }
        @Bean ProfileMapper profileMapper(JdbcDataSource db) {
            var config = new com.baomidou.mybatisplus.core.MybatisConfiguration(new Environment("http-profile",new SpringManagedTransactionFactory(),db));
            config.setMapUnderscoreToCamelCase(true); config.addMapper(ProfileMapper.class);
            config.addInterceptor(new com.enterprise.rag.common.config.MyBatisPlusConfig().mybatisPlusInterceptor());
            return new SqlSessionTemplate(new com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder().build(config)).getMapper(ProfileMapper.class);
        }
        @Bean DataSourceTransactionManager transactionManager(JdbcDataSource db) { return new DataSourceTransactionManager(db); }
        @Bean FilterRegistrationBean<OncePerRequestFilter> syntheticIdentity() {
            return new FilterRegistrationBean<>(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest req,jakarta.servlet.http.HttpServletResponse res,jakarta.servlet.FilterChain chain) throws java.io.IOException,jakarta.servlet.ServletException {
                    try {
                        if ("profile-http-only".equals(req.getHeader("X-Synthetic-Identity"))) SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(UserPrincipal.builder().id(41L).tenantId(901L).username("synthetic-account").enabled(true).roles(Set.of("USER")).build(),null,List.of()));
                        chain.doFilter(req,res);
                    } finally { SecurityContextHolder.clearContext(); }
                }
            });
        }
    }
}
