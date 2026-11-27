# Examples

Twelve complete plugins, smallest first. Every one compiles against the API jar in
[`releases/`](../releases/), and each is commented with the reasoning, not only the mechanics.

They are meant to be **read in order and then copied**. Each file's javadoc header carries the
`plugin.yml` that goes with it.

| | Example | Reads | Threads it crosses |
| --- | --- | --- | --- |
| 1 | [QuickStartPlugin](src/main/java/com/example/histoblock/QuickStartPlugin.java) | the provider, `BlockRecordedEvent` | writer thread |
| 2 | [WhoPlacedPlugin](src/main/java/com/example/histoblock/WhoPlacedPlugin.java) | `placement(Block)`, `block(Position)` | block thread, reader thread |
| 3 | [ProvenancePlugin](src/main/java/com/example/histoblock/ProvenancePlugin.java) | `itemId`, `item(UUID)`, `stackSources` | player thread, reader thread |
| 4 | [DeathReportPlugin](src/main/java/com/example/histoblock/DeathReportPlugin.java) | `EntityRecordedEvent`, `player(UUID)`, `CombatSummary` | writer thread, reader thread |
| 5 | [PortalLogPlugin](src/main/java/com/example/histoblock/PortalLogPlugin.java) | `DimensionRecordedEvent`, `portalKey`, `portal(key)` | all three |
| 6 | [PlayerHistoryPlugin](src/main/java/com/example/histoblock/PlayerHistoryPlugin.java) | `timeInWorlds`, `player(UUID)`, `records(RecordQuery)` | player thread, reader thread |
| 7 | [KillCreditPlugin](src/main/java/com/example/histoblock/KillCreditPlugin.java) | `killCredit`, `combat(LivingEntity)` (API 3) | entity thread only |
| 8 | [PlaceCounterPlugin](src/main/java/com/example/histoblock/PlaceCounterPlugin.java) | `placement(Block)`, `removedPlacement(Block)` (API 3) | block thread only |
| 9 | [ArenaResetPlugin](src/main/java/com/example/histoblock/ArenaResetPlugin.java) | `blocksRestored` (API 4) | the arena's thread only |
| 10 | [CrowdSaverPlugin](src/main/java/com/example/histoblock/CrowdSaverPlugin.java) | `saveSpeed`, `setSaveSpeed`, `SaveSpeed.parse` (API 4) | any thread |
| 11 | [ChestWitnessPlugin](src/main/java/com/example/histoblock/ChestWitnessPlugin.java) | `container(Position, Instant)`, `ContainerSnapshot` (API 5) | player thread, reader thread |
| 12 | [GriefUndoPlugin](src/main/java/com/example/histoblock/GriefUndoPlugin.java) | `restore`, `RestoreResult` (API 5) | player thread, then Histoblock's own |

---

## What each one teaches

### 1. QuickStartPlugin

The smallest useful hook. `HistoblockProvider.find()` for a plugin that works without
Histoblock, the `apiVersion()` check that must come before registering a typed listener, and
a thread-safe counter fed from the writer thread.

### 2. WhoPlacedPlugin

`/whoplaced`. The two kinds of lookup side by side: the live placed mark, read on the block's
own thread and never aged out, and the block view, read from the records on a reader thread
with footsteps and the origin chain of the block.

### 3. ProvenancePlugin

`/provenance`. Both halves of item identity: the id an unstackable item carries, and the
ledger guess for a stack. Walks an origin chain back from stairs to planks to the log.

### 4. DeathReportPlugin

A console line for every player death. Reads the same facts two ways, from the record's
details and from the typed `CombatSummary` of the player view, and shows why a listener should
hand slow work to an async lookup rather than doing it inline.

### 5. PortalLogPlugin

`/portalinfo`. Finds the portal from any of its blocks or its frame, reads its view, and keeps
a per-portal trip counter from `DimensionRecordedEvent`, telling portal records apart from
players changing worlds.

### 6. PlayerHistoryPlugin

`/history <player> [hours]`. A view for the summary, a `RecordQuery` with `oldestFirst()` for
the timeline, and the live time per world for a player who is online.

### 7. KillCreditPlugin

Credits the player behind a kill vanilla gives to nobody, such as a zombie knocked into lava.
Everything happens inside `EntityDeathEvent` with no future: `killCredit` gives the actor the
death record will name, and `combat(LivingEntity)` the whole fight, killing blow included.
Shows the `apiVersion() >= 3` check for a plugin that should still load on an older Histoblock.

### 8. PlaceCounterPlugin

A place and mine quest that cannot be farmed. Mining skips player placed blocks through the
placed mark, read at `HIGHEST` before Histoblock clears it. Placing skips a spot where the same
player's own block was taken away in the last 10 minutes, which `placement` alone cannot see
once the block is gone.

### 9. ArenaResetPlugin

`/arena pos1`, `pos2`, `save`, `reset`. An arena that puts its blocks back with
`setBlockData`, which no plugin can see, and then calls `blocksRestored` once so Histoblock
marks the whole arena as the plugin's instead of keeping the match's player marks. Shows the
soft dependency done right: the arena works without Histoblock, and the API 4 call sits in its
own method behind `apiVersion() >= 4`.

### 10. CrowdSaverPlugin

Slows Histoblock's saving down while the server is crowded and puts the owner's speed back
when it empties. Reads the speed and `tallyDelay()`, changes it only when the player count
crosses the line, stops asking once the owner refused, reads its own config with
`SaveSpeed.parse`, and restores the old speed on disable.

### 11. ChestWitnessPlugin

`/chestwas [minutes]` on the container you look at: what it held then, by material, and who
last changed it. Reads `container(Position, Instant)` and the `ContainerSnapshot` it returns,
and checks `apiVersion() >= 5` rather than trusting an empty answer, which an older Histoblock
gives for everything.

### 12. GriefUndoPlugin

`/undogrief <player> <minutes>`: undoes what one player changed around you. Histoblock plans and
applies the restore itself, so the plugin never touches a block; it only reads the
`RestoreResult`: blocks and chests put back, blocks left alone because someone else changed
them later, and who took items out of the restored chests.

---

## Build them

```bash
cd examples
mvn package
```

Java 16 or newer, Maven 3.8 or newer. The API comes from `../releases/1.3.0/` with system
scope, so there is no install step. The resulting jar holds every example at once and has no
`plugin.yml`: it proves they compile and is not meant to be dropped on a server. To run one,
copy its class into your own plugin together with the `plugin.yml` from its header.
