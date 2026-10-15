package io.github.jemyz3653.lilybloom.block;

import net.minecraft.util.StringRepresentable;

/** Стадии цветения: бутон → полураскрытый бутон → цветок. */
public enum LilyStage implements StringRepresentable {
    BUD("bud"),
    HALF_OPEN("half_open"),
    BLOOM("bloom");

    private final String name;

    LilyStage(final String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public LilyStage next() {
        return this == BUD ? HALF_OPEN : BLOOM;
    }
}
