# Transparent AE2

Renders moving items inside AE2 networks. Works with see-through cables (like normal me cables) and also 
adds transparent versions of the smart / dense cables.

Items are shown moving through AE2 glass cables and specially treated smart or dense cables. The
animation follows the route that AE2 assignd between a machine and the controller. The real item
transfer still happens instantly. The moving item is only a visual effect.

## Features

- Shows imported, exported, inserted, extracted, and crafting-input items moving through an ME
  network.
- Existing AE2 transfer / working logic is untouched.
- Adds a reusable Cable Transparency Applicator Item. Use it on a smart or dense cable to show items through
  that cable. Use it again to remove the treatment.
- Adds ready-made transparent smart, dense smart, and dense covered cable items.

## What it does not do

- It does not change how AE2 stores, moves, crafts, or secures items.
- It does not turn the animation into a real item entity. It cannot be picked up or interact with
  the world.
- It does not slow down item transfers to match the animation.
- It does not currently show fluid transfers.
- It does not show routes on controllerless, booting, or conflicting AE2 networks.
- It does not show a route when every cable on that route is an opaque cable.

## Performance impact

The server observes completed AE2 item transfers and calculates their controller routes once per
tick. Similar transfers are grouped together before the route is sent to nearby players. The client
keeps a limited number of recent animations and only renders nearby items.

Most small installations should see little impact. Very large or very busy ME networks can require
more route calculations and network traffic, especially when many different items move at once. If
needed, the server can disable transfer-path calculation completely. The calculations can be disabled
on the server level, or the client can disable the rendering. Both can be toggled independently.

## Configuration

Server configuration:

- `enableTransferPaths` enables or disables route calculation and animation packets.

Client configuration:

- `renderTransferItems` enables or disables moving item animations.
- `itemMovementSpeed` changes the animation speed.
- `maxTransfers` limits how many recent animations the client keeps.

## Credits

Cable Transparency Applicator Texture: From MalcolmRiley's [unused textures](https://github.com/malcolmriley/unused-textures)
