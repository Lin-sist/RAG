package com.enterprise.rag.admin.profile;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated disposable MySQL only; never points at development or real account data. */
@Testcontainers(disabledWithoutDocker = true)
class ProfileMigrationMySqlTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("profile_test").withUsername("profile_test").withPassword("profile_test_only");
    @Test void upgradePreservesLegacyAndBlobRollbackAndReadback() throws Exception {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()).target("13").load().migrate();
        try (var c=MYSQL.createConnection(""); var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO `user`(id,tenant_id,username,password_hash,email,enabled) VALUES(9001,1,'profile-synthetic','{test-only}','profile@example.test',1)");
        }
        var flyway=Flyway.configure().dataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()).load();
        flyway.migrate(); flyway.validate();
        try (var c=MYSQL.createConnection(""); var s=c.createStatement()) {
            try(var row=s.executeQuery("SELECT display_name,avatar_data,email FROM `user` WHERE id=9001")) {
                assertTrue(row.next()); assertNull(row.getString(1));assertNull(row.getBytes(2));assertEquals("profile@example.test",row.getString(3));
            }
            byte[] png=ProfileContractTest.png(); c.setAutoCommit(false);
            try(var update=c.prepareStatement("UPDATE `user` SET display_name=?,avatar_data=?,version=version+1 WHERE id=9001 AND version=0")) {
                update.setString(1,"合成昵称");update.setBytes(2,png);assertEquals(1,update.executeUpdate());
            }
            c.rollback();
            try(var row=s.executeQuery("SELECT avatar_data,version FROM `user` WHERE id=9001")) {assertTrue(row.next());assertNull(row.getBytes(1));assertEquals(0,row.getInt(2));}
            try(var update=c.prepareStatement("UPDATE `user` SET avatar_data=? WHERE id=9001")) {update.setBytes(1,png);update.executeUpdate();} c.commit();
            try(var row=s.executeQuery("SELECT avatar_data FROM `user` WHERE id=9001")){assertTrue(row.next());assertArrayEquals(png,row.getBytes(1));}
        }
    }
}
