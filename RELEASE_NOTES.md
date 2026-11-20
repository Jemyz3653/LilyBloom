# Lily Bloom 2.0.0 — Living Ponds

For Minecraft 26.3 with Fabric. Install the same version on every client and the server; single-player only needs the client installation. Requires Java 25, Fabric Loader 0.19.5 or later and Fabric API 0.163.0+26.3 or later.

- **Nocturnal lilies:** apply Lunar Pollen to a flowering lily pad. Its flower closes during the day and opens at night. Craft four Lunar Pollen from an amethyst shard and a Blue Water Lily. Honeycomb restores daytime growth.
- **Glowing flowers:** apply a glow ink sac to any flowering lily pad. Flowers emit light level 10 at full bloom and 2 as buds. An ink sac removes the coating. Emissive petal models and optional small particles work without shaders.
- **Floating lights:** place an undyed candle, lantern or soul lantern on an empty 2×2, 3×3 or 4×4 pad. Sneak and use an empty hand to remove it. A pad holds either a flower or a light.
- **Four leaf shapes:** round, oval, deeply notched and curled. Sneak and use shears to cycle shapes, on small pads and all three large sizes.
- **Whole-plant transplantation:** craft a Lily Transplanter from one copper ingot and one stick, diagonally. Use it on any part of a pad to move the whole plant while preserving its colour, growth stage, traits and installed light. The tool lasts 256 uses.
- **Breeding:** use bone meal on a fully open lily with another fully open lily of a different colour within six blocks along the water surface. Receive one small offspring pad. It inherits one parent's colour, leaf shape and night habit; each offspring has a 1-in-8 mutation roll for speckled or striped petals. Existing patterns can also be inherited. Glow coating must be applied separately.
- **Trait preservation:** special small pads keep their traits after shearing and can grow another flower. Small-pad drops preserve growth and traits; breaking a large pad drops its leaf and open flower separately. Use the transplanter to preserve the complete large plant.
- New English and Russian tooltips identify leaf shape, rare petals, nocturnal growth and glow coating.
- Fixed disappearing large-leaf sections at 64× by correcting cutout mipmap metadata.
- New shapes and petal patterns support the built-in 16×, 32× and 64× textures.

Existing block IDs, flower colours, recipes, potted flowers and swamp generation remain supported. Back up your world before updating. Update all clients and the server together; older clients cannot join a 2.0.0 server.
