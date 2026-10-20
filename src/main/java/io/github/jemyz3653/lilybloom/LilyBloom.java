package io.github.jemyz3653.lilybloom;

import io.github.jemyz3653.lilybloom.registry.ModBlocks;
import io.github.jemyz3653.lilybloom.registry.ModItems;
import io.github.jemyz3653.lilybloom.world.GiantLilyPadFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lily Bloom — цветущие кувшинки четырёх цветов со стадиями цветения и цветок кувшинки.
 *
 * <p>Один мод для сервера и клиента: блоки и предметы регистрируются одинаково с обеих сторон
 * (поэтому мод нужен и там, и там), а вся игровая логика — костная мука, ножницы, самостоятельное
 * цветение и генерация мира — выполняется на сервере. Клиентского кода нет: модели, текстуры и
 * перевод лежат в ресурсах.
 */
public final class LilyBloom implements ModInitializer {
    public static final String MOD_ID = "lilybloom";
    public static final Logger LOGGER = LoggerFactory.getLogger("LilyBloom");

    private static final ResourceKey<CreativeModeTab> NATURAL_BLOCKS =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("natural_blocks"));

    /** Ванильный набор кувшинок: по нему находим все биомы, где кувшинки генерируются (болото, мангровое болото, биомы других модов). */
    private static final ResourceKey<PlacedFeature> VANILLA_LILY_PATCH =
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("patch_waterlily"));

    public static final ResourceKey<PlacedFeature> BLOOMING_LILY_PATCH =
            ResourceKey.create(Registries.PLACED_FEATURE, id("patch_blooming_lily_pad"));
    public static final ResourceKey<PlacedFeature> GIANT_LILY_PAD =
            ResourceKey.create(Registries.PLACED_FEATURE, id("giant_lily_pad"));

    public static Identifier id(final String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LilyBloomConfig.load();
        ModBlocks.init();
        ModItems.init();
        Registry.register(BuiltInRegistries.FEATURE_TYPE, id("giant_lily_pad"), GiantLilyPadFeature.CODEC);

        // цветы кувшинки горят, как остальные маленькие цветы (у ванильных 60 / 100); сами кувшинки, как и ванильная, не горят
        for (Block flower : ModBlocks.LILY_FLOWERS.values()) {
            FlammableBlockRegistry.getDefaultInstance().add(flower, 60, 100);
        }

        CreativeModeTabEvents.modifyOutputEvent(NATURAL_BLOCKS).register(output -> {
            output.insertAfter(Items.LILY_PAD, ModItems.BLOOMING_LILY_PADS.values().toArray(new ItemLike[0]));
            output.insertAfter(ModItems.BLOOMING_LILY_PADS.get(LilyColor.BLUE), ModItems.GIANT_LILY_PAD);
            output.insertAfter(Items.LILY_OF_THE_VALLEY, ModItems.LILY_FLOWERS.values().toArray(new ItemLike[0]));
        });

        if (LilyBloomConfig.get().worldGeneration) {
            BiomeModifications.addFeature(
                    context -> context.hasPlacedFeature(VANILLA_LILY_PATCH),
                    GenerationStep.Decoration.VEGETAL_DECORATION,
                    BLOOMING_LILY_PATCH);
            BiomeModifications.addFeature(
                    context -> context.hasPlacedFeature(VANILLA_LILY_PATCH),
                    GenerationStep.Decoration.VEGETAL_DECORATION,
                    GIANT_LILY_PAD);
        }

        LilyBloomConfig config = LilyBloomConfig.get();
        LOGGER.info("Lily Bloom loaded: natural blooming={} (bud 1/{}, stage 1/{}, max {} nearby, only day={}), world generation={}",
                config.naturalBlooming, config.budChance, config.growChance, config.maxBloomingNearby, config.onlyDuringDay, config.worldGeneration);
    }
}
