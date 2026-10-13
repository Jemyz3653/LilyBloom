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
 * config/lilybloom.json. Читается при запуске. Имеет значение только на сервере
 * (в одиночной игре — на встроенном сервере); на клиенте, который заходит на сервер, ни на что не влияет.
 */
public final class LilyBloomConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static LilyBloomConfig instance = new LilyBloomConfig();

    /** Обычные кувшинки сами расцветают от случайных тиков (под открытым небом). Работает и в уже сгенерированных чанках. */
    public boolean naturalBlooming = true;
    /**
     * Шанс расцвести за один случайный тик: 1 из N.
     * При randomTickSpeed = 3 конкретный блок получает случайный тик в среднем раз в ~68 секунд,
     * значит при 25 кувшинка расцветает в среднем примерно за полчаса игрового времени рядом с игроком.
     */
    public int bloomChance = 25;
    /** Если в области 7×3×7 вокруг уже столько цветущих кувшинок или больше, новая сама не расцветёт. 0 — без ограничения. Костной муке не мешает. */
    public int maxBloomingNearby = 3;
    /** Расцветать самостоятельно только днём. */
    public boolean onlyDuringDay = true;
    /** Генерация цветущих кувшинок в новых чанках там же, где генерируются обычные (болота, мангровые болота). */
    public boolean worldGeneration = true;

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
        instance.bloomChance = Math.max(1, instance.bloomChance);
        instance.maxBloomingNearby = Math.max(0, instance.maxBloomingNearby);

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
