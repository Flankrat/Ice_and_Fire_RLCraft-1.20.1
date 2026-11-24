package com.github.alexthe666.iceandfire.block;

import com.github.alexthe666.iceandfire.entity.DragonType;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 龙锻炉类型枚举 - 用于BlockState属性
 * NONE=未激活/无类型, FIRE=火龙, ICE=冰龙, LIGHTNING=闪电龙
 */
public enum DragonForgeType implements StringRepresentable {
    NONE("none", 0),
    FIRE("fire", 0),
    ICE("ice", 1),
    LIGHTNING("lightning", 2),
    GOLD("gold", 3);

    private final String name;
    private final int legacyId;

    DragonForgeType(String name, int legacyId) {
        this.name = name;
        this.legacyId = legacyId;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public int getLegacyId() {
        return legacyId;
    }

    /** 从旧的int类型ID获取枚举（用于过渡兼容） */
    public static DragonForgeType fromLegacyId(int id) {
        return switch (id) {
            case 1 -> ICE;
            case 2 -> LIGHTNING;
            case 3 -> GOLD;
            default -> FIRE;
        };
    }

    /** 判断是否为有效龙类型（非NONE） */
    public static DragonForgeType fromDragonType(DragonType type) {
        if (type == DragonType.ICE)
            return ICE;
        if (type == DragonType.LIGHTNING)
            return LIGHTNING;
        if (type == DragonType.GOLD)
            return GOLD;
        return FIRE;
    }

    /** The dragon types that have their own forge blocks in individual mode. */
    public static final DragonForgeType[] DRAGON_TYPES = {FIRE, ICE, LIGHTNING, GOLD};

    /** True when Dragonforge Mode is "individual": per-type forge blocks work and the shared ones don't. */
    public static boolean individualForges() {
        return "individual".equalsIgnoreCase(com.github.alexthe666.iceandfire.IafConfig.dragonforgeMode);
    }

    /** Whether a forge block with this fixed type (null = the shared block) works in the current mode. */
    public static boolean worksInCurrentMode(@javax.annotation.Nullable DragonForgeType fixedType) {
        return (fixedType != null) == individualForges();
    }

    public boolean isActive() {
        return this != NONE;
    }

    /** 获取配方匹配用的类型ID字符串 */
    public String getTypeId() {
        return name;
    }
}
