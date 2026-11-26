package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.model.SaveSpeed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Lets Histoblock save less often while the server is crowded, and puts the owner's speed back
 * when it empties out. {@code /crowdsaver} shows the speed and how long footsteps may take to
 * reach a query.
 *
 * <ul>
 *   <li>{@code saveSpeed()} reads the current speed; {@code tallyDelay()} says how long tallied
 *   records (footsteps, hopper hand-offs) wait before they are saved.</li>
 *   <li>{@code setSaveSpeed} changes it until {@code /hb reload} or a restart. It returns false
 *   when the owner set {@code api.allow-save-speed-change: false}; this plugin then stops asking.</li>
 *   <li>{@code SaveSpeed.parse} reads the busy speed from this plugin's config without throwing
 *   on a typo.</li>
 * </ul>
 *
 * <p>The signal is the player count, read from join and quit events, so it works the same on
 * Folia. The speed only changes when the count crosses the threshold, never on every join.
 *
 * <pre>
 * name: CrowdSaver
 * version: 1.0
 * main: com.example.histoblock.CrowdSaverPlugin
 * api-version: 1.16
 * depend: [Histoblock]
 * folia-supported: true
 * commands:
 *   crowdsaver:
 *     description: Shows Histoblock's save speed
 * </pre>
 *
 * config.yml:
 * <pre>
 * busy-players: 60
 * busy-speed: slow
 * </pre>
 */
public final class CrowdSaverPlugin extends JavaPlugin implements Listener {

    private HistoblockAPI api;
    private int busyPlayers;
    private SaveSpeed busySpeed;
    /** The speed before we changed it; null while we have not. */
    private volatile SaveSpeed before;
    private volatile boolean refused;

    @Override
    public void onEnable() {
        api = HistoblockProvider.get();
        if (api.apiVersion() < 4) {
            getLogger().warning("Needs Histoblock 1.2.0 or newer (API 4). Doing nothing.");
            return;
        }
        saveDefaultConfig();
        busyPlayers = getConfig().getInt("busy-players", 60);
        String raw = getConfig().getString("busy-speed", "slow");
        Optional<SaveSpeed> parsed = SaveSpeed.parse(raw);
        if (!parsed.isPresent()) getLogger().warning("busy-speed '" + raw + "' is not fast, balance or slow. Using slow.");
        busySpeed = parsed.orElse(SaveSpeed.SLOW);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Leave it as we found it. A restart resets it anyway, but a plugin reload would not.
        if (before != null) api.setSaveSpeed(this, before);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        check(getServer().getOnlinePlayers().size());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        // The quitting player is still counted during the event.
        check(getServer().getOnlinePlayers().size() - 1);
    }

    private synchronized void check(int online) {
        if (refused) return;
        if (online >= busyPlayers && before == null) {
            SaveSpeed now = api.saveSpeed();
            if (now == busySpeed) return;
            if (!api.setSaveSpeed(this, busySpeed)) {
                refused = true;
                getLogger().info("The server owner locked Histoblock's save speed. Leaving it alone.");
                return;
            }
            before = now;
        } else if (online < busyPlayers && before != null) {
            api.setSaveSpeed(this, before);
            before = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (api.apiVersion() < 4) return false;
        SaveSpeed speed = api.saveSpeed();
        sender.sendMessage("Histoblock saves at " + speed.name().toLowerCase()
                + ": footsteps can take up to " + speed.tallyDelay().getSeconds() + " s to show up"
                + (before != null ? " (changed by CrowdSaver, was " + before.name().toLowerCase() + ")" : "") + ".");
        return true;
    }
}
