package com.izakji.stuff.content.worktable;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Recipe metadata determines catalogue placement; ingredient tags remain independent.
 *
 * @author Izakji
 */
public enum WorktableCategory implements StringRepresentable {
    TABLES_AND_CHAIRS("tables_and_chairs"),
    LIGHTING("lighting"),
    UTILITY("utility"),
    STORAGE("storage"),
    DECORATION("decoration");

    public static final Codec<WorktableCategory> CODEC = StringRepresentable.fromEnum(WorktableCategory::values);
    private final String name;

    WorktableCategory(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Component title() {
        return Component.translatable("worktable.category.stuff." + name);
    }
}
