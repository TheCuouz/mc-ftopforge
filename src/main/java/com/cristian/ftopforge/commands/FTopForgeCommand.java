package com.cristian.ftopforge.commands;

import com.cristian.ftopforge.FTopForgePlugin;
import com.cristian.ftopforge.core.RecalcRunner;
import com.cristian.ftopforge.i18n.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FTopForgeCommand implements CommandExecutor {

    private final FTopForgePlugin plugin;
    private final RecalcRunner runner;
    private final Messages msg;

    public FTopForgeCommand(FTopForgePlugin plugin, RecalcRunner runner, Messages msg) {
        this.plugin = plugin;
        this.runner = runner;
        this.msg = msg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7/ftopforge <recalc|reload|version> [args]");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "version":
                sender.sendMessage("§6FTopForge §7v" + plugin.getDescription().getVersion());
                sender.sendMessage("§7modules enabled: §fcore");
                return true;
            case "recalc":
                if (!sender.hasPermission("ftopforge.admin")) { sender.sendMessage(msg.get("errors.no-permission")); return true; }
                if (runner.trigger()) sender.sendMessage(msg.get("admin.recalc-triggered"));
                else sender.sendMessage(msg.get("errors.recalc-in-progress"));
                return true;
            case "reload":
                if (!sender.hasPermission("ftopforge.admin")) { sender.sendMessage(msg.get("errors.no-permission")); return true; }
                plugin.reloadConfig();
                plugin.reloadMessages();
                sender.sendMessage(msg.get("admin.reloaded"));
                return true;
            case "worth":
            case "history":
            case "forensics":
            case "rewards":
                sender.sendMessage("§7'" + sub + "' subcommand lands in Custom 8/9.");
                return true;
            default:
                sender.sendMessage("§7Unknown subcommand: " + sub);
                return true;
        }
    }
}
