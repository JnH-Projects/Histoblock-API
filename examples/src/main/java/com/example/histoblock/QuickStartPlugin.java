package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.event.BlockRecordedEvent;
import me.jaymar921.histoblock.api.model.RecordAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The smallest useful hook: find the API, check its version, count explosions as they are
 * recorded, and say how many there were on shutdown.
 *
 * <pre>
 * name: QuickStart
 * version: 1.0
 * main: com.example.histoblock.QuickStartPlugin
 * api-version: 1.16
 * softdepend: [Histoblock]
 * </pre>
 */
public final class QuickStartPlugin extends JavaPlugin implements Listener {

    // Record events are fired from Histoblock's writer thread, never the server thread, so
    // anything a listener touches has to be thread-safe. An AtomicLong is enough here.
    private final AtomicLong explosions = new AtomicLong();

    @Override
    public void onEnable() {
        // softdepend means Histoblock enables first when it is there, and we still load when
        // it is not. find() is the lookup for a plugin that works either way.
        HistoblockAPI api = HistoblockProvider.find().orElse(null);
        if (api == null) {
            getLogger().info("Histoblock is not installed; explosion counting is off.");
            return;
        }

        // The views and typed events came in API 2 (Histoblock 0.7.0). An older Histoblock's API
        // has no BlockRecordedEvent class at all, so registering this listener there would fail
        // with NoClassDefFoundError. Check first, register after.
        if (api.apiVersion() < 2) {
            getLogger().warning("Histoblock " + api.version() + " is too old for this plugin; it needs 0.7.0.");
            return;
        }

        getLogger().info("Hooked into Histoblock " + api.version() + " (" + api.edition() + ")");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBlock(BlockRecordedEvent event) {
        // A typed event only fires for its own category, so this sees block records and
        // nothing else. The record is already saved; there is nothing to cancel.
        if (event.getAction() == RecordAction.BLOCK_EXPLODED) {
            explosions.incrementAndGet();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info(explosions.get() + " blocks were blown up this session.");
    }
}
