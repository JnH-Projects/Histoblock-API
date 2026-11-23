# Examples

Six complete plugins, smallest first. Every one compiles against the API jar in
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

---

## Build them

```bash
cd examples
mvn package
```

Java 16 or newer, Maven 3.8 or newer. The API comes from `../releases/0.7.0/` with system
scope, so there is no install step. The resulting jar holds every example at once and has no
`plugin.yml`: it proves they compile and is not meant to be dropped on a server. To run one,
copy its class into your own plugin together with the `plugin.yml` from its header.
