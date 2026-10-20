package io.github.jemyz3653.lilybloom.registry;

import io.github.jemyz3653.lilybloom.LilyColor;
import io.github.jemyz3653.lilybloom.item.GiantLilyPadItem;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import org.jspecify.annotations.Nullable;

public final class ModItems {
    /** Цветущая кувшинка каждого цвета — ставится на воду, как обычная кувшинка (сразу распустившейся). */
    public static final Map<LilyColor, Item> BLOOMING_LILY_PADS;
    /** Цветок кувшинки каждого цвета — ставится на землю и сажается в цветочный горшок. */
    public static final Map<LilyColor, Item> LILY_FLOWERS;

    static {
        Map<LilyColor, Item> blooming = new EnumMap<>(LilyColor.class);
        Map<LilyColor, Item> flowers = new EnumMap<>(LilyColor.class);
        for (LilyColor color : LilyColor.values()) {
            blooming.put(color, registerBlockItem(ModBlocks.BLOOMING_LILY_PADS.get(color), PlaceOnWaterBlockItem::new,
                    new Item.Properties().compostable(ContextIntProviders.COMPOSTABLE_MEDIUM)));
            flowers.put(color, registerBlockItem(ModBlocks.LILY_FLOWERS.get(color), BlockItem::new,
                    new Item.Properties().compostable(ContextIntProviders.COMPOSTABLE_MEDIUM)));
        }
        BLOOMING_LILY_PADS = Collections.unmodifiableMap(blooming);
        LILY_FLOWERS = Collections.unmodifiableMap(flowers);
    }

    /** Розовые — для кода, написанного под 1.0.x. */
    public static final Item BLOOMING_LILY_PAD = BLOOMING_LILY_PADS.get(LilyColor.PINK);
    public static final Item LILY_FLOWER = LILY_FLOWERS.get(LilyColor.PINK);

    /** Крупная кувшинка: ставится на воду и занимает 4×4 блока. */
    public static final Item GIANT_LILY_PAD = registerBlockItem(ModBlocks.GIANT_LILY_PAD, GiantLilyPadItem::new,
            new Item.Properties().compostable(ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH));

    private ModItems() {
    }

    /** Цвет цветка кувшинки или null, если это не цветок кувшинки. */
    public static @Nullable LilyColor flowerColor(final ItemStack stack) {
        for (Map.Entry<LilyColor, Item> entry : LILY_FLOWERS.entrySet()) {
            if (stack.is(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Так же, как Items.registerBlock в ванилле: id предмета = id блока, перевод берётся у блока. */
    private static Item registerBlockItem(final Block block, final BiFunction<Block, Item.Properties, Item> factory, final Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        Item item = factory.apply(block, properties.setId(key).useBlockDescriptionPrefix());
        if (item instanceof BlockItem blockItem) {
            blockItem.registerBlocks(Item.BY_BLOCK, item);
        }
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void init() {
        // загрузка класса регистрирует предметы
    }
}
