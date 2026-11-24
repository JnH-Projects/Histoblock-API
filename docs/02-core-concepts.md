# 2. Core concepts

[Index](README.md) | Previous: [Getting started](01-getting-started.md) | Next: [API reference](03-api-reference.md)

---

## Records

Everything Histoblock knows is a list of records. One record is one thing that happened, and
it never changes after it is written. A `HistoryRecord` answers the same questions every
time:

| Question | Method | Example |
| --- | --- | --- |
| When | `time()` | `2026-11-23T08:14:02Z` |
| Where | `position()` | `world 10, 64, -3`, or null for things with no place |
| Who | `actor()` | `PLAYER:Steve`, `ENTITY:CREEPER`, `BLOCK:HOPPER`, `WORLD:world` |
| What | `action()` on `subjectKey()` | `BLOCK_PLACED` on `world:10:64:-3` |
| State | `status()` | `LIVE`, `REMOVED`, `DIED`, `LEFT`, ... |

Anything specific to the action, such as the damage of a hit or the enchantments added, is in
`details()`: a map of strings. [Records reference](05-records.md) lists every action's keys.

An action belongs to one `Category`: `BLOCK`, `ENTITY`, `PLAYER`, `ITEM`, `DIMENSION`,
`WORLD`, or `SYSTEM` for admin actions such as a purge.

---

## Subjects and keys

A record is about one subject, and every record about the same subject shares its key. The
key is how you ask for the rest of that subject's history.

| Subject type | Key | Example |
| --- | --- | --- |
| `BLOCK` | `world:x:y:z` | `world:10:64:-3` |
| `ENTITY` | the mob's UUID | `5f2c...` |
| `PLAYER` | the player's UUID | `069a...` |
| `ITEM` | an item id or a lot id | `b41e...` |
| `PORTAL` | `portal:world:x:y:z` of its lowest frame corner | `portal:world:0:64:0` |
| `WORLD` | the world name | `world_nether` |

`Position.toKey()` and `Position.fromKey(String)` convert between positions and block or
portal keys. World names may contain colons; the last three parts are always the
coordinates.

### Item ids and lots

Items come in two kinds, and the API treats them differently:

- **Items that do not stack** (tools, armor, potions, shulker boxes) get a hidden id the first
  time they change hands. Every record about that one item uses the id as its key, so its
  whole life can be read back. `itemId(ItemStack)` reads the id from a stack.
- **Items that stack** are never tagged, because a tag would stop them stacking with their
  untagged twins. Instead each batch that came into history together is a *lot*: four planks
  from one craft, a vein of ore from one mining run. Every player and container keeps a ledger
  of the lots its items came from. `stackSources(Player, ItemStack)` matches a stack against
  that ledger: exact for one source, a best guess for several.

Records link lots to each other (`made_from`, `lot`), which is how an origin chain goes from
stairs back to planks, logs, and the tree they were cut from.

---

## Three ways in

| | Returns | Thread | Use for |
| --- | --- | --- | --- |
| **Queries**: `records`, `count` | a list of records | completes on a reader thread | lists and timelines across many subjects |
| **Views**: `block`, `entity`, `player`, `item`, `portal` | one view of one subject | completes on a reader thread | everything about one thing, already worked out |
| **Live lookups**: `itemId`, `stackSources`, `placement`, `portalKey`, `timeInWorlds`, and in API 3 `killCredit`, `combat(LivingEntity)`, `removedPlacement` | a value | runs on your thread | state Histoblock keeps on the item, chunk, entity or player, not in records |

A view is a handful of indexed queries on one key, run in one go. It costs about what an
in-game inspect costs, and it does the folding for you: the placement that is still standing,
the combat summary of the newest life, the footstep totals.

Live lookups read what is not in the database. Placed marks and portal notes are kept in the
chunk, item ids on the item, ledgers and world times on the player, the combat ledger on the
entity. They are not subject to the retention and they never age out. The one exception is
`removedPlacement`: those notes are kept in memory for 10 minutes and do not survive a restart.

---

## Retention

Records are deleted when they are older than the server's retention: **7 days** by default.
The server owner can change it, up to keeping everything, and servers that keep a long
history usually move to MySQL or MariaDB. Admins can also purge records by hand. There is no
way to read the retention through the API; plan for any amount of history, including none.

So a view only covers what is still on record:

- `BlockView.placement()` is null for a block placed a month ago on a 7-day server. Use
  `placement(Block)`, which reads the chunk mark, to answer that.
- `ItemView.origin()` stops at the first lot older than the retention.
- `PlayerView.firstJoined()` is the oldest join on record, not the first join ever.

Nothing in the API throws because history is missing. A subject with no records comes back as
a view with `isKnown()` false, null records and empty lists.

---

## Threading

Histoblock never touches the database on the server thread, and neither should you.

**Queries and views** complete on a Histoblock reader thread. In a `thenAccept`:

- Reading the view, logging, and sending chat messages are fine.
- Touching the world (blocks, entities, inventories, scoreboards) is not. Hop back with your
  own scheduler: `Bukkit.getScheduler().runTask(plugin, ...)` on Spigot and Paper, the
  region or entity scheduler on Folia.
- **Never** call `join()` or `get()` on the server thread. It blocks the tick until the
  database answers.

**Live lookups** run on the thread you call them from, and each one says which thread that has
to be:

| Lookup | Call on |
| --- | --- |
| `itemId(ItemStack)` | any thread; it only reads the stack you pass |
| `placement(Block)`, `removedPlacement(Block)`, `portalKey(Block)` | the block's thread: the main thread, or its region thread on Folia |
| `killCredit(LivingEntity)`, `combat(LivingEntity)` | the entity's thread: the main thread, or its region thread on Folia |
| `stackSources(Player, ItemStack)`, `timeInWorlds(Player)` | the player's thread: the main thread, or their region thread on Folia |

A command handler or an event listener for that block or player is already on the right
thread.

**Events** are fired on Histoblock's writer thread. See [Events](04-events.md).

---

## Editions and capabilities

Histoblock comes as **Free** (SpigotMC) and **Pro** (jhprojects.dev). Every query, view, live
lookup and event works the same in both, from the same API jar. `edition()` says which one is
running.

`capabilities()` says what is on right now. A capability is missing when the server owner
turned its tracker off in config.yml, or, for `MOVEMENT_PATHS` and `PLAYBACK`, on the Free
edition. A view for a category whose tracker is off still answers from older records, but
nothing new is coming in.

```java
if (!api.has(Capability.ITEM_TRACKING)) {
    getLogger().info("Item tracking is off on this server; provenance will be empty.");
}
```

Capabilities can change at runtime when an admin reloads Histoblock. Check when you need the
answer, not once at startup.
