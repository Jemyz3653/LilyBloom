# Changelog

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
