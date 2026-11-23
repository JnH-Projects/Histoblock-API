# 3. API reference

[Index](README.md) | Previous: [Core concepts](02-core-concepts.md) | Next: [Events](04-events.md)

Every public type, grouped by package. Methods marked **API 2** need Histoblock 0.7.0 or newer.

---

## `HistoblockProvider`

| Method | Returns | Notes |
| --- | --- | --- |
| `get()` | `HistoblockAPI` | throws `IllegalStateException` when Histoblock is not enabled |
| `find()` | `Optional<HistoblockAPI>` | empty when it is not |
| `isAvailable()` | `boolean` | |

`register` and `unregister` are for Histoblock itself. Calling them replaces the API for every
plugin on the server.

---

## `HistoblockAPI`

### About the plugin

| Method | Returns | Notes |
| --- | --- | --- |
| `version()` | `String` | the plugin version without the edition suffix, e.g. `0.7.0` |
| `apiVersion()` | `int` | the contract the running plugin implements; compare with `HistoblockAPI.API_VERSION`, the one you compiled against |
| `edition()` | `Edition` | `FREE` or `PRO` |
| `capabilities()` | `Set<Capability>` | what is on right now, unmodifiable |
| `has(Capability)` | `boolean` | shorthand for `capabilities().contains(...)` |

### `records(RecordQuery)`

`CompletableFuture<List<HistoryRecord>>`: the records matching the query, newest first unless
the query says `oldestFirst()`. Unmodifiable. A database error completes the future
exceptionally; the call itself never throws for that.

### `count(RecordQuery)`

`CompletableFuture<Long>`: how many records match. Ignores the query's limit and offset.

### `block(Position)` (API 2)

