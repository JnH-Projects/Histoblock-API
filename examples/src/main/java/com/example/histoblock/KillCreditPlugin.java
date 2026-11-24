package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.model.Actor;
import me.jaymar921.histoblock.api.view.CombatSummary;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Credits the player behind a kill that vanilla gives to nobody: the zombie knocked into lava,
 * the skeleton punched off a cliff, the creeper left burning. Tells the player, with the fight
 * as Histoblock counted it.
 *
 * <p>Everything here happens inside the death event, on the entity's own thread, with no
 * future and no database read. {@code killCredit} gives the same actor Histoblock is about to
 * write on the {@code ENTITY_DIED} record, so there is no need to wait for the record and match
 * it back to the death.
 *
 * <p>Needs API 3 (Histoblock 1.1.0). On an older Histoblock the listener is never registered.
 *
 * <pre>
 * name: KillCredit
 * version: 1.0
 * main: com.example.histoblock.KillCreditPlugin
 * api-version: 1.16
 * softdepend: [Histoblock]
 * folia-supported: true
 * </pre>
 */
public final class KillCreditPlugin extends JavaPlugin implements Listener {

    private HistoblockAPI api;

    @Override
    public void onEnable() {
        api = HistoblockProvider.find().orElse(null);
        if (api == null || api.apiVersion() < 3) {
            getLogger().info("Needs Histoblock 1.1.0 or newer; kill credit is off.");
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        // Vanilla already pays this one through getKiller(). We only want the ones it misses.
        if (dead.getKiller() != null || dead instanceof Player) return;

        Actor credit = api.killCredit(dead).orElse(null);
        // The answer can be a mob (a skeleton shot it) or a block (a cactus), not only a player.
        if (credit == null || !credit.isPlayer() || credit.uuid() == null) return;
        Player player = Bukkit.getPlayer(credit.uuid());
        if (player == null) return;

        // Still inside the death event, so the ledger is the whole fight, killing blow included.
        CombatSummary fight = api.combat(dead);
        String line = "You get the kill on the " + dead.getType().name().toLowerCase()
                + " (" + fight.hits() + " hits, " + String.format("%.1f", fight.damageTaken()) + " damage";
        if (dead.getLastDamageCause() != null) {
            line += ", finished by " + dead.getLastDamageCause().getCause().name().toLowerCase();
        }
        // Player messages are safe from any thread, so no hop is needed on Folia either.
        player.sendMessage(line + ").");
    }
}
