package com.enterprise.rag.admin.session;

import com.enterprise.rag.admin.session.mapper.SessionContextMapper;

import com.enterprise.rag.admin.controller.SessionContextController;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.exception.GlobalExceptionHandler;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SessionContextTest {
    private JdbcDataSource dataSource;
    private SqlSessionFactory factory;

    @BeforeEach
    void setup() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:context_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement()) {
            sql.execute("CREATE TABLE tenant(id BIGINT PRIMARY KEY,name VARCHAR(100),enabled INT,deleted INT)");
            sql.execute("CREATE TABLE `user`(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(50),"
                    + "email VARCHAR(100),enabled INT,deleted INT,display_name VARCHAR(50),avatar_data BLOB,version INT DEFAULT 0)");
            sql.execute("INSERT INTO tenant VALUES(901,'组织A · 合成',1,0),(902,'组织B · 合成',1,0),"
                    + "(9007199254740993,'长ID组织 · 合成',1,0)");
            sql.execute("INSERT INTO `user`(id,tenant_id,username,email,enabled,deleted) VALUES(41,901,'same-name','a@example.test',1,0),"
                    + "(42,902,'same-name','b@example.test',1,0),(43,9007199254740993,'large-id',NULL,1,0)");
        }
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration(
                new Environment("context", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(SessionContextMapper.class);
        factory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private UserPrincipal principal(long user, long tenant) {
        return UserPrincipal.builder().id(user).tenantId(tenant).username("untrusted-display")
                .enabled(true).roles(Set.of("USER")).build();
    }
    private SessionContextService service(SqlSession session) {
        return new SessionContextService(new CurrentUserService(), session.getMapper(SessionContextMapper.class));
    }
    private void update(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Test
    void readsServerScopedDatabaseFactsAndPreservesStringIdPrecision() {
        try (SqlSession session = factory.openSession()) {
            var service = service(session);
            var a = service.current(principal(41, 901));
            var b = service.current(principal(42, 902));
            assertEquals("a@example.test", a.userInfo().email());
            assertEquals("b@example.test", b.userInfo().email());
            assertEquals("组织A · 合成", a.workspace().displayName());
            assertEquals("same-name", a.userInfo().username());
            assertEquals("9007199254740993", service.current(principal(43, 9007199254740993L)).workspace().id());
        }
    }

    @Test
    void mismatchedUnknownAndMissingPrincipalShareAuthenticationFailure() {
        try (SqlSession session = factory.openSession()) {
            for (UserPrincipal principal : new UserPrincipal[]{principal(41, 902), principal(42, 901),
                    principal(999, 901), principal(41, 0), null}) {
                BusinessException failure = assertThrows(BusinessException.class, () -> service(session).current(principal));
                assertEquals("AUTH_001", failure.getErrorCode());
                assertEquals(HttpStatus.UNAUTHORIZED, failure.getHttpStatus());
            }
        }
    }

    @Test
    void disabledAndDeletedAccountsAndOrganizationsCannotReadContext() throws Exception {
        for (String table : new String[]{"`user`", "tenant"}) {
            for (String column : new String[]{"enabled", "deleted"}) {
                long id = table.equals("tenant") ? 901 : 41;
                update("UPDATE " + table + " SET " + column + "=" + (column.equals("enabled") ? 0 : 1) + " WHERE id=" + id);
                try (SqlSession session = factory.openSession()) {
                    assertEquals("AUTH_001", assertThrows(BusinessException.class,
                            () -> service(session).current(principal(41, 901))).getErrorCode());
                }
                update("UPDATE " + table + " SET " + column + "=" + (column.equals("enabled") ? 1 : 0) + " WHERE id=" + id);
            }
        }
    }

    @Test
    void missingOrganizationNameIsUnavailableRatherThanInvented() throws Exception {
        update("UPDATE tenant SET name='  ' WHERE id=901");
        try (SqlSession session = factory.openSession()) {
            BusinessException error = assertThrows(BusinessException.class, () -> service(session).current(principal(41, 901)));
            assertEquals("SESSION_CONTEXT_UNAVAILABLE", error.getErrorCode());
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getHttpStatus());
        }
    }

    @Test
    void httpContractIgnoresClientSelectorReturnsNoStoreAndRejectsMissingIdentity() throws Exception {
        try (SqlSession session = factory.openSession()) {
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new SessionContextController(service(session)))
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .setControllerAdvice(new GlobalExceptionHandler()).build();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal(41, 901), null, Set.of()));
            mvc.perform(get("/api/session/context").param("tenantId", "902").header("X-Tenant-ID", "902"))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.data.workspace.id").value("901"))
                    .andExpect(jsonPath("$.data.workspace.displayName").value("组织A · 合成"))
                    .andExpect(jsonPath("$.data.userInfo.email").value("a@example.test"))
                    .andExpect(jsonPath("$.data.userInfo.password").doesNotExist())
                    .andExpect(jsonPath("$.data.workspace.code").doesNotExist());
            SecurityContextHolder.clearContext();
            mvc.perform(get("/api/session/context")).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
        } finally { SecurityContextHolder.clearContext(); }
    }
}
