package com.github.alexthe666.iceandfire.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntityBlackFrostDragon;
import com.github.alexthe666.iceandfire.entity.EntityDreadQueen;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

/** From the 1.12 version: a Dread Queen walks to a free Black Frost dragon within 32 blocks and mounts it. */
public class DreadAIMountDragon extends Goal {
    private final EntityDreadQueen queen;
    private EntityBlackFrostDragon dragon;

    public DreadAIMountDragon(EntityDreadQueen queen) {
        this.queen = queen;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.queen.isPassenger()) {
            return false;
        }
        List<EntityBlackFrostDragon> list = this.queen.level().getEntitiesOfClass(EntityBlackFrostDragon.class, this.queen.getBoundingBox().inflate(32.0D, 7.0D, 32.0D));
        this.dragon = null;
        for (EntityBlackFrostDragon candidate : list) {
            if (!candidate.isVehicle() && candidate.isAlive()) {
                this.dragon = candidate;
                break;
            }
        }
        return this.dragon != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.queen.isPassenger() && this.dragon != null && this.dragon.isAlive() && !this.dragon.isVehicle();
    }

    @Override
    public void start() {
        this.dragon.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.dragon = null;
        this.queen.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.queen.getLookControl().setLookAt(this.dragon, 30.0F, 30.0F);
        this.queen.getNavigation().moveTo(this.dragon, 1.2D);
        if (this.queen.distanceToSqr(this.dragon) < this.dragon.getRenderSize()) {
            this.queen.getNavigation().stop();
            this.dragon.setCommanderId(this.queen.getUUID());
            this.queen.startRiding(this.dragon);
        }
    }
}
