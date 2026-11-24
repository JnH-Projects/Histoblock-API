# Changelog

Changes to the API only. The plugin's own changelog is on its download page.

## 1.1.0 (API 3)

Three live lookups, asked for by Custom Enchantments 3. Each answers on the entity's or
block's own thread, straight away, with no database read. Code written for API 2 keeps working.

- `killCredit(LivingEntity)` returns the `Actor` an `ENTITY_DIED` record would name: the
  vanilla killer, the entity behind the last damage cause, a block, or the last hitter in the
  combat ledger within 5 seconds. Meant for `EntityDeathEvent`, and the same at every listener
  priority, a player's death included.
- `combat(LivingEntity)` returns the `CombatSummary` of an entity that is still alive, read off
  the entity itself. Always complete.
- `removedPlacement(Block)` returns a `RemovedPlacement` when a player placed block was taken
  away from that spot in the last 10 minutes: the old `Placement` and `removedAt()`. Memory
  only. Pistons and falling blocks leave no note.
- `HistoblockAPI.API_VERSION` is 3.
- `CombatSummary` docs list the live ledger as a third, complete source.

Docs: [Which deaths get a record](docs/05-records.md#which-deaths-get-a-record) spells out
that `ENTITY_DIED` is only written for players and for mobs a player hit or that are
followed, named, tamed or Epic. Two new examples, `KillCreditPlugin` and `PlaceCounterPlugin`.

## 1.0.0 (API 2)

No API change: the 1.0.0 jar is the 0.7.0 contract with the new version number. Records gained
one detail: block records carry `state`, the block's properties such as
`facing=east,half=bottom`. See [Block state](docs/05-records.md#block-state).

## 0.7.0 (API 2)

The first public release of the API. Earlier plugin builds shipped API 1 privately: the
provider, `records`, `count` and `HistoryRecordedEvent`. Everything below is new in API 2.

### Views

- `block(Position)` returns a `BlockView`: the placement of the block standing there now, its
  footsteps as a `Footsteps` summary, what it was made from, and its recent records.
- `entity(UUID)` returns an `EntityView`: how its newest life began and ended, and a
  `CombatSummary` with hits, damage, absorption and the first and last attacker.
- `player(UUID)` returns a `PlayerView`: the entity side plus first and last join, last leave,
  last respawn and deaths on record.
- `item(UUID)` returns an `ItemView`: how it was created and its origin chain.
- `portal(String)` returns a `PortalView`: who lit it, whether it is open, times lit, uses, and
  the first and last user.
- Every view shares `SubjectView`: key, name, status, first and last record, recent records. A
  lookup never completes with null.

### Live lookups

- `itemId(ItemStack)`: the hidden id of an item that does not stack.
- `stackSources(Player, ItemStack)`: where a stack most likely came from, as `ItemSource`s.
- `placement(Block)`: the placed mark kept in the chunk, as a `Placement`. It never ages out.
- `portalKey(Block)`: the key of the portal a block belongs to or frames.
- `timeInWorlds(Player)`: an online player's time per world, all time.

### Events

- `BlockRecordedEvent`, `EntityRecordedEvent`, `PlayerRecordedEvent`, `ItemRecordedEvent`,
  `DimensionRecordedEvent` and `WorldRecordedEvent`, sharing `CategoryRecordedEvent`. Fired
  after `HistoryRecordedEvent`, on the writer thread, once the record is saved.

### Smaller additions

- `HistoryRecord.detail`, `detailLong`, `detailDouble` and `detailFlag` read details without
  parsing by hand.
- `RecordQuery.Builder.oldestFirst()`.
- `Position.fromKey` and `Position.toKey` for the block and portal keys records use.
