package com.cristian.ftopforge;

import com.cristian.ftopforge.commands.FTopCommand;
import com.cristian.ftopforge.commands.FTopForgeCommand;
import com.cristian.ftopforge.core.BalanceAggregator;
import com.cristian.ftopforge.core.BlockValuator;
import com.cristian.ftopforge.core.CalculationEngine;
import com.cristian.ftopforge.core.ChunkValuator;
import com.cristian.ftopforge.core.ItemPrices;
import com.cristian.ftopforge.core.ItemValuator;
import com.cristian.ftopforge.core.RecalcRunner;
import com.cristian.ftopforge.core.SpawnerValuator;
import com.cristian.ftopforge.core.TopCache;
import com.cristian.ftopforge.hooks.FactionsUUIDHook;
import com.cristian.ftopforge.hooks.VaultHook;
import com.cristian.ftopforge.i18n.Messages;
import com.cristian.ftopforge.i18n.MessagesLoader;
import com.cristian.ftopforge.storage.JdbcRepository;
import com.cristian.ftopforge.util.Banner;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class FTopForgePlugin extends JavaPlugin {

    private Messages messages;
    private FactionsUUIDHook factionsHook;
    private VaultHook vaultHook;
    private JdbcRepository repo;
    private TopCache topCache;
    private RecalcRunner recalcRunner;
    private int recalcTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("items.yml", false);
        saveResource("messages-es.yml", false);
        saveResource("messages-en.yml", false);

        FileConfiguration cfg = getConfig();
        String lang = cfg.getString("language", "es");
        String storageType = cfg.getString("storage.type", "sqlite");

        Banner.print(this, storageType);
        this.messages = MessagesLoader.load(this, lang);

        try {
            this.factionsHook = FactionsUUIDHook.initOrFail();
            this.vaultHook = VaultHook.initOrFail();
        } catch (IllegalStateException e) {
            getLogger().severe("Hard-dep init failed: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        try {
            this.repo = JdbcRepository.init(this);
        } catch (SQLException e) {
            getLogger().severe("Storage init failed: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        ItemPrices prices = ItemPrices.loadFromYaml(this);
        BlockValuator bv = new BlockValuator(prices, cfg.getConfigurationSection("Blocks"));
        SpawnerValuator sv = new SpawnerValuator(prices);
        ItemValuator iv = new ItemValuator(prices);
        ChunkValuator cv = new ChunkValuator(bv, sv, iv,
                cfg.getBoolean("options.include-block-value-in-total-worth", true),
                cfg.getBoolean("options.include-spawner-value-in-total-worth", true),
                cfg.getBoolean("options.include-item-worth-in-total-worth", true));

        BalanceAggregator ba = new BalanceAggregator(vaultHook,
                cfg.getBoolean("options.include-player-balance-in-total-worth", true),
                cfg.getBoolean("options.include-offline-balances", false));

        CalculationEngine engine = new CalculationEngine(this, factionsHook, cv, ba,
                cfg.getInt("options.chunks-calculated-per-second", 20),
                cfg.getInt("options.player-balances-calculated-per-second", 20));

        this.topCache = new TopCache();
        this.recalcRunner = new RecalcRunner(this, engine, repo, topCache,
                cfg.getBoolean("options.broadcast-when-starting-calculation", true),
                cfg.getBoolean("options.broadcast-when-stopping-calculation", true),
                messages.get("calculation.starting"),
                messages.get("calculation.finished"));

        recalcRunner.recoverStaleOnEnable();

        getCommand("ftop").setExecutor(new FTopCommand(topCache, messages, cfg.getInt("options.factions-per-page", 10)));
        getCommand("ftopforge").setExecutor(new FTopForgeCommand(this, recalcRunner, messages));

        long delaySeconds = cfg.getLong("options.ftop-recalculation-delay-seconds", 1800L);
        long ticks = Math.max(20L, delaySeconds * 20L);
        this.recalcTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> recalcRunner.trigger(), ticks, ticks).getTaskId();

        getLogger().info("FTopForge ready. First recalc in ~" + delaySeconds + "s. Use /ftopforge recalc to force.");
    }

    @Override
    public void onDisable() {
        if (recalcTaskId != -1) Bukkit.getScheduler().cancelTask(recalcTaskId);
        if (repo != null) repo.shutdown();
        getLogger().info("FTopForge disabled");
    }

    public void reloadMessages() {
        String lang = getConfig().getString("language", "es");
        this.messages = MessagesLoader.load(this, lang);
    }

    public Messages messages() { return messages; }
    public TopCache topCache() { return topCache; }
}
