package com.github.alexthe666.iceandfire.entity.ai;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import com.github.alexthe666.iceandfire.entity.util.IDreadMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class DreadAITargetNonDread extends NearestAttackableTargetGoal<LivingEntity> {

    public DreadAITargetNonDread(Mob entityIn, Class<LivingEntity> classTarget, boolean checkSight,
                                 Predicate<LivingEntity> targetSelector) {
        // Dread Targeting Check Sight can turn the sight requirement off for every dread mob
        super(entityIn, classTarget, 0, checkSight && IafConfig.dreadTargetingCheckSight, false, targetSelector);
    }

    @Override
    protected boolean canAttack(@Nullable LivingEntity target, @NotNull TargetingConditions targetPredicate) {
        if (super.canAttack(target, targetPredicate)) {
            return !(target instanceof IDreadMob) && DragonUtils.isAlive(target)
                && !IafConfig.dreadTargetingEntityBlacklist.contains(ForgeRegistries.ENTITY_TYPES.getKey(target.getType()));
        }
        return false;
    }

}
