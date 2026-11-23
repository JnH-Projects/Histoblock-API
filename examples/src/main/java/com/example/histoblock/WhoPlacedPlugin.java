package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.model.HistoryRecord;
import me.jaymar921.histoblock.api.model.Position;
import me.jaymar921.histoblock.api.view.Footsteps;
import me.jaymar921.histoblock.api.view.Placement;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * {@code /whoplaced}: who placed the block you are looking at, who walks on it, and what it
 * was made from. Shows the two kinds of lookup side by side: the live {@code placement(Block)}
 * that reads the chunk on the spot, and the {@code block(Position)} view read from the records
 * off the server thread.
 *
 * <pre>
 * name: WhoPlaced
 * version: 1.0
 * main: com.example.histoblock.WhoPlacedPlugin
 * api-version: 1.16
 * depend: [Histoblock]
 * commands:
 *   whoplaced:
 *     description: Who placed the block you are looking at
 * </pre>
 */
public final class WhoPlacedPlugin extends JavaPlugin {

    private HistoblockAPI api;

    @Override
    public void onEnable() {
        // depend, not softdepend: this plugin does nothing without Histoblock, so get() is
        // right. It throws with a clear message if Histoblock failed to enable.
        api = HistoblockProvider.get();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player player = (Player) sender;
        Block block = player.getTargetBlockExact(6);
        if (block == null) {
            player.sendMessage(ChatColor.GRAY + "Look at a block first.");
            return true;
        }

        // 1. The live lookup. We are on the thread that owns the block (the main thread, or
        //    the player's region on Folia), so this is a direct read. The mark never ages
        //    out, so it can answer for a block placed long before the records go back.
        Optional<Placement> mark = api.placement(block);
        player.sendMessage(ChatColor.AQUA + block.getType().name() + ChatColor.GRAY + " at "
                + block.getX() + ", " + block.getY() + ", " + block.getZ());
        player.sendMessage(mark
                .map(p -> ChatColor.GRAY + "Placed by " + ChatColor.WHITE + p.placerName()
                        + ChatColor.GRAY + " on " + p.time())
                .orElse(ChatColor.GRAY + "Natural, or placed before Histoblock was tracking it."));

        // 2. The view. It reads the database on a Histoblock reader thread and completes
        //    there. Sending chat from that thread is fine; touching the world is not, so a
        //    plugin that wants to change the block would hop back with its scheduler first.
        api.block(Position.of(block.getLocation())).whenComplete((view, error) -> {
            if (error != null) {
                player.sendMessage(ChatColor.RED + "Could not read the history: " + error.getMessage());
                return;
            }
            Footsteps steps = view.footsteps();
            if (!steps.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Walked on " + steps.total() + " times by " + steps.unique()
                        + " players, last by " + steps.lastWalker());
            }
            if (!view.madeFrom().isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Made from:");
                for (HistoryRecord step : view.madeFrom()) {
                    player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.WHITE + step.subjectName()
                            + ChatColor.GRAY + " " + step.action() + " by " + step.actor().name());
                }
            }
            player.sendMessage(ChatColor.GRAY + "" + view.recent().size() + " recent records, status "
                    + view.status());
        });
        return true;
    }
}
