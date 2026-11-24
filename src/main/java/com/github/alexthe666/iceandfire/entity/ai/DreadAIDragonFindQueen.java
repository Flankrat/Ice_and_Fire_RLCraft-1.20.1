package com.github.alexthe666.iceandfire.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntityBlackFrostDragon;
import com.github.alexthe666.iceandfire.entity.EntityDreadQueen;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

/**
 * From the 1.12 version: a Black Frost dragon flies or walks to a Dread Queen within 128 blocks who isn't riding
 * anything, and picks her up. While it's on its way it doesn't take off (see EntityBlackFrostDragon.isAllowedToTriggerFlight).
 */
public class DreadAIDragonFindQueen extends Goal {
    private final EntityBlackFrostDragon dragon;
    private EntityDreadQueen queen;

    public DreadAIDragonFindQueen(EntityBlackFrostDragon dragon) {
        this.dragon = dragon;
        // It steers the dragon, so other movement/look goals have to wait (with no flags they ran alongside it and
        // overrode where it was going)
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.dragon.isVehicle()) {
            return false;
        }
        // The search covers a 257-block box: like vanilla's target goals, only look now and then (~every 10 ticks)
        if (this.dragon.getRandom().nextInt(reducedTickDelay(10)) != 0) {
            return false;
        }
        List<EntityDreadQueen> list = this.dragon.level().getEntitiesOfClass(EntityDreadQueen.class, this.dragon.getBoundingBox().inflate(128.0D, 128.0D, 128.0D));
        this.queen = null;
        for (EntityDreadQueen candidate : list) {
            if (!candidate.isPassenger() && candidate.isAlive()) {
                this.queen = candidate;
                break;
            }
        }
        return this.queen != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.queen != null && this.queen.isAlive() && !this.queen.isPassenger() && !this.dragon.isVehicle();
    }

    @Override
    public void start() {
        this.queen.getNavigation().stop();
        this.dragon.setSeekingQueen(true);
    }

    @Override
    public void stop() {
        this.queen = null;
        this.dragon.setSeekingQueen(false);
        this.dragon.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.dragon.getLookControl().setLookAt(this.queen, 30.0F, 30.0F);
        if (this.dragon.isFlying() || this.dragon.isHovering()) {
            // The queen can only climb on from the ground, so a flying dragon lands once it's over her
            this.dragon.getMoveControl().setWantedPosition(this.queen.getX(), this.queen.getY() + 1, this.queen.getZ(), 1.2D);
            double dx = this.dragon.getX() - this.queen.getX();
            double dz = this.dragon.getZ() - this.queen.getZ();
            if (dx * dx + dz * dz < 36.0D) {
                this.dragon.setFlying(false);
                this.dragon.setHovering(false);
            }
        } else {
            this.dragon.getNavigation().moveTo(this.queen, 1.2D);
        }
        if (this.dragon.distanceToSqr(this.queen) < this.dragon.getRenderSize()) {
            this.dragon.getNavigation().stop();
            this.dragon.setCommanderId(this.queen.getUUID());
            this.queen.startRiding(this.dragon);
        }
    }
}
