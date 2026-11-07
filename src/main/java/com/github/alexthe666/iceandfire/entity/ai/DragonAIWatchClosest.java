package com.github.alexthe666.iceandfire.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;

public class DragonAIWatchClosest extends LookAtPlayerGoal {

    public DragonAIWatchClosest(PathfinderMob LivingEntityIn, Class<? extends LivingEntity> watchTargetClass, float maxDistance) {
        super(LivingEntityIn, watchTargetClass, maxDistance);
    }

    @Override
    public boolean canUse() {
        return isAvailableToWatch() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return isAvailableToWatch() && super.canContinueToUse();
    }

    /**
     * Same conditions as 1.12.2 RLCraft Dregora: no head turning while ridden, while it can't move,
     * during the shake-prey animation, or while breathing into a dragonforge (so the head and the
     * breath stay pointed at the forge).
     */
    private boolean isAvailableToWatch() {
        if (!(this.mob instanceof EntityDragonBase dragon)) {
            return true;
        }
        return dragon.getPassengers().isEmpty()
                && dragon.canMove()
                && dragon.getAnimation() != EntityDragonBase.ANIMATION_SHAKEPREY
                && !dragon.isFuelingForge();
    }
}
