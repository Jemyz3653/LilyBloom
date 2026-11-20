# Verification report — Lily Bloom 2.0.0

Tested on 20 November 2026 with Minecraft Java 26.3, Java 25.0.5, Fabric Loader 0.19.5 and Fabric API 0.163.0+26.3.

## Dedicated server

The regression test mod completed 1331 assertions. Tests cover nocturnal clock boundaries and /time transitions; all four leaf shapes on 2×2, 3×3 and 4×4 pads; glow and night trait application/removal and item costs; adventure-mode edit denial; decoration installation/removal and flower exclusion; decorated block drops; shearing and regrowth of special plants; complete transplantation from a far corner, durability, item components and real large-pad placement; breeding and mutation distribution. A fixed-seed sample produced 119 patterned offspring out of 1024. World save/reopen retained the test plant and its traits. Breaking a large pad dropped it once.

## Client

The actual client opened an integrated world and rendered a pond scene at night and noon, then reloaded the built-in 32× and 64× packs. All tested models and textures resolved without missing-model/texture placeholders. The test enumerated 268609 block states across the nine loaded content mods, then 84680 Lily Bloom states at each HD setting. Each pass also resolved 1509 item models/variants, including transplanted patterned plants. These enumeration totals are model checks, separate from the server assertion count.

Visual inspection found disappearing sections of large leaves in the old strict-cutout mipmap mode at 64×. Leaf metadata now uses cutout coverage; the final 64× scene shows complete leaves. Four final screenshots are included in the release archive.

## Joint launch

Both the dedicated server and client loaded Lily Bloom 2.0.0 together with BloomDepth 1.1.0, ColoredM 1.3.0, CivicCraft 1.1.0, ArtiCrystals 1.0.1, Museum Mannequins 1.0.0, Dye Workshop 1.0.0, Door Designer 1.0.0 and Illustrated Books 1.0.0. No mod startup, resource, recipe or loot parsing errors were observed.

## Scope and limits

Rendering used Mesa llvmpipe software OpenGL. This is functional rendering verification, not an FPS benchmark or a test of every GPU, shader or third-party mod. Sound playback was unavailable in the test environment; authentication and Realms network warnings were unrelated to the mod. Automatic HD detection was retained from the existing implementation; the new assets were verified with explicit 32×/64× selection. Existing IDs/defaults were inspected for old-world compatibility; this does not guarantee every existing world or modpack.

The release JAR was compiled with javac against the official runtime and Fabric APIs using tools/build_manual.py; a local Gradle build was not claimed. Test entrypoints live in src/testmod and are excluded from the release JAR. Cached Minecraft/Fabric runtime JARs and game assets are not included in source delivery.
