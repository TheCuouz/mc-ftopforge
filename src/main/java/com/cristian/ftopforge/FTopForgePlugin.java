package com.cristian.ftopforge;

import com.cristian.ftopforge.i18n.Messages;
import com.cristian.ftopforge.i18n.MessagesLoader;
import com.cristian.ftopforge.util.Banner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class FTopForgePlugin extends JavaPlugin {

    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        String lang = cfg.getString("language", "es");
        String storageType = cfg.getString("storage.type", "sqlite");

        Banner.print(this, storageType);
        this.messages = MessagesLoader.load(this, lang);

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
