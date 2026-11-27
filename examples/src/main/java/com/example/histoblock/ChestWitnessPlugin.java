package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.model.Position;
import me.jaymar921.histoblock.api.view.ContainerSnapshot;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@code /chestwas [minutes]}: what the chest, barrel or shulker box you look at held that many
 * minutes ago, 60 by default.
 *
 * <p>{@code container(position, at)} returns the latest snapshot at or before that time.
 * Histoblock takes one when a player closes a container after changing it, the first time one
 * is opened after a restart, when one is broken, and while hoppers feed it, so "an hour ago"
 * is the state after the last change before then. A container nobody touched has none, and
 * the answer is empty.
 *
 * <p>The snapshot's items are copies, rebuilt off the server thread. This only counts them; to
 * put them anywhere, hop back to the block's thread first.
 *
 * <p>Needs API 5 (Histoblock 1.3.0). On an older Histoblock {@code container} always answers
 * empty, which is why the version is checked rather than trusting an empty answer.
 *
 * <pre>
 * name: ChestWitness
 * version: 1.0
 * main: com.example.histoblock.ChestWitnessPlugin
 * api-version: 1.16
 * softdepend: [Histoblock]
 * folia-supported: true
 * commands:
 *   chestwas:
 *     description: What the container you look at held earlier
 *     usage: /chestwas [minutes]
 * </pre>
 */
public final class ChestWitnessPlugin extends JavaPlugin {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;
        HistoblockAPI api = HistoblockProvider.find().orElse(null);
        if (api == null || api.apiVersion() < 5) {
            player.sendMessage("This needs Histoblock 1.3.0 or newer.");
            return true;
        }
        int minutes = 60;
        if (args.length > 0) {
            try {
                minutes = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage("Look at a chest, barrel or shulker box.");
            return true;
        }
        Instant then = Instant.now().minus(Duration.ofMinutes(minutes));
        int ago = minutes;
        api.container(Position.of(target.getLocation()), then).thenAccept(found -> {
            if (!found.isPresent()) {
                player.sendMessage("Nothing on record for that container " + ago + " minutes ago.");
                return;
            }
            ContainerSnapshot s = found.get();
            player.sendMessage(ago + " minutes ago it held " + s.itemCount() + " items ("
                    + s.cause().toLowerCase() + " by " + s.actor().name() + " at " + s.time() + "):");
            Map<String, Integer> byMaterial = new TreeMap<>();
            for (ItemStack item : s.contents().values()) byMaterial.merge(item.getType().name(), item.getAmount(), Integer::sum);
            byMaterial.forEach((m, n) -> player.sendMessage("  " + n + " " + m));
        });
        return true;
    }
}
