# 5. Records reference

[Index](README.md) | Previous: [Events](04-events.md) | Next: [Best practices](06-best-practices.md)

Every action Histoblock records, who the actor is, and the details it carries. All detail
values are strings, and a key that does not apply is left out rather than written as empty.
Read them with `detail`, `detailLong`, `detailDouble` and `detailFlag`.

Config paths such as `tracking.block.footsteps` are keys in Histoblock's own config.yml. They
tell you what the server owner can switch, and so what may be missing on a given server.

| Category | Section |
| --- | --- |
| `BLOCK` | [Block records](#block-records) |
| `ENTITY` | [Entity records](#entity-records) |
| `PLAYER` | [Player records](#player-records) |
| `ITEM` | [Item records](#item-records) |
| `DIMENSION` | [Portal and dimension records](#portal-and-dimension-records) |
| `WORLD` | [World records](#world-records) |
| `SYSTEM` | [Admin records](#admin-records) |

---

## Block records

Every block record has subject type `BLOCK`, subject key `world:x:y:z` and the block's
material as the subject name. All detail values are strings. A key that does not apply is
left out, never written as empty.

### Natural or placed

Records about a block going away (broken, burned, exploded, faded, moved) say where it
came from:

| Key | Meaning |
| --- | --- |
| `natural` | `true` if no player placed it while Histoblock was running. `false` otherwise. |
| `placed_by` | The placer's name. |
| `placed_by_uuid` | The placer's UUID. |
| `placed_ms` | When it was placed, epoch millis, to the minute. |
| `placed_as` | Only when it changed type since: placed as `OAK_SAPLING`, now `OAK_LOG`. |

Who placed a block is kept inside the chunk, not in the database, so it survives the
retention purge. `natural: true` on a block in a chunk built before Histoblock was
installed only means Histoblock never saw it placed.

Since 1.1.0, when a placed block goes away Histoblock also keeps a short note in memory for 10
minutes: who had placed it and when it was removed. Read it with
[`removedPlacement(Block)`](03-api-reference.md#removedplacementblock-api-3). A piston push or
falling sand moves the mark and leaves no note. The notes do not survive a restart.

Since 1.2.0 a mark can belong to a plugin that called
[`blocksRestored`](03-api-reference.md#blocksrestoredplugin-world-boundingbox-map-api-4). Such a
block reads `natural: false` with `placed_by` the plugin's name and no `placed_by_uuid`.

### Block state

Since 1.0.0, `BLOCK_PLACED`, `BLOCK_BROKEN`, `BLOCK_BURNED`, `BLOCK_EXPLODED`, `BLOCK_MOVED`,
`BLOCK_GREW` and `BLOCK_FADED` carry a `state` detail with the block's properties, such as
`facing=east,half=bottom,shape=straight,waterlogged=false` for a stair. It is the state the
block had when it was placed, and the state it had just before it was broken, burned, blown up
or faded. Blocks with no properties, such as stone, have no `state`, so they cost nothing extra.

The value is Bukkit's block data string without the material, so you can rebuild the block
with `Bukkit.createBlockData(material, "[" + state + "]")`. Records written by 0.7.0 have no
`state`.

### Actions

| Action | Actor | Details |
| --- | --- | --- |
| `BLOCK_PLACED` | The player; a mob for endermen; the player who lit it for `FIRE` | `replaced`, `parts` (beds, doors), `cause` (`BUCKET`, `ENTITY`, or the ignite cause), `bucket` |
| `BLOCK_BROKEN` | The player, the mob, or `BLOCK:WATER` / `BLOCK:LAVA` | `tool`, `exp`, `cause` (`BUCKET`, `ENTITY`, `FLUID`) |
| `BLOCK_BURNED` | Whoever started the fire when known, else `BLOCK:FIRE` | `fire_at`, `started_by` |
| `BLOCK_EXPLODED` | TNT igniter (or TNT placer if lit by redstone), projectile shooter, the creeper, or the bed or anchor block | `via` (entity or block type), `target` (a creeper's target) |
| `BLOCK_MOVED` | `BLOCK:PISTON`, `BLOCK:STICKY_PISTON`, or the world for falling blocks | `from`, `piston`, `cause` (`GRAVITY`) |
| `BLOCK_GREW` | The world; the player for bonemealed trees or frost walker | `was`, `from` (spread source), `blocks` and `bonemeal` for trees |
| `BLOCK_FADED` | The world | `became`, `cause` (`DECAY`) |
| `BLOCK_USED` | The player | `holding`. The click that places a block is not a use of it, even when the client sends it again on the new block |
| `BLOCK_STEPPED` | The player | `steps`, `first_ms`. The record time is the last step. |

Crops are written once, when they are ripe, not at every growth stage. Growth and fading
of the materials in `tracking.block.ignore-natural` is skipped unless a player placed the
block.

### Footsteps

Steps are counted in memory and written every `footsteps.flush-seconds` as one
`BLOCK_STEPPED` per player per block. To get a block's walkers, read its `BLOCK_STEPPED`
records: total is the sum of `steps`, unique is the distinct actors, first is the lowest
`first_ms`, last is the newest record time.

---

## Entity records

Records about a mob have subject type `ENTITY`, subject key the mob's UUID and its entity
type (`ZOMBIE`) as the subject name. Records about a player have subject type `PLAYER`,
their UUID as the key and their name as the subject name. All detail values are strings. A
key that does not apply is left out, never written as empty.

The position is where the mob or player stood when it happened.

### Followed mobs

A mob is followed once something makes it worth following:

- it spawned for a reason not in `tracking.entity.ignore-spawn-reasons` (spawn eggs,
  breeding, golems, withers, commands, plugins);
- a player hit, tamed, named, leashed or rode it.

Following is a mark in the mob's own persistent data, so it survives chunk unloads and
restarts. Only followed mobs get an `ENTITY_DESPAWNED` record. A mob that spawned naturally
and was never touched leaves no history, which is what keeps mob farms out of the database.

### Which deaths get a record

An `ENTITY_DIED` record is written for:

- every player;
- a mob a player killed or hit, since a hit starts following it;
- a mob that is followed, named, tamed, or an Epic Mob.

A farm mob that dies on its own, with no player near it, leaves nothing. So for a plugin paying
out kills, a death with no `ENTITY_DIED` record had nobody to credit. You do not have to wait
for the record to find out: [`killCredit(LivingEntity)`](03-api-reference.md#killcreditlivingentity-api-3)
(API 3) gives the same actor inside the `EntityDeathEvent`.

### Actions

| Action | Actor | Details |
| --- | --- | --- |
| `ENTITY_SPAWNED` | The breeder, the player who used the spawn egg or built the golem, `BLOCK:DISPENSER`, `SERVER` for commands and plugins, else the world | `reason`, `via` (the egg or the last golem block), `plugin` (the plugin behind a `CUSTOM` spawn), `name`, `mother`, `father` (UUIDs, breeding only), Epic Mobs keys |
| `ENTITY_DAMAGED` | Who hit: the shooter behind a projectile, whoever lit the TNT, the evoker behind the fangs; a block for cactus or magma | `cause`, `damage`, `absorbed`, `reduced`, `health` (after the hit), `via` (the projectile or TNT), `weapon` (melee by a player), `by_epic_mob` and `by_epic_name` |
| `ENTITY_DIED` | The killer; the last hitter within 5 seconds; a block; else the world | `cause`, `weapon`, `name`, `owner` (tamed pets), `drops` (item count), `exp`, the combat summary, Epic Mobs keys. Players also get `message`, `level` and `keep_inventory` |
| `ENTITY_DESPAWNED` | The world, or `SERVER` when a plugin removed it | `cause` (`DESPAWN`, `PLUGIN`, `OUT_OF_WORLD`), `plugin`, `name`, the combat summary, Epic Mobs keys |
| `ENTITY_CHANGED` | The player who did it, or the world | `change` and the keys below |
| `ENTITY_MOUNTED` | The player | `vehicle` (type), `vehicle_uuid`, `vehicle_name` |
| `ENTITY_DISMOUNTED` | The player | `vehicle`, `vehicle_uuid` |

`ENTITY_CHANGED` by `change`:

| `change` | Details |
| --- | --- |
| `TAMED` | `owner` |
| `NAMED` | `name` (the new name), `was` (the old one) |
| `LEASHED` | none |
| `UNLEASHED` | `reason` |
| `TRANSFORMED` | `reason` (`CURED`, `INFECTION`, `LIGHTNING`, `DROWNED`, ...), `into` (type), `into_uuid`. A cured zombie villager names the player who fed it the golden apple |

### The combat summary

Every hit on a player, a followed mob or a named mob is added up on the entity itself.
Death and despawn records write the totals:

| Key | Meaning |
| --- | --- |
| `first_damager`, `first_damager_uuid`, `first_damage_ms` | The first entity or block to hurt it |
| `last_damager`, `last_damager_uuid`, `last_damage_ms` | The last one |
| `hits` | Every hit, including fire, falls and poison |
| `damage_taken` | Damage that reached health. `/kill` and the void count only the health that was left |
| `absorbed` | What absorption hearts soaked up |
| `reduced` | What armor, resistance, enchantments and shields took off |
| `lived_ticks` | Ticks the mob was alive and loaded. For a player, ticks since their last death. Since 1.2.0 left out when the server reports 0, as Paper 26.x does for a mob being removed |
| `tracked_ms` | Milliseconds since Histoblock started following the mob |

A player's totals cover one life: they start over at death.

### Single hits

`tracking.entity.damage.per-hit` decides which hits also get an `ENTITY_DAMAGED` record:
`none`, `players` (the default: anything hitting a player) or `all`. Hits with no attacker,
such as falls and fire, only get one when `damage.environment` is true. Every hit counts
towards the summary either way.

### Despawns

Despawn records need a removal event that says why: Spigot and Paper 1.20.4 and newer.
On older servers despawns are not recorded; deaths and everything else are.

### Epic Mobs

With Epic Mobs installed, records about an Epic Mob add `epic_mob` (the definition key),
`epic_name`, `epic_tier` and `epic_boss`. The tag is stored on the mob the first time it is
seen, so it is still there when the mob dies after Epic Mobs has let go of it. Turn it off
with `hooks.epic-mobs: false`.

### Epic Lairs

Since 1.2.0, with Epic Lairs installed, spawn, damage, death and despawn records about a lair
mob add `lair_mob` (the mob reference, such as `lair:broodmother`) and `lair_run` (the run that
spawned it; absent for a mob spawned outside a run). A lair mob that does the hitting or killing
adds `by_lair_mob`. Lair mobs always get a death record. Turn it off with
`hooks.epic-lairs: false`.

---

## Player records

Every player record has subject type `PLAYER`, the player's UUID as the subject key and
their name as the subject name. The actor is the player unless the table says otherwise.
All detail values are strings. A key that does not apply is left out, never written as
empty.

Player deaths, damage and riding are entity records; see [Entity records](#entity-records).

### Actions

| Action | Where | Details |
| --- | --- | --- |
| `PLAYER_JOINED` | Where they logged in | `first_join`, `gamemode`, `health`, `food`, `level` |
| `PLAYER_LEFT` | Where they logged out | `session_ms`, `kicked` (the reason, or `true` without one), `gamemode`, `health`. Status `LEFT` |
| `PLAYER_CRAFTED` | The player | `item`, `amount`, `estimated` (shift-click: the count assumes the inventory had room), `station` (`WORKBENCH`, `CRAFTING`) |
| `PLAYER_SMELTED` | The furnace | `item`, `amount`, `exp`, `furnace` (`FURNACE`, `SMOKER`, `BLAST_FURNACE`) |
| `PLAYER_ENCHANTED` | The enchanting table or anvil | `item`, `enchants` (`SHARPNESS:3,UNBREAKING:2`), `cost` (levels), `station` (`ENCHANTING_TABLE`, `ANVIL`). Anvil only: `with` (the right-hand item), `renamed` (the new name, or `true` when the name was taken off). An anvil writes this when the result is taken out and lists only enchantments that are new or went up a level. A plain repair or rename is not an enchant |
| `PLAYER_ATE` | The player | `item`, `food` and `saturation` before eating |
| `PLAYER_EQUIPPED` | The player | `slot` (`HEAD`, `CHEST`, `LEGS`, `FEET`, `OFF_HAND`), `item`, `replaced` |
| `PLAYER_UNEQUIPPED` | The player | `slot`, `item` |
| `PLAYER_EFFECT_ADDED` | The player | `effect`, `level`, `seconds` (or `infinite`), `cause`, `was_level` (an upgrade), `plugin`, `ce3_enchant`, `ce3_source` |
| `PLAYER_EFFECT_REMOVED` | The player | `effect`, `level`, `cause` (`EXPIRATION`, `MILK`, `PLUGIN`, ...) |
| `PLAYER_RESURRECTED` | The player | `cause` (what would have killed them), `hand`. The actor is the last hitter within a second, else the world |
| `PLAYER_RESPAWNED` | The respawn point | `bed`, `anchor` |
| `PLAYER_LEVEL_CHANGED` | The player | `from`, `to` |
| `PLAYER_VITALS` | The player | `health`, `max_health`, `food`, `saturation`, `level`, `air` (only when running out), `effects` (`REGENERATION:2,SPEED:1`) |

### Effects

The actor of an effect record is the player for potions, food, milk and totems; `SERVER` for
commands and plugins; the last hitter for a wither skeleton or a poisonous attack; the world
for everything else.

A beacon or conduit topping up the same effect is not recorded again; only a change of level
is. Losing every effect on death is not recorded either: the death record covers it.

For effects a plugin gave, Histoblock reads the call stack to name the plugin in `plugin`.
For Custom Enchantments 3 it also names the enchantment in `ce3_enchant` (`Poison`,
`PoisonousThorns`), or the CE3 class in `ce3_source` when the effect did not come from an
enchantment. Turn this off with `tracking.player.effects.find-source: false`.

### Equipment and vitals

Armor and the offhand are compared once a second. Whatever the player wears when they join is
the starting point, not a change. The main hand is left out; it changes with every scroll of
the hotbar.

Vitals are written every `vitals.interval-seconds`. With `only-on-change` (the default) a
snapshot is skipped when nothing a player would notice changed: health to the half heart,
saturation to the whole point, effects by type and level but not by time left.

---

## Item records

Every item record has subject type `ITEM` and a UUID as the subject key. That UUID is one
of two things:

- **An item id.** Items that do not stack (tools, armor, potions, shulker boxes, enchanted
  books) carry a hidden id in their persistent data, `histoblock:item`. Every record about
  that one item uses its id, so its whole life can be read back: made, enchanted, dropped,
  put in a chest, picked up by someone else, worn out.
- **A lot id.** Items that stack are never tagged, because a tag would stop them stacking.
  Instead each batch that comes into history together is a lot: 4 planks from one craft, 23
  cobblestone from one mining run, the diamonds in one loot chest. The first record about a
  lot says how it came to be.

The subject name is the material. The actor is the player unless the table says otherwise.
All detail values are strings. A key that does not apply is left out.

### Actions

| Action | Where | Details |
| --- | --- | --- |
| `ITEM_CREATED` | The player, furnace or chest | `cause` (`CRAFTED`, `SMELTED`, `BLOCK`, `MOB`, `FISHING`, `LOOT`), `amount`. Crafted: `station`, `made_from` (the lots used, largest first), `unknown_inputs`, `estimated` (shift-click). Smelted: `furnace`, `made_from`. Block: `from` (the block key), `from_type`. Mob: `from` (the mob type). Loot: `container`, `loot_table`. Pickup lots: `pickups`, `sources`, `first_ms` |
| `ITEM_MODIFIED` | The workstation | `change` (`ENCHANTED`, `REPAIRED`, `RENAMED`, `UPGRADED`, `DISENCHANTED`, `COMBINED`, comma separated), `station` (`ENCHANTING_TABLE`, `ANVIL`, `GRINDSTONE`, `SMITHING`), `enchants`, `removed`, `renamed`, `repaired` (durability restored), `was` (the material before a smithing upgrade), `cost`, `lot` |
| `ITEM_DROPPED` | Where it landed | `amount`, `lots`, `unknown`, `lot`. `cause: DEATH` for items with an id dropped on death |
| `ITEM_PICKED_UP` | Where it lay | `amount`, `dropped_by`, `lots`, `unknown`, `lot`. `origin: unknown` when nothing was known about it |
| `ITEM_TRANSFERRED` | The container | `direction` (`IN`, `OUT`, `HOPPER`), `container`, `amount`, `lots`, `unknown`. Hopper: `from`, `from_type`, `to`, `to_type`; the actor is `BLOCK:HOPPER` |
| `ITEM_CONSUMED` | The player | `cause` (`BROKE` from wear, `CONSUMED` eaten or drunk). Status `REMOVED` |
| `ITEM_DESTROYED` | Where it was | `cause` (`DESPAWN`, `LAVA`, `FIRE`, `CONTACT`, `BLOCK_EXPLOSION`, `ENTITY_EXPLOSION`, `LIGHTNING`, `VOID`, ...), `amount`, `dropped_by`, `lots`. Actor is the world, status `REMOVED` |

`BLOCK_PLACED` records carry `lot` too: the id or lot the placed block was taken from.

#### Linking keys

- `made_from`: the lots a crafted or smelted lot was made from, comma separated.
- `lot`: the lot an item came from at the moment it got an id of its own, or that a block
  was placed from.
- `lots`: `<id>:<amount>,...` when a record covers items from more than one lot. The subject
  key is then the lot with the most items.
- `unknown`: how many of the items had no known origin. They were given a new lot, the
  subject or one of `lots`, so they can be followed from here on.

### Ledgers

Each player and each container keeps a ledger: per material, which lots their items came
from, oldest first. Picking up, crafting, smelting and taking from a chest add to it; placing,
crafting, dropping and putting into a chest take from it, oldest lot first. A take from a single
lot is exact. A take across lots is a best guess, since stacked items cannot tell them apart.

A player's ledger lives in their own persistent data and a container's in the container block,
so both survive restarts. Every 30 seconds a player's ledger is trimmed to what they really
carry, in case items left through something Histoblock does not watch.

### Pickups

Picking up items a player dropped, or a container spilled, keeps their lots. Picking up items a
block, a mob or a fishing rod produced opens a new lot. Pickups of one material from one kind
of source join that lot while they come within `pickup-merge-seconds` of each other, so mining a
vein of ore is one `ITEM_CREATED` record, written when the run ends.

Mob drops are only traced when a player made the kill. Drops from a block broken by anything
other than a player, a dispenser, or a plugin count as unknown.

### Containers

A container is counted when a player opens it and again when they close it. The difference is
what they moved, so sorting a chest for a minute is one record per material and direction.
Hoppers moving items while it is open are left out of the difference. Two players using the
same chest at once can be credited with each other's moves.

For furnaces only the input slot counts. Smelted items take their lot from what went in.

Hoppers follow only items with a known lot. Output from a farm nobody touched is never
followed or written, however many hoppers it passes through. Hopper moves are tallied per path
and material and written every `hoppers.flush-seconds`.

When a chest, barrel, hopper, furnace, dispenser or dropper is broken or blown up, the items
it spills keep their lots.

### Following an item back

`ItemView.origin()` follows an item or lot back up to `chain-depth` steps (6 by default):
stairs to the planks they were crafted from, the planks to the logs, the logs to the tree they
were cut from. Each step is the first record about one lot, and the next step is the first lot
in its `made_from`, or its `lot`. When the logs were mined out of a block someone placed, the
chain goes on through that block's `BLOCK_PLACED` record and its `lot`.

`BlockView.madeFrom()` is the same chain, starting from the lot a block was placed from, and
`stackSources` gives one chain per lot of a stack.

A chain stops at a lot whose records are older than the retention.

---

## Portal and dimension records

### Portals

Portal records have subject type `PORTAL` and the key `portal:world:x:y:z` of the portal's
lowest frame corner. For an end portal that is the frame corner at the height of the portal
blocks; an end gateway is keyed by its own block. The subject name is the portal block:
`NETHER_PORTAL`, `END_PORTAL` or `END_GATEWAY`.

A nether portal lit again in the same frame keeps its key, so its whole history reads as one
subject: lit, used, broken, lit again.

| Action | Where | Actor | Details |
| --- | --- | --- | --- |
| `PORTAL_CREATED` | The corner | Who lit it, or who arrived through it | `reason` (`FIRE`, `NETHER_PAIR` when made on the far side for someone arriving, `EYES` when the last eye of ender went in), `size` (`4x5`, width by height), `axis` (`X`, `Z`, nether only), `linked_from` (the portal the traveller came through, for `NETHER_PAIR`) |
| `PORTAL_REOPENED` | The corner | Who lit it | `reason`, `size`, `axis`, `opened` (times lit, the first included), `closed_ms` (how long it was out), `created_by` |
| `PORTAL_BROKEN` | The corner | Who broke the frame, or what blew up | `cause` (`BROKEN`, `EXPLODED`), `block` (the frame block mined, or the exploding entity type), `open_ms`, `uses`, `created_by`. Status `REMOVED` |
| `PORTAL_ENTERED` | Where they stood | The player or mob | `to_world`, `first_use` (the first player through that Histoblock saw), `uses` (players through, all time) |
| `PORTAL_EXITED` | Where they came out | The player | `from_portal` (the key of the portal they went into), `from_world`, `first_use` |

A frame block that holds nothing up, such as a corner, can be mined without the portal going
out; that writes nothing. Lighting a fire inside a frame that is not complete writes nothing.

Mobs going through portals are recorded only for mobs Histoblock already follows, unless
`tracking.dimension.portals.mobs` says `all` or `none`. Mobs never count as users.

Arriving in the End, or back at spawn from the End, is not through a portal and writes no
`PORTAL_EXITED`. The world change is still recorded below.

#### What every portal remembers

Each portal keeps a note in the persistent data of the chunk that holds its corner: who lit
it and when, whether it is open, how many times it was lit, how many players went through,
and the first and last of them. Like placed marks, the note is part of the world and is not
touched by the retention purge. A portal that was there before Histoblock gets a note the
first time someone uses or breaks it.

### Players changing worlds

These records are about the player: subject type `PLAYER`, the player's UUID as the key, and
the player as the actor. Their category is still `DIMENSION`, so they fire
`DimensionRecordedEvent` with `isPortal()` false.

| Action | Where | Details |
| --- | --- | --- |
| `DIMENSION_LEFT` | Where they left from, when known | `world`, `environment` (`NORMAL`, `NETHER`, `THE_END`, `CUSTOM`), `to`, `stay_ms` (online time since they arrived), `total_ms` (online time in that world, all time) |
| `DIMENSION_ENTERED` | Where they arrived | `world`, `environment`, `from`, `via` (`NETHER_PORTAL`, `END_PORTAL`, `END_GATEWAY`, `COMMAND`, `PLUGIN`, `RESPAWN`, ..., or `UNKNOWN`), `first_visit` |

The total time per world is kept on the player and topped up on every world change and on
quit. `timeInWorlds(Player)` reads it for an online player, the current stay included. Leaving
the server writes no `DIMENSION_LEFT`, so `total_ms` on the newest record can be behind.

---

## World records

World records have subject type `WORLD`, and the world name as both the subject key and the
subject name. They have no position.

| Action | Actor | Details |
| --- | --- | --- |
| `WORLD_TRACKING_STARTED` | `SERVER` | `environment`, `world_day` (the world's own day number), `time` (time of day in ticks). Written once per world, the first time Histoblock sees it |
| `DAY_PASSED` | The world, or `SERVER` for a command or plugin | `day` (Minecraft days since tracking started), `cause` (`NATURAL`, `SLEEP`, `COMMAND`, `PLUGIN`, or `SKIPPED` when the time jumped and the server does not say why), `passed` (only when more than one day went by at once), `world_day` |
| `WEATHER_CHANGED` | The world, or `SERVER` for a command or plugin | `to` (`RAIN`, `CLEAR`), `lasted_ms` (how long the weather before it lasted, when Histoblock saw it start), `cause` (Paper only: `NATURAL`, `COMMAND`, `SLEEP`, `PLUGIN`, ...) |
| `THUNDER_CHANGED` | As above | `to` (`THUNDER`, `CALM`), `lasted_ms`, `cause` |

Rain covers snow: which one falls depends on the biome, not the world.

### Days

The count follows the world's own clock, read every `tracking.world.check-seconds`. Sleeping
through the night counts as one day with `cause: SLEEP`, and `/time add` counts every day it
skips. Setting the time back with `/time set` takes nothing away; counting carries on from the
new time.

Only worlds with a day cycle (environment `NORMAL`) count days or record weather. The Nether
and the End still get `WORLD_TRACKING_STARTED`.

The counts are kept outside the database, so they carry on across restarts and are not
touched by the retention purge. The newest `DAY_PASSED` of a world carries its count.

---

## Admin records

Category `SYSTEM`. They only fire `HistoryRecordedEvent`.

| Action | Actor | Details |
| --- | --- | --- |
| `DATA_PURGED` | The admin who deleted records, at their position | `scope` (`all`, `category:<CATEGORY>`, `before:<epoch ms>`, `player:<uuid>` or `area:<world>:<x>:<y>:<z>:<radius>`), `deleted` (how many records went) |

Subject type `SYSTEM`, key `purge`. A purge never deletes these, so the trail of who deleted
what survives until the retention removes it. Placed marks, portal notes and item ids are
never purged.

### `BLOCKS_RESTORED`

**API 4, Histoblock 1.2.0.** Written once per
[`blocksRestored`](03-api-reference.md#blocksrestoredplugin-world-boundingbox-map-api-4) call.

| Actor | Position | Details |
| --- | --- | --- |
| `SERVER`, named after the calling plugin, no UUID | the centre of the box | `plugin`, `from` and `to` (the corner blocks, `x,y,z`, both inside), `blocks` (the box volume), `marked` (blocks now marked as the plugin's), then every detail the plugin passed |

Subject type `SYSTEM`, key `restore`. Read them back with
`RecordQuery.builder().action(RecordAction.BLOCKS_RESTORED)`, and check `details().get("plugin")`
to keep one plugin's.

`UNKNOWN` is not written by Histoblock. It is what `RecordAction.parse` returns for an action
newer than your API jar.
