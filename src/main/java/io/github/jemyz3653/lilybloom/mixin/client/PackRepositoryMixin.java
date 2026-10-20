package io.github.jemyz3653.lilybloom.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.jemyz3653.lilybloom.client.HdTextures;
import java.util.List;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Каждый раз, когда игра собирает список включённых ресурспаков (запуск, экран ресурспаков, F3+T),
 * включает или выключает встроенные HD-паки Lily Bloom под чёткость текущего ресурспака.
 */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    @ModifyReturnValue(method = "rebuildSelected", at = @At("RETURN"))
    private List<Pack> lilybloom$hdTextures(final List<Pack> selected) {
        return HdTextures.adjustSelection((PackRepository) (Object) this, selected);
    }
}
