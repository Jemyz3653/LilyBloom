HD textures for Faithful-style packs, and fixes.

- **32x and 64x textures** in the style of Faithful, built into the mod: flowers, buds, Water Lilies and items. They switch on by themselves when your resource pack makes the vanilla lily pad 32x or 64x (Faithful 32x, Faithful 64x and similar packs), and switch off when you go back to 16x. In the resource pack list they show up as "Lily Bloom 32x" and "Lily Bloom 64x".
- New client option `hdTextures` in `config/lilybloom.json`: `auto` (default), `32x`, `64x` or `off` (the mod does not touch the packs, you can enable them by hand).
- Fix: the leaf of the blooming lily pad and of its item now uses the lily pad texture of your resource pack. Before, it was always the default 16x leaf, so blooming lily pads stood out next to normal ones with Faithful or any other pack.
- Fix: Water Lilies burn like other small flowers.

No new blocks: worlds and configs from 1.1.0 work as they are, and a 1.1.0 client can still join a 1.2.0 server. Update both to get all fixes.
