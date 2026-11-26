# 1. Getting started

[Index](README.md) | Next: [Core concepts](02-core-concepts.md)

From an empty plugin to a working lookup.

---

## Requirements

| | |
| --- | --- |
| Java | 16 or newer |
| Server | Spigot, Paper or Folia, 1.16.5 or newer |
| Runtime | Histoblock 0.7.0 or newer installed on the server; 1.1.0 or newer for API 3, 1.2.0 or newer for API 4 |

You do **not** need the plugin jar to compile. You compile against `Histoblock-api.jar`,
which holds interfaces, value classes and events only. It is the same jar for the Free and
Pro editions.

---

## Step 1: add the dependency

Download [`Histoblock-api.jar`](../releases/1.2.0/Histoblock-api.jar), then pick one of the
three ways below.

### Maven (install to your local repository)

```bash
mvn install:install-file \
  -Dfile=Histoblock-api.jar \
  -DgroupId=me.jaymar921 \
  -DartifactId=histoblock-api \
  -Dversion=1.2.0 \
  -Dpackaging=jar
```

```xml
<dependency>
    <groupId>me.jaymar921</groupId>
    <artifactId>histoblock-api</artifactId>
    <version>1.2.0</version>
    <scope>provided</scope>
</dependency>
```

### Maven (jar checked into your project)

```xml
<dependency>
    <groupId>me.jaymar921</groupId>
    <artifactId>histoblock-api</artifactId>
    <version>1.2.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/Histoblock-api.jar</systemPath>
</dependency>
```

### Gradle

```groovy
dependencies {
    compileOnly files('libs/Histoblock-api.jar')
}
```

> **`provided` or `compileOnly`, always.** Never shade the API jar into your plugin. Two
> copies of `me.jaymar921.histoblock.api` on one server, under different class loaders, are
> two different types: the provider you see is never the one Histoblock filled in.

---

## Step 2: declare the dependency in `plugin.yml`

```yaml
name: MyPlugin
version: 1.0
main: com.example.MyPlugin
api-version: 1.16
softdepend: [Histoblock]
```

`softdepend` makes Histoblock enable before you when it is installed, and still lets your
plugin load when it is not. Use `depend` instead if your plugin is useless without it.

Folia servers also need `folia-supported: true` in your `plugin.yml`, as for any plugin.

---

## Step 3: resolve the API

```java
public final class MyPlugin extends JavaPlugin {

    private HistoblockAPI histoblock;

    @Override
    public void onEnable() {
        histoblock = HistoblockProvider.find().orElse(null);
        if (histoblock == null) {
            getLogger().info("Histoblock not found; history features are off.");
            return;
        }
        getLogger().info("Using Histoblock " + histoblock.version()
                + " (" + histoblock.edition() + ", API " + histoblock.apiVersion() + ")");
    }
}
```

| Method | Use when |
| --- | --- |
| `HistoblockProvider.find()` | your plugin works without Histoblock. Returns an `Optional` |
| `HistoblockProvider.get()` | it does not. Throws `IllegalStateException` with a clear message when Histoblock is missing |
| `HistoblockProvider.isAvailable()` | you only need a yes or no |

Resolve in `onEnable` or later, never in a static initialiser or a field initialiser: the
provider is filled in during Histoblock's own enable and emptied on its disable.

---

## Step 4: a first lookup

Who placed the block a player is looking at, read from the history:

```java
Block block = player.getTargetBlockExact(6);
if (block == null) return;

histoblock.block(Position.of(block.getLocation())).thenAccept(view -> {
    HistoryRecord placed = view.placement();
    player.sendMessage(placed == null
            ? "Natural, or placed before the records go back."
            : "Placed by " + placed.actor().name() + " at " + placed.time());
});
```

Three things to notice:

1. `block(...)` returns a `CompletableFuture`. The database is read on a Histoblock reader
   thread and the future completes there, never on the server thread.
2. `thenAccept` runs on that reader thread. Sending a chat message from it is fine. Touching
   the world (blocks, entities, inventories) is not: hop back with your scheduler first. See
   [Threading](02-core-concepts.md#threading).
3. The view is never null. A block with no history comes back with `isKnown()` false.

---

## Step 5: a first listener

```java
public final class ExplosionCounter implements Listener {

    private final AtomicLong count = new AtomicLong();

    @EventHandler
    public void onBlock(BlockRecordedEvent event) {
        if (event.getAction() == RecordAction.BLOCK_EXPLODED) count.incrementAndGet();
    }
}
```

Register it like any listener. Record events are **asynchronous**: they come from
Histoblock's writer thread after the record is saved. Hence the `AtomicLong`. See
[Events](04-events.md).

If you support Histoblock older than 0.7.0, check `apiVersion() >= 2` before registering a
listener for a typed event; those classes do not exist in older builds.

---

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| `IllegalStateException: Histoblock is not enabled` | Histoblock is missing or failed to start, or you resolved the API before your own `onEnable`. Check `softdepend` and the console |
| `NoClassDefFoundError: me/jaymar921/histoblock/api/...` | Histoblock is not installed and your plugin touched an API class anyway. Guard with `find()` and keep API types out of fields of classes that load regardless |
| `AbstractMethodError` on a view method | The server runs Histoblock older than 0.7.0. Check `apiVersion() >= 2` |
| `AbstractMethodError` on `killCredit`, `combat` or `removedPlacement` | The server runs Histoblock older than 1.1.0. Check `apiVersion() >= 3` |
| `AbstractMethodError` on `saveSpeed`, `setSaveSpeed` or `blocksRestored` | The server runs Histoblock older than 1.2.0. Check `apiVersion() >= 4` |
| `ClassCastException` between two `HistoblockAPI` types | The API jar is shaded into your plugin. Use `provided` scope |
| A view says `isKnown()` false for something that surely happened | It is older than the retention, its tracker is off, or it happened before Histoblock was installed. See [Retention](02-core-concepts.md#retention) |
| A listener never fires | `api.fire-record-events` is off in Histoblock's config.yml, or the tracker for that category is off. Check `api.capabilities()` |
| The server freezes for a moment | Something called `join()` or `get()` on a future on the server thread. Never do that |
