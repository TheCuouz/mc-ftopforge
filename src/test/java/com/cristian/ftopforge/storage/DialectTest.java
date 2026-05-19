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
}
