package io.github.jemyz3653.lilybloom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * config/lilybloom.json. Читается при запуске. Всё, кроме {@code hdTextures}, имеет значение только на сервере
 * (в одиночной игре — на встроенном сервере); {@code hdTextures} — настройка клиента.
 */
public final class LilyBloomConfig {
    public static final String HD_AUTO = "auto";
    public static final String HD_32 = "32x";
    public static final String HD_64 = "64x";
    public static final String HD_OFF = "off";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static LilyBloomConfig instance = new LilyBloomConfig();

    /** Обычные кувшинки сами пускают бутоны, а бутоны сами распускаются (под открытым небом). Работает и в уже сгенерированных чанках. */
    public boolean naturalBlooming = true;
    /**
     * Шанс, что обычная кувшинка пустит бутон, за один случайный тик: 1 из N.
     * При randomTickSpeed = 3 конкретный блок получает случайный тик в среднем раз в ~68 секунд,
     * значит при 150 бутон появляется в среднем примерно за 3 часа игрового времени рядом с игроком.
     * (В 1.0.x было bloomChance = 25 сразу до цветка; старое поле больше не читается.)
     */
    public int budChance = 150;
    /** Шанс перейти на следующую стадию (бутон → полураскрытый → цветок) за случайный тик: 1 из N. При 20 — около 20 минут на стадию. */
    public int growChance = 20;
    /** Если в области 7×3×7 вокруг уже столько кувшинок с бутоном или цветком, новый бутон сам не появится. 0 — без ограничения. Костной муке не мешает. */
    public int maxBloomingNearby = 3;
    /** Бутоны появляются и распускаются сами только днём. */
    public boolean onlyDuringDay = true;
    /** Генерация цветущих кувшинок в новых чанках там же, где генерируются обычные (болота, мангровые болота). */
    public boolean worldGeneration = true;
    /**
     * Клиент: текстуры 32x/64x в стиле Faithful. "auto" — сами включаются, если ресурспак делает обычную кувшинку
     * 32x или 64x; "32x" / "64x" — всегда; "off" — не трогать (паки можно включить вручную в списке ресурспаков).
     */
    public String hdTextures = HD_AUTO;

    public static LilyBloomConfig get() {
        return instance;
    }

    static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(LilyBloom.MOD_ID + ".json");
        LilyBloomConfig loaded = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, LilyBloomConfig.class);
            } catch (IOException | JsonParseException e) {
                LilyBloom.LOGGER.warn("Could not read {}, using defaults: {}", path, e.toString());
            }
        }
        instance = loaded != null ? loaded : new LilyBloomConfig();
        instance.budChance = Math.max(1, instance.budChance);
        instance.growChance = Math.max(1, instance.growChance);
        instance.maxBloomingNearby = Math.max(0, instance.maxBloomingNearby);
        String hd = instance.hdTextures == null ? HD_AUTO : instance.hdTextures.trim().toLowerCase(java.util.Locale.ROOT);
        if (!hd.equals(HD_AUTO) && !hd.equals(HD_32) && !hd.equals(HD_64) && !hd.equals(HD_OFF)) {
            LilyBloom.LOGGER.warn("Unknown hdTextures value '{}' in {}, using '{}' (allowed: auto, 32x, 64x, off)", instance.hdTextures, path, HD_AUTO);
            hd = HD_AUTO;
        }
        instance.hdTextures = hd;

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            LilyBloom.LOGGER.warn("Could not save {}: {}", path, e.toString());
        }
    }
}
