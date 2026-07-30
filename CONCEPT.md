# Transparent AE2 concept

Transparent AE2 adds a visual layer on top of AE2 transfers. AE2 remains authoritative and still
moves resources instantly. The addon only observes completed transfers and sends a separate visual
event to nearby clients.

## Prototype

The prototype logs successful item transfers at two points:

- `NetworkStorage.insert` and `NetworkStorage.extract` cover imports, exports, terminals, storage
  buses, crafting results, and other operations that cross the ME network-storage boundary.
- The crafting CPU's `ICraftingProvider.pushPattern` call covers crafting inputs sent internally
  from a crafting CPU to any crafting provider.

Simulation calls, rejected transfers, fluids, and amounts that did not move are ignored. Logging can
be disabled with `logItemTransfers` in `transparentae2-common.toml`. Channel path logging can be
controlled separately with `logTransferPaths`.

Example:

```text
[AE2 TRANSFER/IMPORT] 16x minecraft:iron_ingot | ImportBusPart external @ minecraft:overworld 4, 64, 9 -> ME network
[AE2 TRANSFER/EXPORT] 8x minecraft:iron_ingot | ME network -> ExportBusPart external @ minecraft:overworld 20, 64, 9
[AE2 TRANSFER/CRAFTING] 1x minecraft:iron_ingot | crafting CPU -> pattern provider @ minecraft:overworld 12, 64, 9
```

Each transfer is followed by an `AE2 PATH` line. It walks the controller parent route assigned by
AE2's channel calculation in the same direction the visual item would travel:

```text
[AE2 PATH/EXPORT] 8x minecraft:iron_ingot | ControllerBlockEntity @ minecraft:overworld 0, 64, 0 --[used=3, cable]--> CablePart @ minecraft:overworld 1, 64, 0 --[used=1, cable]--> ExportBusPart @ minecraft:overworld 2, 64, 0
```

`used` is AE2's finalized number of channels crossing that connection. `cable` is an adjacent
in-world connection; `internal/virtual` identifies links without a world direction, including
same-block part connections and potentially P2P or quantum links. Controllerless, conflicting, or
currently booting networks produce an explicit `unavailable` reason.

Crafting logs show the CPU-to-controller leg and the controller-to-provider leg separately because
each endpoint has its own assigned controller route.

## Debug rendering

When `renderTransferPaths` is enabled, each successfully resolved route is sent to players within
128 blocks of any route point. The payload contains the transfer kind, item identifier, amount, and
the ordered dimension-aware block positions for each route leg.

The client keeps at most 32 received paths for five seconds. It outlines every route block and draws
colored lines between consecutive block centers. Lines are raised by half a block from the normal
block center, placing them at `Y + 1.0`, so they remain distinguishable from cables and machines.
Imports are green, exports orange, generic inserts yellow, extracts cyan, and crafting paths magenta.
Only the part of a cross-dimensional route in the player's current dimension is rendered.

## Visual implementation

1. Convert observed operations into a small server-side event containing the item key, amount,
   direction, grid, and endpoint node.
2. Resolve the endpoint node's route to a controller after AE2 pathing has finished.
3. Convert the route into world-space cable waypoints. Virtual connections such as P2P tunnels and
   quantum bridges become explicit discontinuities rather than straight lines through unloaded
   space.
4. Send the immutable route and item representation only to nearby players.
5. Animate a fake item on the client. The debug renderer implemented by the prototype is the first
   consumer of these route payloads. The transfer event never participates in storage, crafting,
   energy, or security logic.

AE2 does retain a controller-route parent for each grid node/connection as part of channel
calculation. That route is internal implementation state rather than public API, so the prototype
accesses it through the isolated `TransferPathResolver` compatibility adapter. If that adapter
cannot resolve a route, it reports the reason without affecting the real transfer.

To keep busy networks readable and inexpensive, equivalent events should be coalesced per
grid/endpoint/item over a short window and capped per client. Route snapshots can be cached until
AE2 reports a pathing change.
