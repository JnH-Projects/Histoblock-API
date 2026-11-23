package com.example.histoblock;

import me.jaymar921.histoblock.api.HistoblockAPI;
import me.jaymar921.histoblock.api.HistoblockProvider;
import me.jaymar921.histoblock.api.event.EntityRecordedEvent;
import me.jaymar921.histoblock.api.model.HistoryRecord;
import me.jaymar921.histoblock.api.model.RecordAction;
import me.jaymar921.histoblock.api.view.CombatSummary;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Writes a short report to the console every time a player dies: who struck first, who
 * finished it, how much damage it took, and how many times they have died on record.
 *
 * <p>Shows the two ways to get at the same facts. The death record itself carries the combat
 * summary in its details; the entity view reads it back as a typed {@link CombatSummary}. The
 * view also answers what one record cannot, such as the death count.
 *
 * <pre>
 * name: DeathReport
 * version: 1.0
 * main: com.example.histoblock.DeathReportPlugin
 * api-version: 1.16
 * depend: [Histoblock]
 * </pre>
 */
public final class DeathReportPlugin extends JavaPlugin implements Listener {

    private HistoblockAPI api;

    @Override
    public void onEnable() {
        api = HistoblockProvider.get();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onEntity(EntityRecordedEvent event) {
        // Writer thread. Keep the listener short: filter, then hand off to an async lookup.
        if (event.getAction() != RecordAction.ENTITY_DIED || !event.isPlayer()) return;
        UUID who = event.getEntityId();
        if (who == null) return;

        HistoryRecord death = event.getRecord();
        // Reading straight from the record: every key is a string, and absent when unknown.
        String cause = death.detail("cause");

        // The record is committed before the event fires, so the view already includes it.
        api.player(who).thenAccept(view -> {
            CombatSummary c = view.combat();
            StringBuilder line = new StringBuilder(view.name()).append(" died (").append(cause).append(')');
            if (c.first() != null) {
                line.append(", first hit by ").append(c.first().name())
                        .append(", finished by ").append(c.last().name())
                        .append(" after ").append(c.hits()).append(" hits and ")
                        .append(String.format("%.1f", c.damageTaken())).append(" damage");
            }
            line.append(". Deaths on record: ").append(view.deaths());
            getLogger().info(line.toString());
        }).exceptionally(error -> {
            getLogger().warning("Could not read " + who + ": " + error.getMessage());
            return null;
        });
    }
}
