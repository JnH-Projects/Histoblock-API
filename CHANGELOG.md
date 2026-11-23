# Changelog

Changes to the API only. The plugin's own changelog is on its download page.

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
