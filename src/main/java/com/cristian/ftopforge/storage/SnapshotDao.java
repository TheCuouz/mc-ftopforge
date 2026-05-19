package com.cristian.ftopforge.storage;

import com.cristian.ftopforge.core.FactionSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class SnapshotDao {

    private final JdbcRepository repo;

    public SnapshotDao(JdbcRepository repo) {
        this.repo = repo;
    }

    public void upsert(FactionSnapshot s, long recalcId) throws SQLException {
        String upsert = repo.dialect() == Dialect.SQLITE
                ? "INSERT OR REPLACE INTO ftf_snapshots_current" +
                  "(faction_id, faction_name, leader_uuid, leader_name, total_value, " +
                  " chunks_value, spawners_value, items_value, blocks_value, balance_value, " +
                  " chunks_count, spawners_count_json, blocks_count_json, " +
                  " richest_member_uuid, richest_member_balance, calculated_at, recalc_id) " +
                  "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
                : "INSERT INTO ftf_snapshots_current" +
                  "(faction_id, faction_name, leader_uuid, leader_name, total_value, " +
                  " chunks_value, spawners_value, items_value, blocks_value, balance_value, " +
                  " chunks_count, spawners_count_json, blocks_count_json, " +
                  " richest_member_uuid, richest_member_balance, calculated_at, recalc_id) " +
                  "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                  "ON DUPLICATE KEY UPDATE faction_name=VALUES(faction_name), leader_uuid=VALUES(leader_uuid), " +
                  "leader_name=VALUES(leader_name), total_value=VALUES(total_value), " +
                  "chunks_value=VALUES(chunks_value), spawners_value=VALUES(spawners_value), " +
                  "items_value=VALUES(items_value), blocks_value=VALUES(blocks_value), " +
                  "balance_value=VALUES(balance_value), chunks_count=VALUES(chunks_count), " +
                  "spawners_count_json=VALUES(spawners_count_json), blocks_count_json=VALUES(blocks_count_json), " +
                  "richest_member_uuid=VALUES(richest_member_uuid), richest_member_balance=VALUES(richest_member_balance), " +
                  "calculated_at=VALUES(calculated_at), recalc_id=VALUES(recalc_id)";

        try (Connection c = repo.connection();
             PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setString(1, s.factionId());
            ps.setString(2, s.factionName());
            ps.setString(3, s.leaderUuid());
            ps.setString(4, s.leaderName());
            ps.setLong(5, s.totalValue());
            ps.setLong(6, s.chunksValue());
            ps.setLong(7, s.spawnersValue());
            ps.setLong(8, s.itemsValue());
            ps.setLong(9, s.blocksValue());
            ps.setLong(10, s.balanceValue());
            ps.setInt(11, s.chunksCount());
            ps.setString(12, mapToJson(s.spawnersCount()));
            ps.setString(13, mapToJson(s.blocksCount()));
            ps.setString(14, s.richestMemberUuid());
            ps.setLong(15, s.richestMemberBalance());
            ps.setLong(16, s.calculatedAt());
            ps.setLong(17, recalcId);
            ps.executeUpdate();
        }
    }

    /** Minimal hand-rolled JSON serializer for Map<String, Integer> (no Gson dep). */
    private static String mapToJson(Map<String, Integer> m) {
        if (m == null || m.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey().replace("\"", "\\\"")).append("\":").append(e.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}
