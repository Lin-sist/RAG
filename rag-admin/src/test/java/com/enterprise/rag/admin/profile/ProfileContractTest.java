package com.enterprise.rag.admin.profile;

import com.enterprise.rag.admin.profile.mapper.ProfileMapper;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.admin.controller.ProfileController;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockPart;
import org.springframework.transaction.support.TransactionTemplate;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfileContractTest {
    JdbcDataSource db;
    ProfileService service;
    TransactionTemplate tx;
    MockMvc mvc;
    final ObjectMapper json = new ObjectMapper();
    final UserPrincipal principal = identity(41, 901);
    static UserPrincipal identity(long id, long tenant) {
        return UserPrincipal.builder().id(id).tenantId(tenant).username("untrusted").enabled(true).roles(Set.of("USER")).build();
    }
    @BeforeEach void setup() throws Exception {
        db = new JdbcDataSource(); db.setURL("jdbc:h2:mem:profile_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        sql("CREATE TABLE tenant(id BIGINT PRIMARY KEY,enabled INT,deleted INT)");
        sql("CREATE TABLE `user`(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(50),email VARCHAR(100) UNIQUE,display_name VARCHAR(50),avatar_data BLOB,version INT DEFAULT 0,enabled INT,deleted INT,updated_at TIMESTAMP)");
        sql("INSERT INTO tenant VALUES(901,1,0),(902,1,0)");
        sql("INSERT INTO `user`(id,tenant_id,username,email,enabled,deleted) VALUES(41,901,'account-a','a@example.test',1,0),(42,902,'account-b','b@example.test',1,0)");
        var configuration = new org.apache.ibatis.session.Configuration(new Environment("profile", new JdbcTransactionFactory(), db));
        configuration.setMapUnderscoreToCamelCase(true); configuration.addMapper(ProfileMapper.class);
        var factory = new SqlSessionFactoryBuilder().build(configuration);
        // Spring-managed transactions supply the same connection to the mapper.
        configuration.setEnvironment(new Environment("profile", new org.mybatis.spring.transaction.SpringManagedTransactionFactory(), db));
        service = new ProfileService(new CurrentUserService(), new SqlSessionTemplate(factory).getMapper(ProfileMapper.class), new AvatarNormalizer());
        tx = new TransactionTemplate(new DataSourceTransactionManager(db));
        mvc = MockMvcBuilders.standaloneSetup(new ProfileController(service, json)).setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    void sql(String statement) throws Exception { try (var c = db.getConnection(); var s = c.createStatement()) { s.execute(statement); } }
    ProfileDTO save(ProfileInput input, byte[] avatar) { return tx.execute(status -> service.save(principal, input, avatar)); }
    static byte[] png() throws Exception {
        var image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics(); g.setColor(java.awt.Color.ORANGE); g.fillRect(0, 0, 400, 200); g.dispose();
        var out = new ByteArrayOutputStream(); ImageIO.write(image, "png", out); return out.toByteArray();
    }
    @Test void strictInputRejectsUnknownMissingDuplicateAndInvalidFields() {
        String valid = "{\"displayName\":null,\"email\":null,\"expectedRevision\":0,\"avatarAction\":\"KEEP\"}";
        assertNull(ProfileInput.parse(json, valid).displayName());
        for (String input : List.of("{}", valid + "{}", valid.replace("null,\"email\"", "42,\"email\""), valid.replace("\"KEEP\"", "\"URL\""),
                valid.replace("0,", "0.5,"), valid.replace("0,", "-1,"), valid.replace("0,", "2147483648,"),
                valid.replace("{", "{\"userId\":42,"), valid.replace("{", "{\"email\":null,"),
                valid.replace("\"email\":null", "\"email\":\"invalid\""), valid.replace("\"displayName\":null", "\"displayName\":\"" + "😀".repeat(51) + "\""))) {
            assertEquals("PROFILE_VALIDATION_FAILED", assertThrows(BusinessException.class, () -> ProfileInput.parse(json, input)).getErrorCode());
        }
        assertEquals(50, ProfileInput.parse(json, valid.replace("\"displayName\":null", "\"displayName\":\"" + "😀".repeat(50) + "\"")).displayName().codePointCount(0, 100));
    }
    @Test void atomicSaveCropReadRemoveAndVersionConflict() throws Exception {
        var saved = save(new ProfileInput("昵称", "New@Example.test", 0, "REPLACE"), png());
        assertEquals(1, saved.revision()); assertTrue(saved.hasAvatar()); assertEquals("account-a", saved.username());
        var decoded = ImageIO.read(new java.io.ByteArrayInputStream(service.avatar(principal)));
        assertEquals(256, decoded.getWidth()); assertEquals(256, decoded.getHeight());
        assertEquals("PROFILE_REVISION_CONFLICT", assertThrows(BusinessException.class, () -> save(new ProfileInput("stale", null, 0, "REMOVE"), null)).getErrorCode());
        assertEquals(saved, service.current(principal));
        var kept = save(new ProfileInput(null, null, 1, "KEEP"), null); assertTrue(kept.hasAvatar());
        assertFalse(save(new ProfileInput(null, null, 2, "REMOVE"), null).hasAvatar());
        assertEquals("PROFILE_AVATAR_NOT_FOUND", assertThrows(BusinessException.class, () -> service.avatar(principal)).getErrorCode());
    }
    @Test void duplicateEmailRollsBackWholeWriteWithoutChangingOtherAccount() throws Exception {
        save(new ProfileInput("before", "a@example.test", 0, "REPLACE"), png());
        var before = service.current(principal); byte[] image = service.avatar(principal);
        assertEquals("PROFILE_EMAIL_UNAVAILABLE", assertThrows(BusinessException.class,
                () -> save(new ProfileInput("after", "b@example.test", 1, "REMOVE"), null)).getErrorCode());
        assertEquals(before, service.current(principal)); assertArrayEquals(image, service.avatar(principal));
        assertEquals(0, service.current(identity(42,902)).revision());
    }
    @Test void invalidAvatarAndActionLeaveAllFieldsUnchanged() throws Exception {
        var before = service.current(principal);
        for (var action : List.of("KEEP", "REMOVE")) assertThrows(BusinessException.class, () -> save(new ProfileInput("bad", null, 0, action), png()));
        assertThrows(BusinessException.class, () -> save(new ProfileInput("bad", null, 0, "REPLACE"), null));
        assertThrows(BusinessException.class, () -> save(new ProfileInput("bad", null, 0, "REPLACE"), "<svg/>".getBytes(StandardCharsets.UTF_8)));
        assertEquals(before, service.current(principal));
    }
    @Test void identityChecksGuardBothMetadataAndAvatarAndWrites() throws Exception {
        for (var bad : new UserPrincipal[]{null, identity(41,902), identity(42,901), identity(999,901)}) {
            assertEquals("AUTH_001", assertThrows(BusinessException.class, () -> service.current(bad)).getErrorCode());
            assertEquals("AUTH_001", assertThrows(BusinessException.class, () -> service.avatar(bad)).getErrorCode());
            assertEquals("AUTH_001", assertThrows(BusinessException.class, () -> service.save(bad,new ProfileInput(null,null,0,"KEEP"),null)).getErrorCode());
        }
        for (String table : List.of("tenant", "`user`")) for (String flag : List.of("enabled", "deleted")) {
            long id = table.equals("tenant") ? 901 : 41;
            sql("UPDATE " + table + " SET " + flag + "=" + (flag.equals("enabled")?0:1) + " WHERE id="+id);
            assertThrows(BusinessException.class, () -> service.current(principal));
            assertThrows(BusinessException.class, () -> service.avatar(principal));
            assertThrows(BusinessException.class, () -> save(new ProfileInput(null,null,0,"KEEP"),null));
            sql("UPDATE " + table + " SET " + flag + "=" + (flag.equals("enabled")?1:0) + " WHERE id="+id);
        }
    }
    MockPart part(String name, String value) {
        var part = new MockPart(name,value.getBytes(StandardCharsets.UTF_8)); part.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON); return part;
    }
    @Test void httpWhitelistNoStoreAndAuthenticatedBinary() throws Exception {
        mvc.perform(get("/api/account/profile").param("userId","42")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.id").value(41)).andExpect(jsonPath("$.data.hasAvatar").value(false));
        var profile = part("profile","{\"displayName\":\"new\",\"email\":null,\"expectedRevision\":0,\"avatarAction\":\"KEEP\"}");
        mvc.perform(MockMvcRequestBuilders.multipart(org.springframework.http.HttpMethod.PUT,"/api/account/profile").part(profile)).andExpect(status().isOk()).andExpect(jsonPath("$.data.revision").value(1));
        mvc.perform(MockMvcRequestBuilders.multipart(org.springframework.http.HttpMethod.PUT,"/api/account/profile").part(profile,profile)).andExpect(status().isBadRequest());
        mvc.perform(MockMvcRequestBuilders.multipart(org.springframework.http.HttpMethod.PUT,"/api/account/profile").part(profile,part("tenantId","902"))).andExpect(status().isBadRequest());
        mvc.perform(MockMvcRequestBuilders.multipart(org.springframework.http.HttpMethod.PUT,"/api/account/profile").part(profile,new MockPart("avatar","one.png",png()),new MockPart("avatar","two.png",png()))).andExpect(status().isBadRequest());
        save(new ProfileInput(null,null,1,"REPLACE"),png());
        mvc.perform(get("/api/account/profile/avatar")).andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Content-Type-Options","nosniff"));
        SecurityContextHolder.clearContext(); mvc.perform(get("/api/account/profile")).andExpect(status().isUnauthorized());
    }
    @Test void signaturesDimensionsAndSizeAreBoundedBeforeDecode() throws Exception {
        var normalizer = new AvatarNormalizer();
        assertEquals("PROFILE_AVATAR_TOO_LARGE",assertThrows(BusinessException.class,()->normalizer.normalize(new byte[AvatarNormalizer.SOURCE_LIMIT+1])).getErrorCode());
        for (byte[] bytes : List.of(new byte[0],"GIF89a".getBytes(),"<svg/>".getBytes())) assertEquals("PROFILE_AVATAR_UNSUPPORTED",assertThrows(BusinessException.class,()->normalizer.normalize(bytes)).getErrorCode());
        byte[] bomb = png(); java.nio.ByteBuffer.wrap(bomb,16,8).putInt(4097).putInt(4096);
        assertEquals("PROFILE_AVATAR_INVALID",assertThrows(BusinessException.class,()->normalizer.normalize(bomb)).getErrorCode());
        assertEquals("PROFILE_AVATAR_INVALID",assertThrows(BusinessException.class,()->normalizer.normalize(Arrays.copyOf(png(),30))).getErrorCode());
    }
}