`CompletableFuture<BlockView>` for one block position. See [`BlockView`](#blockview).

### `entity(UUID)` (API 2)

`CompletableFuture<EntityView>` for one mob, or a player's entity side. Works for entities that
are gone. See [`EntityView`](#entityview).

### `player(UUID)` (API 2)

`CompletableFuture<PlayerView>` for one player, online or not. See [`PlayerView`](#playerview).

### `item(UUID)` (API 2)

`CompletableFuture<ItemView>` for one item id or lot id. See [`ItemView`](#itemview).

### `portal(String)` (API 2)

`CompletableFuture<PortalView>` for one portal key, `portal:world:x:y:z`. See
[`PortalView`](#portalview).

### `itemId(ItemStack)` (API 2)

`Optional<UUID>`: the hidden id of an item that does not stack. Empty for null, air, a
stackable item, or an item Histoblock has not given an id yet. Any thread.

### `stackSources(Player, ItemStack)` (API 2)

`CompletableFuture<List<ItemSource>>`: where a stack of stackable items most likely came
from, read against the player's ledger, oldest lot first, up to eight lots, each with its
origin chain. Empty when nothing is known or the item does not stack. Call on the player's
thread; the origins are read on a reader thread.

The stack does not have to be in the player's inventory, but the answer only makes sense for
items the player is holding: it is their ledger that is read.

### `placement(Block)` (API 2)

`Optional<Placement>`: who placed the block, from the mark Histoblock keeps in the chunk. Never
ages out. Empty for a natural block or one placed before tracking. Call on the block's thread.

### `portalKey(Block)` (API 2)

`Optional<String>`: the key of the nether or end portal the block is part of, or frames. Call
on the block's thread.

### `timeInWorlds(Player)` (API 2)

`Map<String, Duration>`: an online player's time in each world, all time, including the stay
they are in now. Most time first. Unmodifiable. Call on the player's thread.

---

## `RecordQuery`

Build with `RecordQuery.builder()`. Every filter you leave out matches everything; filters you
set are combined with AND.

| Builder method | Filters to | Notes |
| --- | --- | --- |
| `category(Category)` | records in this category | call more than once for several |
| `action(RecordAction)` | records of this action | call more than once for several |
| `subject(SubjectType, String)` | one subject by key | the fastest filter there is |
| `subjectType(SubjectType)` | one kind of subject | |
| `actor(UUID)` | records caused by this player or entity | |
| `world(String)` | one world | |
| `near(Position, int)` | a cube of `radius` blocks on every axis around a block | radius 0 to 256; sets the world too |
| `since(Instant)`, `until(Instant)` | a time window, both ends included | |
| `limit(int)` | at most this many | default 100, at most `RecordQuery.MAX_LIMIT` (5000) |
| `offset(int)` | skip this many | for paging |
| `oldestFirst()` | oldest records first | **API 2**. An older plugin ignores it and returns newest first |

```java
// the last 20 blocks Steve broke in the nether this week
RecordQuery q = RecordQuery.builder()
        .actor(steve.getUniqueId())
        .action(RecordAction.BLOCK_BROKEN)
        .world("world_nether")
        .since(Instant.now().minus(Duration.ofDays(7)))
        .limit(20)
        .build();
```

The getters on a built query (`categories()`, `actions()`, `limit()`, ...) return what was set.

---

## `HistoryRecord`

| Method | Returns | Notes |
| --- | --- | --- |
| `id()` | `long` | the database id; increases with time on one server |
| `time()` | `Instant` | when it happened |
| `position()` | `Position` or null | null when the action has no place |
| `action()` | `RecordAction` | `UNKNOWN` for an action newer than your API jar |
| `category()` | `Category` | shorthand for `action().category()` |
| `subjectType()` | `SubjectType` | |
| `subjectKey()` | `String` | see [Subjects and keys](02-core-concepts.md#subjects-and-keys) |
| `subjectName()` | `String` | `OAK_STAIRS`, `ZOMBIE`, `Steve` |
| `actor()` | `Actor` | who or what caused it |
| `status()` | `RecordStatus` | the subject's state when it was written |
| `details()` | `Map<String, String>` | unmodifiable, never null; see [Records reference](05-records.md) |
| `detail(String)` | `String` or null | **API 2** |
| `detailLong(String, long)` | `long` | **API 2**. The fallback when absent or not a number |
| `detailDouble(String, double)` | `double` | **API 2** |
| `detailFlag(String)` | `boolean` | **API 2**. True only for `true` |

### `Actor`

`type()` (`PLAYER`, `ENTITY`, `BLOCK`, `WORLD`, `SERVER`, `UNKNOWN`), `uuid()` (players and
entities only), `name()` (a player name, an entity type, a block type), `isPlayer()`.

### `Position`

An immutable block position in a named world, safe to keep after the world unloads.
`world()`, `x()`, `y()`, `z()`, `toLocation()` (null if the world is not loaded),
`Position.of(Location)`, and in API 2 `toKey()` and `Position.fromKey(String)`.

### `RecordStatus`

| Value | Meaning |
| --- | --- |
| `LIVE` | the subject exists and is tracked |
| `DESPAWNED` | a mob despawned or was removed; tracking stopped, history stays |
| `LEFT` | a player left the server |
| `DIED` | an entity or player died |
| `REMOVED` | a block or item no longer exists |
| `UNTRACKED` | nothing on record; also what a view reports for an unknown subject |

---

## Views (API 2)

All views live in `me.jaymar921.histoblock.api.view`. They are snapshots: read once, then never
changed, so they are safe to keep and to pass between threads.

### `SubjectView`

What every view has.

| Method | Returns | Notes |
| --- | --- | --- |
| `subjectType()` | `SubjectType` | for an entity lookup, `PLAYER` when the UUID turns out to be a player |
| `subjectKey()` | `String` | |
| `name()` | `String` | the newest name on record, or the key |
| `isKnown()` | `boolean` | anything on record at all |
| `status()` | `RecordStatus` | of the newest record; `UNTRACKED` when there is none |
| `first()` | `HistoryRecord` or null | the oldest still on record |
| `last()` | `HistoryRecord` or null | the newest, of any action |
| `recent()` | `List<HistoryRecord>` | newest first, at most `SubjectView.RECENT_LIMIT` (20). Leaves out footstep tallies and vitals snapshots |

### `BlockView`

| Method | Returns | Notes |
| --- | --- | --- |
| `position()` | `Position` | |
| `placement()` | `HistoryRecord` or null | the `BLOCK_PLACED` of the block standing there now. Null when the newest placement was broken, burned or blown up since, or there is none on record |
| `isPlaced()` | `boolean` | `placement() != null` |
| `footsteps()` | `Footsteps` | never null |
| `madeFrom()` | `List<HistoryRecord>` | the origin chain of the lot the block was placed from; empty when unknown |

`Footsteps`: `total()`, `unique()`, `firstWalker()`, `firstTime()`, `lastWalker()`,
`lastTime()`, `isEmpty()`.

### `EntityView`

| Method | Returns | Notes |
| --- | --- | --- |
| `uuid()` | `UUID` | |
| `spawned()` | `HistoryRecord` or null | the newest `ENTITY_SPAWNED` |
| `ended()` | `HistoryRecord` or null | the `ENTITY_DIED` or `ENTITY_DESPAWNED` that ended its newest life; null while it lives |
| `combat()` | `CombatSummary` | who hurt it in its newest life; never null |

A player's newest life starts at their newest spawn or respawn, so a player who died and
respawned has `ended()` null and an empty combat summary until they are hurt again.

`CombatSummary`: `hits()`, `damageTaken()`, `absorbed()`, `first()` and `last()` (each a
`CombatSummary.Damager` with `name()`, `uuid()` and `time()`), `isEmpty()`, and
`isComplete()`:

- **true**: the life ended and the totals come from the death or despawn record. They count
  every hit, fire and falls included.
- **false**: it is still alive and the totals were added up from single-hit records. Which
  hits get a record is the server owner's choice (by default only hits on players), so the
  numbers can be short.

### `PlayerView`

Everything in `EntityView`, plus:

| Method | Returns | Notes |
| --- | --- | --- |
| `firstJoined()` | `HistoryRecord` or null | the oldest `PLAYER_JOINED` on record |
| `lastJoined()` | `HistoryRecord` or null | |
| `lastLeft()` | `HistoryRecord` or null | |
| `lastRespawned()` | `HistoryRecord` or null | |
| `deaths()` | `long` | deaths on record |

Time per world is kept on the player, not in records: see
[`timeInWorlds(Player)`](#timeinworldsplayer-api-2).

### `ItemView`

| Method | Returns | Notes |
| --- | --- | --- |
| `id()` | `UUID` | the item id or lot id |
| `created()` | `HistoryRecord` or null | the `ITEM_CREATED`, if its beginning is still on record |
| `origin()` | `List<HistoryRecord>` | starting with itself, then each lot it was made from, as deep as the server's `chain-depth` (6 by default). A mined block goes back through its `BLOCK_PLACED` |

`ItemSource`, from `stackSources`: `lot()`, `amount()`, `origin()`.

### `PortalView`

| Method | Returns | Notes |
| --- | --- | --- |
| `corner()` | `Position` or null | the lowest frame corner, from the key |
| `created()` | `HistoryRecord` or null | the oldest `PORTAL_CREATED`: who first lit it |
| `broken()` | `HistoryRecord` or null | the newest `PORTAL_BROKEN` |
| `isOpen()` | `boolean` | lit and not broken since |
| `timesLit()` | `int` | creations and reopenings on record |
| `uses()` | `long` | players through it, all time where Histoblock kept count, else player trips on record |
| `firstUse()`, `lastUse()` | `HistoryRecord` or null | the oldest and newest `PORTAL_ENTERED` by a player. Mobs going through never count as uses |

### `Placement`

From `placement(Block)`: `placer()` (null when a block or the world placed it), `placerName()`,
`material()` (what it was placed as, which can differ from what stands there now, such as a
sapling that grew), `time()`.

---

## Enums

| Enum | Values |
| --- | --- |
| `Category` | `BLOCK`, `ENTITY`, `PLAYER`, `ITEM`, `DIMENSION`, `WORLD`, `SYSTEM` |
| `SubjectType` | `BLOCK`, `ENTITY`, `PLAYER`, `ITEM`, `PORTAL`, `WORLD`, `SYSTEM` |
| `ActorType` | `PLAYER`, `ENTITY`, `BLOCK`, `WORLD`, `SERVER`, `UNKNOWN` |
| `Capability` | `BLOCK_TRACKING`, `ENTITY_TRACKING`, `PLAYER_TRACKING`, `ITEM_TRACKING`, `DIMENSION_TRACKING`, `MOVEMENT_PATHS`, `PLAYBACK` |
| `Edition` | `FREE`, `PRO` |
| `RecordAction` | see [Records reference](05-records.md). `RecordAction.parse(String)` never throws |
