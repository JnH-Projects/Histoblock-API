package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small arena that puts itself back after a match, and tells Histoblock it did.
 *
 * <p>{@code /arena pos1} and {@code /arena pos2} on two corner blocks, {@code /arena save} to
 * remember every block between them, {@code /arena reset} to put them all back.
 *
 * <p>The reset uses {@code setBlockData}, which fires no events, so Histoblock cannot see it.
 * Without the {@code blocksRestored} call the arena would keep the placed marks of whatever
 * players built during the match, and the blocks they broke would come back looking natural.
 * A quest plugin reading {@code placement(Block)} would then pay out for mining the arena
 * walls. One call after the reset marks every block in the box as placed by this plugin and
 * writes one {@code BLOCKS_RESTORED} record.
 *
 * <p>Needs API 4 (Histoblock 1.2.0) for the call; on an older Histoblock the arena still resets.
 * On Folia, run the commands standing in the arena: the reset runs on the player's region
 * thread, so the arena has to fit in one region.
 *
 * <pre>
 * name: ArenaReset
 * version: 1.0
 * main: com.example.histoblock.ArenaResetPlugin
 * api-version: 1.16
 * softdepend: [Histoblock]
 * folia-supported: true
 * commands:
 *   arena:
 *     description: Save and reset the arena
 *     permission: arena.admin
 * </pre>
 */
public final class ArenaResetPlugin extends JavaPlugin {

    private Block pos1;
    private Block pos2;
    private World world;
    private BoundingBox box;
    private final List<BlockData> saved = new ArrayList<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || args.length == 0) return false;
        Player player = (Player) sender;
        switch (args[0]) {
            case "pos1":
                pos1 = player.getLocation().getBlock().getRelative(0, -1, 0);
                player.sendMessage("Corner 1 set.");
                return true;
            case "pos2":
                pos2 = player.getLocation().getBlock().getRelative(0, -1, 0);
                player.sendMessage("Corner 2 set.");
                return true;
            case "save":
                return save(player);
            case "reset":
                return reset(player);
            default:
                return false;
        }
    }

    private boolean save(Player player) {
        if (pos1 == null || pos2 == null || !pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage("Set both corners in one world first.");
            return true;
        }
        world = pos1.getWorld();
        // BoundingBox.of(Block, Block) spans both corner blocks completely: the same box
        // blocksRestored expects, from the minimum corner up to, not including, the maximum.
        box = BoundingBox.of(pos1, pos2);
        saved.clear();
        forEachBlock(block -> saved.add(block.getBlockData()));
        player.sendMessage("Saved " + saved.size() + " blocks.");
        return true;
    }

    private boolean reset(Player player) {
        if (box == null) {
            player.sendMessage("Nothing saved yet.");
            return true;
        }
        int[] i = {0};
        forEachBlock(block -> block.setBlockData(saved.get(i[0]++), false));

        // Tell Histoblock, once, right after the blocks are back and while the chunks are
        // loaded. Optional: the arena works without Histoblock or on an older one. Check the
        // plugin is there before touching any API class, since this plugin only soft-depends.
        boolean installed = getServer().getPluginManager().getPlugin("Histoblock") != null;
        int marked = !installed ? -1 : HistoblockProvider.find()
                .filter(api -> api.apiVersion() >= 4)
                .map(this::tellHistoblock)
                .orElse(-1);
        player.sendMessage("Arena reset." + (marked >= 0 ? " Histoblock marked " + marked + " blocks." : ""));
        return true;
    }

    /** In its own method so the API 4 types are only touched once the version is checked. */
    private int tellHistoblock(HistoblockAPI api) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("arena", "main");
        details.put("reason", "match over");
        return api.blocksRestored(this, world, box, details);
    }

    private void forEachBlock(java.util.function.Consumer<Block> work) {
        for (int x = (int) box.getMinX(); x < (int) box.getMaxX(); x++) {
            for (int y = (int) box.getMinY(); y < (int) box.getMaxY(); y++) {
                for (int z = (int) box.getMinZ(); z < (int) box.getMaxZ(); z++) {
                    work.accept(world.getBlockAt(x, y, z));
                }
            }
        }
    }
}
