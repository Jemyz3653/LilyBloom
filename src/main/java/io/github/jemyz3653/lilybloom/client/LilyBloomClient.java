package io.github.jemyz3653.lilybloom.client;

import io.github.jemyz3653.lilybloom.LilyBloom;
import io.github.jemyz3653.lilybloom.LilyTraits;
import io.github.jemyz3653.lilybloom.registry.ModItems;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;

/**
 * Клиентская часть: встроенные паки HD-текстур (resourcepacks/hd_32x и hd_64x внутри jar).
 * Включаются сами вместе с ресурспаками 32x/64x — см. {@link HdTextures}.
 * Подкраска листа кувшинки — в BlockColorsMixin.
 */
public final class LilyBloomClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModContainer mod = FabricLoader.getInstance().getModContainer(LilyBloom.MOD_ID).orElseThrow();
        ResourceLoader.registerBuiltinPack(HdTextures.PACK_32, mod, Component.translatable("pack.lilybloom.hd_32x"), PackActivationType.NORMAL);
        ResourceLoader.registerBuiltinPack(HdTextures.PACK_64, mod, Component.translatable("pack.lilybloom.hd_64x"), PackActivationType.NORMAL);
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (stack.is(ModItems.TRANSPLANTER)) {
                lines.add(Component.translatable("tooltip.lilybloom.transplanter").withStyle(ChatFormatting.GRAY));
            } else if (stack.is(ModItems.LUNAR_POLLEN)) {
                lines.add(Component.translatable("tooltip.lilybloom.lunar_pollen").withStyle(ChatFormatting.GRAY));
            } else if (stack.getItem() instanceof BlockItem item && LilyTraits.pad(item.getBlock().defaultBlockState())) {
                var state = item.getBlock().defaultBlockState();
                var properties = stack.get(DataComponents.BLOCK_STATE);
                if (properties != null) state = properties.apply(state);
                lines.add(Component.translatable("tooltip.lilybloom.shape", Component.translatable("trait.lilybloom.leaf." + state.getValue(LilyTraits.LEAF).getSerializedName())).withStyle(ChatFormatting.GRAY));
                if (state.getValue(LilyTraits.PATTERN) != LilyTraits.Pattern.PLAIN)
                    lines.add(Component.translatable("tooltip.lilybloom.pattern", Component.translatable("trait.lilybloom.pattern." + state.getValue(LilyTraits.PATTERN).getSerializedName())).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (state.getValue(LilyTraits.NIGHT)) lines.add(Component.translatable("tooltip.lilybloom.nocturnal").withStyle(ChatFormatting.BLUE));
                if (state.getValue(LilyTraits.GLOW)) lines.add(Component.translatable("tooltip.lilybloom.glowing").withStyle(ChatFormatting.AQUA));
                lines.add(Component.translatable("tooltip.lilybloom.reshape").withStyle(ChatFormatting.DARK_GRAY));
            }
        });
    }
}
