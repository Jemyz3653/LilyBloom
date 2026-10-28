# Changelog

## 1.3.0+26.3 — 2026-10-28

Big lily pads in three sizes: Medium 2×2 and Large 3×3 join the Giant 4×4 lily pad, which is now called the Huge Lily Pad.

- **Medium Lily Pad (2×2)**: craft it from 4 lily pads in a square. Shears cut 1 Water Lily off its open flower.
- **Large Lily Pad (3×3)**: craft it from 4 lily pads in a cross around a bone meal. Shears cut 2 Water Lilies.
- **Huge Lily Pad (4×4)**: same block and recipe as before (8 lily pads around a bone meal), only the name changed. Existing pads in your worlds stay as they are.
- All three work the same way: place on water, a flower in four colours in the middle (bud → half-open → bloom with bone meal, or by itself in daylight), shears and dispensers cut it, breaking any part breaks the whole leaf and drops it once.
- All three generate in swamps and mangrove swamps (new chunks).
- 32x and 64x textures for the new pads are built in and switch on by themselves with packs like Faithful, like the rest of the mod.

Update the mod on the server and on every client: 1.3.0 adds new blocks. Worlds and configs from 1.2.x load as they are.

## 1.2.1+26.3 — 2026-10-20

Hotfix: Blooming Lily Pad items showed the missing (purple and black) texture.

- In 1.2.0 the item drew the lily pad from the block texture atlas and the flower from the item atlas. Minecraft 26.3 refuses item models that mix two atlases, so all four Blooming Lily Pad items lost their texture. The flower of the item is now in the block atlas too, like the vanilla lily pad.
- Blocks, worlds and configs are not affected. No new blocks: 1.2.0 and 1.2.1 can play together, but the fix only shows on clients with 1.2.1.

## 1.2.0+26.3 — 2026-10-20

Giant lily pads, HD textures for Faithful-style packs, and fixes.

**Giant Lily Pad (4×4 blocks)**
- A round leaf with an upturned rim, after the Amazon water lily. Craft it from 8 lily pads around a bone meal, or find it in swamps and mangrove swamps (new chunks).
- Place it on water like a lily pad: it needs 4×4 free water and grows forward from the block you look at.
- It gets a big flower in the same four colours: bone meal starts a bud of a random colour and moves it through bud → half-open → bloom; a Water Lily on the leaf gives an open flower of that colour; in daylight under the open sky it buds and opens on its own.
- Shears (by hand or from a dispenser) cut 2 Water Lilies off an open flower.
- Breaking any part breaks the whole leaf and drops it once (plus a Water Lily if the flower was open). Boats break it, like lily pads. Frogs and mobs walk on it.

**HD textures**
- 32x and 64x textures in the style of Faithful are built into the mod, for all lily pads, flowers and items. They switch on by themselves when your resource pack makes the vanilla lily pad 32x or 64x (Faithful 32x, Faithful 64x and similar packs). In the resource pack list they show up as "Lily Bloom 32x" and "Lily Bloom 64x".
- Client option `hdTextures` in `config/lilybloom.json`: `auto` (default), `32x`, `64x` or `off`.

**Fixes**
- The leaf of the blooming lily pad (block and item) now uses the lily pad texture of your resource pack, so it matches normal lily pads with Faithful or any other pack.
- Water Lilies burn like other small flowers.

Update the mod on the server and on every client: 1.2.0 adds a new block. Worlds and configs from 1.1.0 load as they are.

## 1.1.0+26.3 — 2026-10-14

Colors, bloom stages and slower natural blooming.

- **Four colors:** pink, white, yellow and blue. Each color is its own block and item (like vanilla tulips): Blooming Lily Pad, Water Lily and Potted Water Lily in every color.
- **Bloom stages:** bud → half-open bud → bloom. A bud grows by itself in daylight under the open sky, about 20 minutes per stage near a player.
- **Bone meal** on a lily pad starts a bud of a random color (white 35%, pink 30%, yellow 20%, blue 15%). Each bone meal moves it one stage further.
- **To pick a color,** right-click a plain lily pad with a Water Lily of that color: it blooms right away.
- **Natural blooming is much rarer:** a bud now appears about 6 times less often than a bloom did in 1.0.x, and it still needs time to open. From plain lily pad to full bloom takes about 7 times longer on average.
- Shears only work on a fully open lily pad and give a Water Lily of its color. A bud or half-open bud that breaks drops a plain lily pad.
- Water Lilies craft into pink, white, yellow or light blue dye.
- Swamps and mangrove swamps generate all four colors, in all stages.
- Pink keeps the 1.0.x IDs, so pink lily pads and flowers already placed in your world stay as they are (fully open).
- Config: `bloomChance` is replaced by `budChance` (default 150) and `growChance` (default 20). The old file is updated automatically.

**Update the mod on the server and on every client:** 1.1.0 adds new blocks, so 1.0.x clients cannot join a 1.1.0 server.

## 1.0.1+26.3 — 2026-10-13

Fix: bone meal on lily pads did nothing when Carpet Extra was installed.

- Bone meal handling no longer adds methods to the vanilla lily pad, so it no longer clashes with Carpet Extra's `betterBonemeal` mixin.
- With Carpet Extra and `betterBonemeal` on, bone meal on a plain lily pad makes it bloom, and on a blooming lily pad it spreads lily pads around as before.
- Green particles when a lily pad blooms from bone meal.

## 1.0.0+26.3 — 2026-10-13

First release for Minecraft 26.3 (Fabric).

- Blooming Lily Pad: a separate block. Use bone meal on a lily pad (by hand or from a dispenser) to make it bloom.
- Lily pads bloom on their own in daylight under open sky (max 3 blooming pads per 7×3×7 area), so ponds in already generated worlds get flowers too.
- Blooming lily pads generate in swamps and mangrove swamps in new chunks.
- Water Lily flower: shear a blooming lily pad (by hand or from a dispenser). Plant it on the ground or in a flower pot, put it back on a lily pad, craft it into pink dye.
- Config file: `config/lilybloom.json`.
- Translations: English, Russian, Ukrainian.
