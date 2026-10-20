package io.github.jemyz3653.lilybloom.client;

import io.github.jemyz3653.lilybloom.LilyBloom;
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
    }
}
