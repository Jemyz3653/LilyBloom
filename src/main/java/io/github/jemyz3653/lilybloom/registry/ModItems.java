package io.github.jemyz3653.lilybloom.registry;

import java.util.function.BiFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public final class ModItems {
    /** Ставится на воду, как обычная кувшинка. */
    public static final Item BLOOMING_LILY_PAD = registerBlockItem(
            ModBlocks.BLOOMING_LILY_PAD, PlaceOnWaterBlockItem::new,
            new Item.Properties().compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));

    /** Ставится на землю и сажается в цветочный горшок. */
    public static final Item LILY_FLOWER = registerBlockItem(
            ModBlocks.LILY_FLOWER, BlockItem::new,
            new Item.Properties().compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));

    private ModItems() {
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
