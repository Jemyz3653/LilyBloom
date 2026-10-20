package io.github.jemyz3653.lilybloom.block;

import io.github.jemyz3653.lilybloom.LilyColor;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

/** Цветок на крупной кувшинке: нет или один из цветов кувшинок. */
public enum GiantFlower implements StringRepresentable {
    NONE("none", null),
    PINK("pink", LilyColor.PINK),
    WHITE("white", LilyColor.WHITE),
    YELLOW("yellow", LilyColor.YELLOW),
    BLUE("blue", LilyColor.BLUE);

    private final String name;
    private final @Nullable LilyColor color;

    GiantFlower(final String name, final @Nullable LilyColor color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public @Nullable LilyColor color() {
        return color;
    }

    public static GiantFlower of(final LilyColor color) {
        return switch (color) {
            case PINK -> PINK;
            case WHITE -> WHITE;
            case YELLOW -> YELLOW;
            case BLUE -> BLUE;
        };
    }
}
