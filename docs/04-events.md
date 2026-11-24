# 4. Events

[Index](README.md) | Previous: [API reference](03-api-reference.md) | Next: [Records reference](05-records.md)

Histoblock tells other plugins about every record it saves. There is one general event and,
since API 2, one typed event per category.

---

## How record events behave

All of them share these rules:

- **Asynchronous.** They are fired from Histoblock's writer thread, never the server thread.
  `isAsynchronous()` is true. Do not touch the world from a listener: copy what you need and
  schedule the rest on the right thread.
- **After the fact.** The record is already committed to the database when its event fires,
  so a lookup started from the listener already sees it. Nothing can be cancelled or changed.
- **In order.** Records fire in the order they were saved, batch by batch, one event per
  record.
- **Cheap when unused.** Before each batch Histoblock checks which events have listeners, and
  never builds an event nobody listens to.
- **Switchable.** The server owner can turn them all off with `api.fire-record-events: false`.
  A tracker that is off writes no records, and so fires no events.

Keep listeners short. A slow listener holds up the writer, and with it every record after it.

### Which to listen to

For each record Histoblock fires `HistoryRecordedEvent` first, then the typed event of the
record's category. Listen to the general event for everything, or to the typed events you
care about, not both, or you will see each record twice.

| Record category | Typed event |
| --- | --- |
| `BLOCK` | [`BlockRecordedEvent`](#blockrecordedevent) |
| `ENTITY` | [`EntityRecordedEvent`](#entityrecordedevent) |
| `PLAYER` | [`PlayerRecordedEvent`](#playerrecordedevent) |
| `ITEM` | [`ItemRecordedEvent`](#itemrecordedevent) |
| `DIMENSION` | [`DimensionRecordedEvent`](#dimensionrecordedevent) |
| `WORLD` | [`WorldRecordedEvent`](#worldrecordedevent) |
| `SYSTEM` | none; only `HistoryRecordedEvent` |

The typed events share the abstract `CategoryRecordedEvent`, which gives them `getRecord()`,
`getAction()` and `getCategory()`. Listen to a concrete event, not to the base class: Bukkit
needs a handler list, and only the concrete classes have one.

---

## `HistoryRecordedEvent`

Every record, of every category. `getRecord()`.

```java
@EventHandler
public void onRecord(HistoryRecordedEvent event) {
    HistoryRecord r = event.getRecord();
    if (r.category() == Category.SYSTEM) {
        getLogger().info("An admin purged records: " + r.details());
    }
}
```

---

## `BlockRecordedEvent`

A block was placed, broken, burned, exploded, moved, grew, faded, used or stepped on.

| Method | Returns |
| --- | --- |
| `getPosition()` | `Position` of the block |
| `getMaterial()` | the material, as the record's subject name |

```java
@EventHandler
public void onBlock(BlockRecordedEvent event) {
    HistoryRecord r = event.getRecord();
    if (r.action() == RecordAction.BLOCK_BROKEN && "false".equals(r.detail("natural"))) {
        // someone broke a block another player placed
        griefCheck.offer(new Report(r.actor(), r.detail("placed_by"), event.getPosition()));
    }
}
```

Footsteps come as `BLOCK_STEPPED` tallies, one per player per block every flush window, never
one per step.

---

## `EntityRecordedEvent`

A mob or player spawned, took damage, died, despawned, was changed (tamed, named, leashed,
transformed), mounted or dismounted.

| Method | Returns |
| --- | --- |
| `getEntityId()` | the UUID of the mob or player |
| `isPlayer()` | whether the record is about a player |

A player's damage and death are entity records, so they come here rather than to
`PlayerRecordedEvent`. Which hits get an `ENTITY_DAMAGED` record is the server owner's choice;
death and despawn records always carry the full combat summary.

Not every death fires this event. An `ENTITY_DIED` record, and so the event, comes only for a
player, a mob a player hit or killed, and a mob that is followed, named, tamed or an Epic Mob.
See [Which deaths get a record](05-records.md#which-deaths-get-a-record). A death that never
shows up here had nobody to credit.

The event fires on the writer thread a moment after the death. To pay out a kill on the spot,
call [`killCredit(LivingEntity)`](03-api-reference.md#killcreditlivingentity-api-3) (API 3)
from your own `EntityDeathEvent` listener instead of matching deaths to records later:

```java
@EventHandler
public void onDeath(EntityDeathEvent event) {
    if (event.getEntity().getKiller() != null) return; // vanilla already credits this one
    api.killCredit(event.getEntity())
       .filter(Actor::isPlayer)
       .map(actor -> Bukkit.getPlayer(actor.uuid()))
       .ifPresent(player -> reward(player, event.getEntity()));
}
```

---

## `PlayerRecordedEvent`

A player joined, left, crafted, smelted, enchanted, ate, equipped or took off armor, gained or
lost an effect, was saved by a totem, respawned, changed level, or had a vitals snapshot taken.

| Method | Returns |
| --- | --- |
| `getPlayerId()` | the player's UUID |
| `getPlayerName()` | the name, as the record's subject name |

`PLAYER_VITALS` snapshots come on a timer for every online player, by default only when
health, food or effects changed. They are the most frequent player record; filter them out
early if you do not need them.

---

## `ItemRecordedEvent`

An item or lot was created, modified, dropped, picked up, transferred, consumed or destroyed.

| Method | Returns |
| --- | --- |
| `getItemId()` | the item id or lot id |
| `getMaterial()` | the material, as the record's subject name |

---

## `DimensionRecordedEvent`

A portal was created, broken, reopened, entered or exited, or a player arrived in or left a
world.

| Method | Returns |
| --- | --- |
| `isPortal()` | true for a portal record (the key is the portal key), false for a player changing worlds (the key is the player's UUID) |

---

## `WorldRecordedEvent`

Tracking started in a world, the weather or thunder changed, or a Minecraft day passed.

| Method | Returns |
| --- | --- |
| `getWorldName()` | the world |

```java
@EventHandler
public void onWorld(WorldRecordedEvent event) {
    if (event.getAction() == RecordAction.DAY_PASSED) {
        long day = event.getRecord().detailLong("day", 0);
        if (day % 100 == 0) announce(event.getWorldName() + " has seen " + day + " days");
    }
}
```

`announce` here has to be thread-safe, or schedule onto the server thread itself.
