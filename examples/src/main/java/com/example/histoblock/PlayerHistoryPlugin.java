package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.model.Category;
import me.jaymar921.histoblock.api.model.HistoryRecord;
import me.jaymar921.histoblock.api.query.RecordQuery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /history <player> [hours]}: a player's profile and what they did lately, in the
 * order it happened. Uses all three ways in: a view for the summary, a query for the
 * timeline, and a live lookup for the time per world.
 *
 * <pre>
 * name: PlayerHistory
 * version: 1.0
 * main: com.example.histoblock.PlayerHistoryPlugin
 * api-version: 1.16
 * depend: [Histoblock]
 * commands:
 *   history:
 *     description: A player's recent history
 *     usage: /history &lt;player&gt; [hours]
 *     permission: playerhistory.use
 * </pre>
 */
public final class PlayerHistoryPlugin extends JavaPlugin {

    private HistoblockAPI api;

    @Override
    public void onEnable() {
        api = HistoblockProvider.get();
    }

    @Override
    @SuppressWarnings("deprecation") // getOfflinePlayer(String) is the only lookup by name on 1.16
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID id = target.getUniqueId();
        int hours = args.length > 1 ? parse(args[1], 24) : 24;

        // The live part first, while we are on a thread that may read the player. Only an
        // online player carries their world times; for anyone else we skip it.
        Player online = target.getPlayer();
        if (online != null) {
            Map<String, Duration> worlds = api.timeInWorlds(online);
            StringBuilder sb = new StringBuilder(ChatColor.GRAY + "Time in ");
            worlds.forEach((world, time) -> sb.append(ChatColor.WHITE).append(world).append(ChatColor.GRAY)
                    .append(' ').append(time.toHours()).append("h ").append(time.toMinutes() % 60).append("m  "));
            if (!worlds.isEmpty()) sender.sendMessage(sb.toString());
        }

        // The view: one lookup, a summary of the player.
        api.player(id).thenAccept(view -> {
            if (!view.isKnown()) {
                sender.sendMessage(ChatColor.GRAY + "Nothing on record for " + args[0] + ".");
                return;
            }
            HistoryRecord left = view.lastLeft();
            sender.sendMessage(ChatColor.AQUA + view.name() + ChatColor.GRAY + ", deaths on record: " + view.deaths()
                    + (left == null ? "" : ", last left " + left.time()));
        });

        // The query: everything they did themselves in the window, oldest first, leaving
        // out vitals snapshots by asking only for the categories that read as a story.
        RecordQuery query = RecordQuery.builder()
                .actor(id)
                .category(Category.BLOCK)
                .category(Category.PLAYER)
                .category(Category.DIMENSION)
                .since(Instant.now().minus(Duration.ofHours(hours)))
                .oldestFirst()
                .limit(15)
                .build();
        api.records(query).thenAccept(records -> {
            for (HistoryRecord r : records) {
                sender.sendMessage(ChatColor.DARK_GRAY + r.time().toString().substring(11, 19) + " "
                        + ChatColor.WHITE + r.action() + ChatColor.GRAY + " " + r.subjectName()
                        + (r.position() == null ? "" : " at " + r.position()));
            }
        }).exceptionally(error -> {
            sender.sendMessage(ChatColor.RED + "Could not read the history: " + error.getMessage());
            return null;
        });
        return true;
    }

    private static int parse(String s, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
