package com.cristian.ftopforge.storage;

public enum Dialect {
    SQLITE {
        @Override public String recalcsDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_recalcs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "started_at BIGINT NOT NULL, " +
                    "finished_at BIGINT, " +
                    "duration_ms INTEGER, " +
                    "faction_count INTEGER)";
        }
        @Override public String snapshotsCurrentDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_snapshots_current (" +
                    "faction_id TEXT PRIMARY KEY, " +
                    "faction_name TEXT NOT NULL, " +
                    "leader_uuid TEXT, leader_name TEXT, " +
                    "total_value BIGINT NOT NULL, " +
                    "chunks_value BIGINT, spawners_value BIGINT, items_value BIGINT, " +
                    "blocks_value BIGINT, balance_value BIGINT, " +
                    "chunks_count INTEGER, " +
                    "spawners_count_json TEXT, blocks_count_json TEXT, " +
                    "richest_member_uuid TEXT, richest_member_balance BIGINT, " +
                    "calculated_at BIGINT NOT NULL, " +
                    "recalc_id INTEGER)";
        }
        @Override public String skullCacheDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_skull_cache (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "profile_property_value TEXT NOT NULL, " +
                    "fetched_at BIGINT NOT NULL)";
        }
        @Override public String skullCacheIndexDdl() {
            return "CREATE INDEX IF NOT EXISTS idx_ftf_skull_cache_fetched_at " +
                    "ON ftf_skull_cache(fetched_at)";
        }
    },
    MYSQL {
        @Override public String recalcsDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_recalcs (" +
                    "id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY, " +
                    "started_at BIGINT NOT NULL, " +
                    "finished_at BIGINT, " +
                    "duration_ms INT, " +
                    "faction_count INT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        @Override public String snapshotsCurrentDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_snapshots_current (" +
                    "faction_id VARCHAR(64) PRIMARY KEY, " +
                    "faction_name VARCHAR(64) NOT NULL, " +
                    "leader_uuid VARCHAR(36), leader_name VARCHAR(64), " +
                    "total_value BIGINT NOT NULL, " +
                    "chunks_value BIGINT, spawners_value BIGINT, items_value BIGINT, " +
                    "blocks_value BIGINT, balance_value BIGINT, " +
                    "chunks_count INT, " +
                    "spawners_count_json TEXT, blocks_count_json TEXT, " +
                    "richest_member_uuid VARCHAR(36), richest_member_balance BIGINT, " +
                    "calculated_at BIGINT NOT NULL, " +
                    "recalc_id INT UNSIGNED) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        @Override public String skullCacheDdl() {
            return "CREATE TABLE IF NOT EXISTS ftf_skull_cache (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "profile_property_value TEXT NOT NULL, " +
                    "fetched_at BIGINT NOT NULL, " +
                    "INDEX idx_ftf_skull_cache_fetched_at (fetched_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        @Override public String skullCacheIndexDdl() {
            return ""; // MySQL has INDEX inline in the CREATE TABLE above
        }
    };

    public abstract String recalcsDdl();
    public abstract String snapshotsCurrentDdl();
    public abstract String skullCacheDdl();
    public abstract String skullCacheIndexDdl();

    public static Dialect byName(String name) {
        if (name == null) throw new IllegalArgumentException("storage.type is null");
        switch (name.toLowerCase()) {
            case "sqlite": return SQLITE;
            case "mysql": return MYSQL;
            default: throw new IllegalArgumentException("Unknown storage type: " + name);
        }
    }
}
