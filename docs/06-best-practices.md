# 6. Best practices

[Index](README.md) | Previous: [Records reference](05-records.md)

---

## The hard rules

1. **Never block the server thread on a future.** No `join()`, no `get()`, no busy wait. Use
   `thenAccept`, `thenCompose` and `whenComplete`, and hop back with your scheduler when you
   need the world.
2. **Never touch the world from a reader or writer thread.** Futures complete on Histoblock's
   reader threads and events fire on its writer thread. Chat messages and logging are fine
   there; blocks, entities and inventories are not.
3. **Call live lookups on the owning thread.** `placement`, `removedPlacement` and `portalKey`
   on the block's thread, `killCredit` and `combat` on the entity's, `stackSources` and
   `timeInWorlds` on the player's. On Folia that is the region thread, not the global one.
4. **Never shade the API jar.** `provided` or `compileOnly`, always.
5. **Never assume history exists.** Retention, purges, trackers switched off and worlds older
   than the install all leave gaps. Every view handles it with `isKnown()`, null records and
   empty lists; your code has to as well.

---

## Versioning

`HistoblockAPI.API_VERSION` is the contract your jar describes; `api.apiVersion()` is the one
the running plugin implements.

- New methods, views, events and actions are added in minor plugin releases, and
  `apiVersion()` goes up when they are. Nothing is removed or changes meaning within a major
  version.
- Code written for API 2 runs on any later API.
- If you support older Histoblock builds, check `api.apiVersion() >= 2` before calling a view
  or a live lookup, and before registering a listener for a typed event. On an older plugin a
  view method throws `AbstractMethodError`, and a typed event class does not exist at all.
- The same goes for API 3 (Histoblock 1.1.0): check `api.apiVersion() >= 3` before calling
  `killCredit`, `combat(LivingEntity)` or `removedPlacement`, and keep your old path for
  servers that have not updated yet.

| Plugin | `apiVersion()` | Added |
| --- | --- | --- |
| 0.7.0, 1.0.0 | 2 | views, live lookups, typed events |
| 1.1.0 | 3 | `killCredit`, `combat(LivingEntity)`, `removedPlacement` |
- `RecordAction` grows. A record written by a newer plugin and read through an older API jar
  comes back as `RecordAction.UNKNOWN`. Give every `switch` on it a `default` branch.
- Detail keys grow too. Read the keys you know and ignore the rest; never fail on an
  unexpected key or a missing one.

---

## Performance

Histoblock is built so that tracking costs the server almost nothing: trackers only build a
record and queue it. Reads are where a consumer can hurt a server, so:

- **Prefer views and subject queries.** Anything filtered by subject key uses the main index
  and answers in milliseconds even on a large database. A view is a few such queries.
- **Bound every query.** Always set a time window with `since`, or a subject, or an area with
  `near`. A query with only a category and no time window scans more than it needs to on a
  server that keeps months of history.
- **Page large reads.** `limit` is capped at 5000. Use `offset`, or move `until` back, rather
  than asking for everything at once.
- **Do not poll.** If you want to know when something happens, listen to its event. Querying
  every few seconds for new records is what events exist to avoid.
- **Cache what does not change.** A record never changes after it is written. A view is a
  snapshot; keep it as long as the answer it gave is good enough for you.
- **Keep listeners cheap.** They run on the writer thread. Filter on `getAction()` first and
  hand anything slow to your own executor.

---

## Reading details

Every detail is a string, and a key that does not apply is left out rather than written as
empty. Use the helpers rather than parsing by hand:

```java
long steps    = r.detailLong("steps", 1);
double damage = r.detailDouble("damage", 0);
boolean first = r.detailFlag("first_join");
String cause  = r.detail("cause");          // null when absent
```

Lists inside a detail are comma separated, such as `SHARPNESS:3,UNBREAKING:2` or the lot ids in
`made_from`.

---

## Ship checklist

- [ ] `softdepend` or `depend` on `Histoblock` in `plugin.yml`
- [ ] the API jar is `provided` or `compileOnly`
- [ ] the API is resolved in `onEnable`, with `find()` if Histoblock is optional
- [ ] `apiVersion()` is checked if you support Histoblock older than the API you compiled against
- [ ] no `join()` or `get()` anywhere a server thread can reach
- [ ] world changes after a lookup go through your scheduler, Folia's if you support it
- [ ] listeners are thread-safe and short
- [ ] every `switch` on `RecordAction` has a `default`
- [ ] an empty view, a null record and a missing detail are all handled
