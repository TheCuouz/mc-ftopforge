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
    };

    public abstract String recalcsDdl();
    public abstract String snapshotsCurrentDdl();

    public static Dialect byName(String name) {
        if (name == null) throw new IllegalArgumentException("storage.type is null");
        switch (name.toLowerCase()) {
            case "sqlite": return SQLITE;
            case "mysql": return MYSQL;
            default: throw new IllegalArgumentException("Unknown storage type: " + name);
        }
    }
}
