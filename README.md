# Histoblock: Developer API

The public developer API for [Histoblock](https://www.jhprojects.dev/histoblock), a Spigot,
Paper and Folia plugin that keeps the history of blocks, mobs, players, items, portals and
worlds.

This repository holds the API artifact, the documentation and compilable examples. The plugin
itself is closed source; everything you need to build against it is here.

```java
HistoblockProvider.find().ifPresent(api ->
        api.block(Position.of(block.getLocation())).thenAccept(view -> {
            HistoryRecord placed = view.placement();
            getLogger().info(placed == null ? "natural" : "placed by " + placed.actor().name());
        }));
```

## What the API gives you

| | |
| --- | --- |
| **Records** | every recorded action, filtered by category, action, subject, actor, world, area and time, newest or oldest first |
| **Views** | everything about one block, mob, player, item or portal in one call: who placed it, who hurt it, where it came from, who lit it |
| **Live lookups** | an item's hidden id, where a stack came from, the placed mark of a block, the portal a block belongs to, a player's time per world |
| **Events** | `HistoryRecordedEvent` for every record, plus one typed event per category |

Everything lives under `me.jaymar921.histoblock.api`. Nothing in it names a class outside it
apart from Bukkit and the JDK, and it ships as its own thin jar, the same for the Free and Pro
editions.

The API is read only. It never changes the world, and it never writes or deletes history.

## Start here

1. **[Getting started](docs/01-getting-started.md)**: add the dependency, resolve the API, first lookup.
2. **[Core concepts](docs/02-core-concepts.md)**: records, subjects and keys, retention, threading, editions.
3. **[API reference](docs/03-api-reference.md)**: every method and every view.
4. **[Events](docs/04-events.md)**: the general event and the six typed ones.
5. **[Records reference](docs/05-records.md)**: every action and the details it carries.
6. **[Best practices](docs/06-best-practices.md)**: the rules that keep your plugin fast and correct.

## Examples

Six compilable plugins in [`examples/`](examples/), smallest first:

| Example | Shows |
| --- | --- |
| [QuickStartPlugin](examples/src/main/java/com/example/histoblock/QuickStartPlugin.java) | resolving the API, the version check, one typed listener |
| [WhoPlacedPlugin](examples/src/main/java/com/example/histoblock/WhoPlacedPlugin.java) | `/whoplaced`: the placed mark and the block view |
| [ProvenancePlugin](examples/src/main/java/com/example/histoblock/ProvenancePlugin.java) | `/provenance`: item ids, stack ledgers, origin chains |
| [DeathReportPlugin](examples/src/main/java/com/example/histoblock/DeathReportPlugin.java) | a report on every player death from the combat summary |
| [PortalLogPlugin](examples/src/main/java/com/example/histoblock/PortalLogPlugin.java) | `/portalinfo` and a live trip counter |
| [PlayerHistoryPlugin](examples/src/main/java/com/example/histoblock/PlayerHistoryPlugin.java) | `/history`: a view, a timeline query, time per world |

See [examples/README.md](examples/README.md) for how to build them.

## Downloads

| Plugin | `apiVersion()` | Artifact |
| --- | --- | --- |
| 0.7.0 | 2 | [`Histoblock-api.jar`](releases/0.7.0/Histoblock-api.jar) |

`apiVersion()` goes up when a method, view or event is added. Code written for an older
number keeps working on a newer plugin. See [Versioning](docs/06-best-practices.md#versioning).

## Requirements

| | |
| --- | --- |
| Java | 16 or newer |
| Server | Spigot, Paper or Folia, 1.16.5 or newer |
| Runtime | Histoblock 0.7.0 or newer installed on the server |

## License

The API jar, documentation and examples are [MIT](LICENSE). The Histoblock plugin is not.
