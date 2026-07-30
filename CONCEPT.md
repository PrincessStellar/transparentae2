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

Each successfully resolved route is sent to players within 128 blocks of any route point. The
payload contains the transfer kind, complete component-bearing display stack, amount, and the
ordered dimension-aware block positions for each route leg.

The client keeps at most 32 received transfers. It renders the stack moving along the route at four
blocks per second. The item follows a line raised by half a block from the normal block center,
placing it at `Y + 1.0`, so it remains distinguishable from cables and machines. Crafting routes and
routes split by virtual connections are traversed segment by segment; the item teleports across the
unrenderable gap rather than drawing a false connection through the world.

The optional line debugger can be enabled with `debugRenderTransferPaths` in
`transparentae2-client.toml` and is disabled by default. It draws only colored route lines for five
seconds; the former block-outline cubes have been removed. Imports are green, exports orange,
generic inserts yellow, extracts cyan, and crafting paths magenta. Only the part of a
cross-dimensional route in the player's current dimension is rendered.

## Visual implementation

1. Convert observed operations into a small server-side event containing the item key, amount,
   direction, grid, and endpoint node.
2. Resolve the endpoint node's route to a controller after AE2 pathing has finished.
3. Convert the route into world-space cable waypoints. Virtual connections such as P2P tunnels and
   quantum bridges become explicit discontinuities rather than straight lines through unloaded
   space.
4. Send the immutable route and complete display stack only to nearby players.
5. Animate a fake item on the client, with the optional line debugger consuming the same route.
   The transfer event never participates in storage, crafting, energy, or security logic.

AE2 does retain a controller-route parent for each grid node/connection as part of channel
calculation. That route is internal implementation state rather than public API, so the prototype
accesses it through the isolated `TransferPathResolver` compatibility adapter. If that adapter
cannot resolve a route, it reports the reason without affecting the real transfer.

To keep busy networks readable and inexpensive, equivalent events should be coalesced per
grid/endpoint/item over a short window and capped per client. Route snapshots can be cached until
AE2 reports a pathing change.

## Transparent cable treatment

The Cable Treatment Applicator is a reusable tool crafted from a brush and AE2 quartz glass.
Using it on the center cable of a cable-bus toggles a cosmetic treatment. Using it again removes
the treatment.

Treatment can only be applied to smart, dense covered, and dense smart cables in every color.
Glass and normal covered cables are already see-through and are not treatable. Treated cables keep
AE2's original dimensions, connection geometry, and base textures, but omit its additional
emissive channel-indicator passes. The smart and dense-smart base textures already contain
transparent pixels beneath those passes, leaving the indicator strip visibly cut out.
Dense-covered cable has no separate indicator pass, so treated segments use AE2's matching
dense-smart base texture without its overlay to expose the same cutout.

Transparent ME Smart Cable, Transparent ME Dense Smart Cable, and Transparent ME Dense Covered
Cable are also available as dedicated Fluix-colored placement items. They carry the treatment
component by default, so they render with the cutout immediately after placement. AE2's color
applicator can recolor them after placement while retaining the treatment state.

Treatment is stored on the cable part and synchronized as part of its normal update data. It is
also copied to the dropped cable item's data component when dismantled, so it survives breaking,
placing, and recoloring. The change is visual only: connections, channels, rendering bounds,
collision, selection, and all ME network behavior continue to use AE2's original cable geometry
and logic.
