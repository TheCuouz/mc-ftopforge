package com.cristian.ftopforge.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DialectTest {
    @Test
    void sqliteUsesAutoincrement() {
        String ddl = Dialect.SQLITE.recalcsDdl();
        assertTrue(ddl.contains("AUTOINCREMENT"));
        assertTrue(ddl.contains("ftf_recalcs"));
    }

    @Test
    void mysqlUsesAutoIncrement() {
        String ddl = Dialect.MYSQL.recalcsDdl();
        assertTrue(ddl.contains("AUTO_INCREMENT"));
        assertFalse(ddl.contains("AUTOINCREMENT"));
    }

    @Test
    void factoryByName() {
        assertSame(Dialect.SQLITE, Dialect.byName("sqlite"));
        assertSame(Dialect.SQLITE, Dialect.byName("SQLite"));
        assertSame(Dialect.MYSQL, Dialect.byName("mysql"));
        assertThrows(IllegalArgumentException.class, () -> Dialect.byName("oracle"));
    }

    @Test
    void sqlite_skullCacheDdl_isCreateTableIfNotExists() {
        String ddl = Dialect.SQLITE.skullCacheDdl();
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS ftf_skull_cache"));
        assertTrue(ddl.contains("uuid TEXT PRIMARY KEY"));
        assertTrue(ddl.contains("profile_property_value TEXT NOT NULL"));
        assertTrue(ddl.contains("fetched_at"));
    }

    @Test
    void mysql_skullCacheDdl_usesVarcharAndInnodb() {
        String ddl = Dialect.MYSQL.skullCacheDdl();
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS ftf_skull_cache"));
        assertTrue(ddl.contains("uuid VARCHAR(36) PRIMARY KEY"));
        assertTrue(ddl.contains("ENGINE=InnoDB"));
        assertTrue(ddl.contains("CHARSET=utf8mb4"));
    }

    @Test
    void both_dialects_haveFetchedAtIndex() {
        assertTrue(Dialect.SQLITE.skullCacheIndexDdl().contains("idx_ftf_skull_cache_fetched_at"));
        assertTrue(Dialect.MYSQL.skullCacheIndexDdl().contains("idx_ftf_skull_cache_fetched_at") ||
                   Dialect.MYSQL.skullCacheDdl().contains("INDEX idx_ftf_skull_cache_fetched_at"));
    }
}
