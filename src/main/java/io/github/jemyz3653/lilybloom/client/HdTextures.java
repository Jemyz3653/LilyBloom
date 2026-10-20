package io.github.jemyz3653.lilybloom.client;

import com.google.common.collect.ImmutableList;
import io.github.jemyz3653.lilybloom.LilyBloom;
import io.github.jemyz3653.lilybloom.LilyBloomConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.IoSupplier;

/**
 * Встроенные паки текстур 32x и 64x (в стиле Faithful) и их автоматическое включение.
 *
 * <p>Режим {@code hdTextures} в config/lilybloom.json:
 * <ul>
 *   <li>{@code auto} — смотрим, какой ресурспак сейчас даёт текстуру обычной кувшинки
 *       ({@code minecraft:textures/block/lily_pad.png}). Если она 32x или 64x (Faithful и другие HD-паки),
 *       сразу над этим паком ставится наш пак той же чёткости; иначе наши HD-паки выключаются;</li>
 *   <li>{@code 32x} / {@code 64x} — всегда этот пак;</li>
 *   <li>{@code off} — мод сам ничего не включает и не выключает, паки можно включить вручную в списке ресурспаков.</li>
 * </ul>
 * Вызывается из PackRepositoryMixin каждый раз, когда игра собирает список включённых ресурспаков
 * (запуск, выход из экрана ресурспаков, F3+T).
 */
public final class HdTextures {
    public static final Identifier PACK_32 = LilyBloom.id("hd_32x");
    public static final Identifier PACK_64 = LilyBloom.id("hd_64x");

    private static final Identifier LILY_PAD_TEXTURE = Identifier.withDefaultNamespace("textures/block/lily_pad.png");

    private HdTextures() {
    }

    public static List<Pack> adjustSelection(final PackRepository repository, final List<Pack> selected) {
        Pack pack32 = repository.getPack(PACK_32.toString());
        Pack pack64 = repository.getPack(PACK_64.toString());
        if (pack32 == null || pack64 == null) {
            return selected; // это не список ресурспаков клиента (например, датапаки мира)
        }
        String mode = LilyBloomConfig.get().hdTextures;
        if (LilyBloomConfig.HD_OFF.equals(mode)) {
            return selected;
        }

        List<Pack> packs = new ArrayList<>(selected);
        packs.remove(pack32);
        packs.remove(pack64);
        Pack wanted = null;
        int index = packs.size();
        switch (mode) {
            case LilyBloomConfig.HD_32 -> wanted = pack32;
            case LilyBloomConfig.HD_64 -> wanted = pack64;
            default -> {
                // верхний пак, в котором есть текстура кувшинки, — именно её игрок и видит
                for (int i = packs.size() - 1; i >= 0; i--) {
                    int width = lilyPadWidth(packs.get(i));
                    if (width > 0) {
                        if (width >= 64) {
                            wanted = pack64;
                        } else if (width >= 32) {
                            wanted = pack32;
                        }
                        index = i + 1;
                        break;
                    }
                }
            }
        }
        if (wanted != null) {
            packs.add(index, wanted);
        }
        if (packs.equals(selected)) {
            return selected;
        }
        LilyBloom.LOGGER.info("HD textures ({}): {}", mode, wanted == null ? "off" : wanted.getId());
        return ImmutableList.copyOf(packs);
    }

    /** Ширина текстуры кувшинки в паке или -1, если пак её не меняет. */
    static int lilyPadWidth(final Pack pack) {
        List<PackResources> opened = List.of();
        try {
            opened = pack.open().toList();
            int width = -1;
            for (PackResources resources : opened) {
                IoSupplier<InputStream> supplier = resources.getResource(PackType.CLIENT_RESOURCES, LILY_PAD_TEXTURE);
                if (supplier != null) {
                    try (InputStream in = supplier.get()) {
                        width = pngWidth(in);
                    }
                }
            }
            return width;
        } catch (IOException | RuntimeException e) {
            LilyBloom.LOGGER.debug("Could not read the lily pad texture of pack {}: {}", pack.getId(), e.toString());
            return -1;
        } finally {
            for (PackResources resources : opened) {
                try {
                    resources.close();
                } catch (RuntimeException ignored) {
                    // пак уже закрыт или не умеет закрываться повторно
                }
            }
        }
    }

    /** Ширина PNG из заголовка IHDR (байты 16–19), без чтения всей картинки. */
    static int pngWidth(final InputStream in) throws IOException {
        byte[] header = in.readNBytes(24);
        if (header.length < 24 || (header[0] & 0xFF) != 0x89 || header[1] != 'P' || header[2] != 'N' || header[3] != 'G'
                || header[12] != 'I' || header[13] != 'H' || header[14] != 'D' || header[15] != 'R') {
            return -1;
        }
        return ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16) | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
    }
}
