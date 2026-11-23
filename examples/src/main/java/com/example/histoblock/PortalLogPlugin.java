package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.event.DimensionRecordedEvent;
import me.jaymar921.histoblock.api.model.HistoryRecord;
import me.jaymar921.histoblock.api.model.RecordAction;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code /portalinfo}: the story of the portal you are looking at, plus a live count of trips
 * through each portal since the server started.
 *
 * <pre>
 * name: PortalLog
 * version: 1.0
 * main: com.example.histoblock.PortalLogPlugin
 * api-version: 1.16
 * depend: [Histoblock]
 * commands:
 *   portalinfo:
 *     description: The history of the portal you are looking at
 * </pre>
 */
public final class PortalLogPlugin extends JavaPlugin implements Listener {

    private HistoblockAPI api;
    // Filled from the writer thread, read from command threads: a concurrent map.
    private final ConcurrentHashMap<String, AtomicInteger> trips = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        api = HistoblockProvider.get();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onDimension(DimensionRecordedEvent event) {
        // The dimension category holds two kinds of record: portals (keyed by the portal) and
        // players changing worlds (keyed by the player). Only the first kind matters here.
        if (event.isPortal() && event.getAction() == RecordAction.PORTAL_ENTERED) {
            trips.computeIfAbsent(event.getRecord().subjectKey(), k -> new AtomicInteger()).incrementAndGet();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        Block block = player.getTargetBlockExact(8);
        // Works on the portal blocks and on the frame around them. Block's thread only.
        Optional<String> key = block == null ? Optional.empty() : api.portalKey(block);
        if (!key.isPresent()) {
            player.sendMessage(ChatColor.GRAY + "Look at a nether or end portal, or its frame.");
            return true;
        }

        api.portal(key.get()).thenAccept(view -> {
            HistoryRecord lit = view.created();
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Portal at " + view.corner()
                    + ChatColor.GRAY + (view.isOpen() ? " (open)" : " (broken)"));
            player.sendMessage(ChatColor.GRAY + (lit == null ? "Lit before the records go back."
                    : "First lit by " + lit.actor().name() + " on " + lit.time()));
            player.sendMessage(ChatColor.GRAY + "Lit " + view.timesLit() + " times, used " + view.uses() + " times");
            if (view.firstUse() != null) {
                player.sendMessage(ChatColor.GRAY + "First through: " + view.firstUse().actor().name()
                        + ", last: " + view.lastUse().actor().name());
            }
            AtomicInteger session = trips.get(key.get());
            player.sendMessage(ChatColor.GRAY + "Trips since the server started: " + (session == null ? 0 : session.get()));
        });
        return true;
    }
}
