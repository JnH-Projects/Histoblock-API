package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.model.Position;
import me.jaymar921.histoblock.api.view.RestoreResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletionException;

/**
 * {@code /undogrief <player> <minutes>}: undoes what one player changed within 16 blocks of you
 * in the last few minutes, blocks and chest contents, and says who walked off with items.
 *
 * <p>Histoblock plans the restore from its records, sets the blocks chunk by chunk on the right
 * threads, and records every change, so this plugin never touches a block. Restricted to one
 * player, a block somebody else changed afterwards is left alone: the repair a builder already
 * made is not undone. {@link RestoreResult#conflicts()} counts those.
 *
 * <p>The future completes off the server thread. Sending a chat message from there is fine;
 * anything that touches the world would have to hop back first.
 *
 * <p>Needs API 5 (Histoblock 1.3.0). On an older Histoblock the default {@code restore} fails
 * with {@link UnsupportedOperationException}, so the version is checked first.
 *
 * <pre>
 * name: GriefUndo
 * version: 1.0
 * main: com.example.histoblock.GriefUndoPlugin
 * api-version: 1.16
 * softdepend: [Histoblock]
 * folia-supported: true
 * commands:
 *   undogrief:
 *     description: Undo what one player changed around you
 *     usage: /undogrief &lt;player&gt; &lt;minutes&gt;
 *     permission: griefundo.use
 * </pre>
 */
public final class GriefUndoPlugin extends JavaPlugin {

    private static final int RADIUS = 16;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || args.length != 2) return false;
        Player admin = (Player) sender;
        HistoblockAPI api = HistoblockProvider.find().orElse(null);
        if (api == null || api.apiVersion() < 5) {
            admin.sendMessage("This needs Histoblock 1.3.0 or newer.");
            return true;
        }
        OfflinePlayer griefer = Bukkit.getOfflinePlayer(args[0]);
        int minutes;
        try {
            minutes = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        // The player's own block, read on the player's thread, which a command already is.
        Position here = Position.of(admin.getLocation());
        Instant since = Instant.now().minus(Duration.ofMinutes(minutes));
        admin.sendMessage("Undoing what " + args[0] + " changed in the last " + minutes + " minutes...");
        api.restore(this, here, RADIUS, since, griefer.getUniqueId()).whenComplete((result, error) -> {
            if (error != null) {
                // IllegalStateException: another restore is running, block tracking is off, or
                // the area has too much history. IllegalArgumentException: radius too big.
                Throwable why = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                admin.sendMessage("Could not undo it: " + why.getMessage());
                return;
            }
            admin.sendMessage("Put back " + result.blocks() + " blocks and " + result.containers() + " chests.");
            if (result.conflicts() > 0) {
                admin.sendMessage(result.conflicts() + " blocks were left alone: someone changed them after " + args[0] + ".");
            }
            // Restoring a chest does not take the items back from whoever took them out.
            for (Map.Entry<String, Integer> e : result.takenOut().entrySet()) {
                admin.sendMessage(e.getKey() + " took " + e.getValue() + " items out of a restored chest.");
            }
        });
        return true;
    }
}
