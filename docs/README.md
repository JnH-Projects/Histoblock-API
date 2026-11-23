# Documentation

The Histoblock developer API, in reading order.

| | Page | Covers |
| --- | --- | --- |
| 1 | [Getting started](01-getting-started.md) | requirements, the dependency, `plugin.yml`, resolving the API, a first lookup, troubleshooting |
| 2 | [Core concepts](02-core-concepts.md) | records, subjects and keys, views, retention, threading, editions |
| 3 | [API reference](03-api-reference.md) | every method on `HistoblockProvider`, `HistoblockAPI`, `RecordQuery`, `HistoryRecord` and the views |
| 4 | [Events](04-events.md) | `HistoryRecordedEvent` and the six typed events, when they fire, what they cost |
| 5 | [Records reference](05-records.md) | every action, its actor and the details it carries |
| 6 | [Best practices](06-best-practices.md) | the hard rules, versioning, performance, a ship checklist |

Then read the [examples](../examples/README.md): six complete plugins, smallest first.

## Quick answers

| I want to | Read |
| --- | --- |
| add the jar to my build | [Getting started, step 1](01-getting-started.md#step-1-add-the-dependency) |
| know who placed a block | [`placement(Block)`](03-api-reference.md#placementblock-api-2) and [`BlockView`](03-api-reference.md#blockview) |
| know who killed a mob, or hit it first | [`EntityView.combat()`](03-api-reference.md#entityview) |
| follow an item back to where it was made | [`ItemView.origin()`](03-api-reference.md#itemview) |
| know where a stack of cobblestone came from | [`stackSources`](03-api-reference.md#stacksourcesplayer-itemstack-api-2) |
| list everything a player broke this week | [`records(RecordQuery)`](03-api-reference.md#recordsrecordquery) |
| react to every explosion | [`BlockRecordedEvent`](04-events.md#blockrecordedevent) |
| know what is in a record's details | [Records reference](05-records.md) |
| know why a view is empty | [Retention](02-core-concepts.md#retention) |
| avoid freezing the server | [Threading](02-core-concepts.md#threading) |
| avoid breaking on the next release | [Versioning](06-best-practices.md#versioning) |
