package com.cristian.ftopforge;

import com.cristian.ftopforge.hooks.FactionsUUIDHook;
import com.cristian.ftopforge.hooks.VaultHook;
import com.cristian.ftopforge.i18n.Messages;
import com.cristian.ftopforge.i18n.MessagesLoader;
import com.cristian.ftopforge.util.Banner;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class FTopForgePlugin extends JavaPlugin {

    private Messages messages;
    private FactionsUUIDHook factionsHook;
    private VaultHook vaultHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        String lang = cfg.getString("language", "es");
        String storageType = cfg.getString("storage.type", "sqlite");

        Banner.print(this, storageType);
        this.messages = MessagesLoader.load(this, lang);

        try {
            this.factionsHook = FactionsUUIDHook.initOrFail();
            getLogger().info("FactionsUUID hook OK");
        } catch (IllegalStateException e) {
            getLogger().severe("FactionsUUID hook failed: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        try {
            this.vaultHook = VaultHook.initOrFail();
            getLogger().info("Vault hook OK");
        } catch (IllegalStateException e) {
            getLogger().severe("Vault hook failed: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Loaded messages (lang=" + lang + ", storage=" + storageType + ")");
        getLogger().info("FTopForge enabled (banner-only; hooks + engine wired in later tasks)");
    }

    @Override
    public void onDisable() {
        getLogger().info("FTopForge disabled");
    }

    public Messages messages() {
        return messages;
    }
}
