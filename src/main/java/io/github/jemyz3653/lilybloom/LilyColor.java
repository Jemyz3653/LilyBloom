package io.github.jemyz3653.lilybloom;

import net.minecraft.util.RandomSource;

/**
 * Цвета кувшинок. Каждый цвет — отдельные блоки и предметы (как ванильные тюльпаны).
 * У розового нет префикса в ID: он был единственным в 1.0.x, и так уже поставленные в мире блоки не пропадают.
 */
public enum LilyColor {
    PINK("pink", "", 30),
    WHITE("white", "white_", 35),
    YELLOW("yellow", "yellow_", 20),
    BLUE("blue", "blue_", 15);

    private final String name;
    private final String idPrefix;
    /** Вес при случайном выборе цвета (самостоятельное цветение, костная мука, генерация мира). */
    private final int weight;

    LilyColor(final String name, final String idPrefix, final int weight) {
        this.name = name;
        this.idPrefix = idPrefix;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    /** "white_blooming_lily_pad" и т. п.; для розового — исторические ID без префикса. */
    public String id(final String base) {
        return idPrefix + base;
    }

    /** "potted_white_lily_flower"; для розового — "potted_lily_flower". */
    public String pottedId() {
        return "potted_" + idPrefix + "lily_flower";
    }

    public int getWeight() {
        return weight;
    }

    public static LilyColor random(final RandomSource random) {
        int total = 0;
        for (LilyColor color : values()) {
            total += color.weight;
        }
        int roll = random.nextInt(total);
        for (LilyColor color : values()) {
            roll -= color.weight;
            if (roll < 0) {
                return color;
            }
        }
        return PINK;
    }
}
