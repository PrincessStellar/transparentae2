# Transparent AE2 concept

Transparent AE2 adds a visual layer on top of AE2 transfers. AE2 remains authoritative and still
moves resources instantly. The addon only observes completed transfers and sends a separate visual
event to nearby clients.

## Prototype

The prototype logs successful item transfers at two points:

- `NetworkStorage.insert` and `NetworkStorage.extract` cover imports, exports, terminals, storage
  buses, crafting results, and other operations that cross the ME network-storage boundary.
- `PatternProviderLogic.pushPattern` covers crafting inputs sent internally from a crafting CPU to a
  pattern provider.

Simulation calls, rejected transfers, fluids, and amounts that did not move are ignored. Logging can
be disabled with `logItemTransfers` in `transparentae2-common.toml`.

Example:

```text
[AE2 TRANSFER/IMPORT] 16x minecraft:iron_ingot | ImportBusPart external @ minecraft:overworld 4, 64, 9 -> ME network
[AE2 TRANSFER/EXPORT] 8x minecraft:iron_ingot | ME network -> ExportBusPart external @ minecraft:overworld 20, 64, 9
[AE2 TRANSFER/CRAFTING] 1x minecraft:iron_ingot | crafting CPU -> pattern provider @ minecraft:overworld 12, 64, 9
```

## Visual implementation

1. Convert observed operations into a small server-side event containing the item key, amount,
   direction, grid, and endpoint node.
2. Resolve the endpoint node's route to a controller after AE2 pathing has finished.
3. Convert the route into world-space cable waypoints. Virtual connections such as P2P tunnels and
   quantum bridges become explicit discontinuities rather than straight lines through unloaded
   space.
4. Send the immutable route and item representation only to players tracking its chunks.
5. Animate a fake item on the client. The transfer event never participates in storage, crafting,
   energy, or security logic.

AE2 does retain a controller-route parent for each grid node/connection as part of channel
calculation. That route is internal implementation state rather than public API, so the visual phase
should access it behind a small compatibility adapter. If that adapter cannot resolve a route, the
event should be dropped without affecting the real transfer.

To keep busy networks readable and inexpensive, equivalent events should be coalesced per
grid/endpoint/item over a short window and capped per client. Route snapshots can be cached until
AE2 reports a pathing change.
