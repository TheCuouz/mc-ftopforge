package com.cristian.ftopforge.commands;

import com.cristian.ftopforge.core.FactionSnapshot;
import com.cristian.ftopforge.core.TopCache;
import com.cristian.ftopforge.i18n.Messages;
import com.cristian.ftopforge.util.MoneyFormat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class FTopCommand implements CommandExecutor {

    private final TopCache cache;
    private final Messages msg;
    private final int perPage;

    public FTopCommand(TopCache cache, Messages msg, int perPage) {
        this.cache = cache;
        this.msg = msg;
        this.perPage = Math.max(1, perPage);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
            sender.sendMessage("§7GUI coming in Sesión Custom 8.");
            return true;
        }
        int page = 1;
        if (args.length > 0) {
            try { page = Math.max(1, Integer.parseInt(args[0])); } catch (NumberFormatException ignored) {}
        }
        int total = cache.size();
        if (total == 0) {
            sender.sendMessage(msg.get("calculation.starting"));
            return true;
        }
        int pages = (int) Math.ceil((double) total / perPage);
        if (page > pages) page = pages;
        int from = (page - 1) * perPage;
        int to = Math.min(from + perPage, total);
        List<FactionSnapshot> top = cache.top(total);

        sender.sendMessage(msg.get("command.ftop.header", "page", String.valueOf(page)));
        for (int i = from; i < to; i++) {
            FactionSnapshot s = top.get(i);
            String line = msg.get("command.ftop.line",
                    "rank", String.valueOf(i + 1),
                    "faction_name", s.factionName(),
                    "value", MoneyFormat.shortFmt(s.totalValue()));
            sender.sendMessage(line);
        }
        return true;
    }
}
