package com.cristian.ftopforge.core;

import com.cristian.ftopforge.storage.JdbcRepository;
import com.cristian.ftopforge.storage.RecalcDao;
import com.cristian.ftopforge.storage.SnapshotDao;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;

public class RecalcRunner {

    private final JavaPlugin plugin;
    private final CalculationEngine engine;
    private final RecalcDao recalcDao;
    private final SnapshotDao snapshotDao;
    private final TopCache cache;
    private final boolean broadcastStart, broadcastFinish;
    private final String startMsg, finishMsgTemplate;

    public RecalcRunner(JavaPlugin plugin, CalculationEngine engine, JdbcRepository repo, TopCache cache,
                        boolean broadcastStart, boolean broadcastFinish,
                        String startMsg, String finishMsgTemplate) {
        this.plugin = plugin;
        this.engine = engine;
        this.recalcDao = new RecalcDao(repo);
        this.snapshotDao = new SnapshotDao(repo);
        this.cache = cache;
        this.broadcastStart = broadcastStart;
        this.broadcastFinish = broadcastFinish;
        this.startMsg = startMsg;
        this.finishMsgTemplate = finishMsgTemplate;
    }

    /** Returns false if a recalc is already running (caller should warn). */
    public boolean trigger() {
        if (engine.isRunning()) return false;

        long startedNanos = System.nanoTime();
        long recalcId;
        try {
            recalcId = recalcDao.startNew();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not insert ftf_recalcs row: " + e.getMessage());
            return false;
        }

        if (broadcastStart && startMsg != null) Bukkit.broadcastMessage(startMsg);

        boolean ok = engine.tryStart(snapshots -> {
            // back on main thread (BukkitScheduler.runTaskTimer is sync)
            try {
                for (FactionSnapshot s : snapshots) snapshotDao.upsert(s, recalcId);
            } catch (SQLException e) {
                plugin.getLogger().severe("Snapshot persist failed: " + e.getMessage());
            }
            cache.rebuild(snapshots);
            long ms = (System.nanoTime() - startedNanos) / 1_000_000L;
            try {
                recalcDao.markFinished(recalcId, ms, snapshots.size());
            } catch (SQLException e) {
                plugin.getLogger().warning("markFinished failed: " + e.getMessage());
            }
            if (broadcastFinish && finishMsgTemplate != null) {
                Bukkit.broadcastMessage(finishMsgTemplate.replace("%duration%", String.valueOf(ms / 1000L)));
            }
            plugin.getLogger().info("[recalc] done id=" + recalcId + " factions=" + snapshots.size() + " ms=" + ms);
        });
        return ok;
    }

    public void recoverStaleOnEnable() {
        try {
            int n = recalcDao.markStaleAsAborted();
            if (n > 0) plugin.getLogger().info("Marked " + n + " stale recalc rows as aborted on startup");
        } catch (SQLException e) {
            plugin.getLogger().warning("recoverStaleOnEnable failed: " + e.getMessage());
        }
    }
}
