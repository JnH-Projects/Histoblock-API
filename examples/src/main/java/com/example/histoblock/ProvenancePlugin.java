package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.model.HistoryRecord;
import me.jaymar921.histoblock.api.view.ItemSource;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code /provenance}: where the item in your hand came from. Covers both halves of item
 * identity: an item that does not stack has an id of its own and a full history; a stack is
 * matched against the player's ledger, which is exact for one source and a best guess for
 * several.
 *
 * <pre>
 * name: Provenance
 * version: 1.0
 * main: com.example.histoblock.ProvenancePlugin
 * api-version: 1.16
 * depend: [Histoblock]
 * commands:
 *   provenance:
 *     description: Where the item in your hand came from
 * </pre>
 */
public final class ProvenancePlugin extends JavaPlugin {

    private HistoblockAPI api;

    @Override
    public void onEnable() {
        api = HistoblockProvider.get();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            player.sendMessage(ChatColor.GRAY + "Hold something first.");
            return true;
        }

        // A sword, a potion, a shulker box: one item, one id, its whole life on record.
        Optional<UUID> id = api.itemId(hand);
        if (id.isPresent()) {
            api.item(id.get()).thenAccept(view -> {
                HistoryRecord made = view.created();
                player.sendMessage(ChatColor.AQUA + view.name() + ChatColor.GRAY + " "
                        + (made == null ? "made before its history goes back"
                        : "made by " + made.actor().name() + " (" + made.detail("cause") + ")"));
                show(player, view.origin());
            });
            return true;
        }

        // Stackables are never tagged; the ledger says which lots this stack most likely came
        // from. Call on the player's thread, which a command is.
        api.stackSources(player, hand).thenAccept(sources -> {
            if (sources.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Nobody knows where these came from.");
                return;
            }
            if (sources.size() > 1) player.sendMessage(ChatColor.DARK_GRAY + "(a best guess across " + sources.size() + " lots)");
            for (ItemSource s : sources) {
                player.sendMessage(ChatColor.WHITE + "" + s.amount() + ChatColor.GRAY + " of them:");
                show(player, s.origin());
            }
        });
        return true;
    }

    /** One line per step back, newest first: stairs, the planks before them, the log before that. */
    private static void show(Player player, List<HistoryRecord> origin) {
        if (origin.isEmpty()) {
            player.sendMessage(ChatColor.DARK_GRAY + " older than the records go back");
            return;
        }
        for (HistoryRecord step : origin) {
            String how = step.detail("cause");
            player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.WHITE + step.subjectName() + ChatColor.GRAY
                    + " " + (how != null ? how.toLowerCase() : step.action().name().toLowerCase())
                    + " by " + step.actor().name());
        }
    }
}
