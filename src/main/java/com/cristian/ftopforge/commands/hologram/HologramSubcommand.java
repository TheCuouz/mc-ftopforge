package com.cristian.ftopforge.commands.hologram;

import com.cristian.ftopforge.FTopForgePlugin;
import com.cristian.ftopforge.i18n.Messages;
import org.bukkit.command.CommandSender;

/**
 * Orchestrates /ftopforge hologram <set|move|info|tp|disable|enable|refresh>.
 * Plain dispatcher class — branches live in this same file as private methods.
 */
public final class HologramSubcommand {

    private final FTopForgePlugin plugin;
    private final Messages msg;
    private final HologramConfigWriter writer;

    public HologramSubcommand(FTopForgePlugin plugin, Messages msg, HologramConfigWriter writer) {
        this.plugin = plugin;
        this.msg = msg;
        this.writer = writer;
    }

    /** args[0] is always "hologram"; subcommand sits in args[1] when present. */
    public void handle(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(msg.get("hologram.usage")); return; }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "set":     handleSet(sender, args); return;
            case "move":    handleMove(sender, args); return;
            case "info":    handleInfo(sender, args); return;
            case "tp":      handleTp(sender, args); return;
            case "disable": handleDisable(sender, args); return;
            case "enable":  handleEnable(sender, args); return;
            case "refresh": handleRefresh(sender, args); return;
            default:        sender.sendMessage(msg.get("hologram.usage")); return;
        }
    }

    private void handleSet(CommandSender s, String[] args)     { s.sendMessage(msg.get("hologram.usage")); }
    private void handleMove(CommandSender s, String[] args)    { s.sendMessage(msg.get("hologram.usage")); }
    private void handleInfo(CommandSender s, String[] args)    { s.sendMessage(msg.get("hologram.usage")); }
    private void handleTp(CommandSender s, String[] args)      { s.sendMessage(msg.get("hologram.usage")); }
    private void handleDisable(CommandSender s, String[] args) { s.sendMessage(msg.get("hologram.usage")); }
    private void handleEnable(CommandSender s, String[] args)  { s.sendMessage(msg.get("hologram.usage")); }
    private void handleRefresh(CommandSender s, String[] args) { s.sendMessage(msg.get("hologram.usage")); }
}
