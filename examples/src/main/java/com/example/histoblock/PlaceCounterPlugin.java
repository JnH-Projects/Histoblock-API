package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.view.RemovedPlacement;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A "place blocks" and "mine blocks" quest that cannot be farmed by placing and breaking the
 * same block over and over. {@code /quest} shows the counts.
 *
 * <ul>
 *   <li>Mining skips a block a player placed, read from the placed mark Histoblock keeps in the
 *   chunk ({@code placement(Block)}, API 2).</li>
 *   <li>Placing skips a spot where the same player's own placed block was taken away in the last
 *   10 minutes ({@code removedPlacement(Block)}, API 3). By the time the block is placed again
 *   its old mark is gone, so {@code placement} alone cannot catch the loop.</li>
 * </ul>
 *
 * <p>Both lookups are live: they read the chunk on the block's thread and return at once. The
 * counters are concurrent maps because Folia fires these events on many region threads.
 *
 * <pre>
 * name: PlaceCounter
 * version: 1.0
 * main: com.example.histoblock.PlaceCounterPlugin
 * api-version: 1.16
 * depend: [Histoblock]
 * folia-supported: true
 * commands:
 *   quest:
 *     description: Shows your quest progress
 * </pre>
 */
public final class PlaceCounterPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, AtomicLong> placed = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicLong> mined = new ConcurrentHashMap<>();
    private HistoblockAPI api;
    private boolean canSeeRemovals;

    @Override
    public void onEnable() {
        api = HistoblockProvider.get();
        // Without API 3 the place quest still works, it just cannot see the loop.
        canSeeRemovals = api.apiVersion() >= 3;
        getServer().getPluginManager().registerEvents(this, this);
    }

    // HIGHEST, not MONITOR: Histoblock clears the placed mark in its own MONITOR handler, so
    // read it before that. Protection plugins cancel at HIGH or lower, so by HIGHEST a break
    // that is still on is almost always going to happen.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (api.placement(event.getBlock()).isPresent()) return;
        mined.computeIfAbsent(event.getPlayer().getUniqueId(), id -> new AtomicLong()).incrementAndGet();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (canSeeRemovals) {
            Optional<RemovedPlacement> before = api.removedPlacement(event.getBlockPlaced());
            boolean ownLoop = before.map(r -> player.getUniqueId().equals(r.placement().placer())).orElse(false);
            if (ownLoop) return;
        }
        placed.computeIfAbsent(player.getUniqueId(), id -> new AtomicLong()).incrementAndGet();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        UUID id = ((Player) sender).getUniqueId();
        sender.sendMessage("Placed: " + count(placed, id) + ", mined: " + count(mined, id));
        return true;
    }

    private static long count(Map<UUID, AtomicLong> map, UUID id) {
        AtomicLong n = map.get(id);
        return n == null ? 0 : n.get();
    }
}
