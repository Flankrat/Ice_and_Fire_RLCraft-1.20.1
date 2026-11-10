package com.github.alexthe666.iceandfire.pathfinding;

import javax.annotation.Nullable;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class DragonFlyingPathNavigation extends FlyingPathNavigation {

    /** How often to try again when no free wander point was found (ticks). */
    private static final int RETRY_TICKS = 5;
    /** How long a dragon may go without a wander point before it uses the escape point (ticks). */
    private static final int GIVE_UP_TICKS = 40;

    private final EntityDragonBase dragon;
    private int retryCooldown;
    private int failedTicks;

    public DragonFlyingPathNavigation(EntityDragonBase dragon, Level level) {
        super(dragon, level);
        this.dragon = dragon;
    }

    @Override
    public boolean moveTo(@Nullable Path p_26537_, double p_26538_) {
    	return true;
    }
    
	@Override
	public void tick() {
		boolean chasing = this.dragon.getTarget() != null && this.dragon.getTarget().isAlive();
		if (chasing) {
			// Follow a moving attack target every tick, like the 1.12.2 air-target AI did,
			// instead of flying to where it was when the point was picked.
			this.dragon.airTarget = DragonUtils.getBlockWithTarget(dragon);
			this.failedTicks = 0;
		} else if (this.dragon.airTarget == null) {
			this.dragon.airTarget = pickWanderPoint();
		}
		BlockPos target = this.dragon.airTarget;
		if (target == null) {
			// No free spot yet (e.g. right after hitting a wall); pickWanderPoint() tries again shortly.
			return;
		}
		BlockPos dragonpos = this.dragon.blockPosition();
		int distance = target.distManhattan(dragonpos);
		boolean arrived = distance <= dragon.getRenderSize() + 2;
		boolean hasHome = this.dragon.hasHomePosition && this.dragon.homePos != null;

		if (!chasing && hasHome && !isNearHome(dragonpos) && !isNearHome(target)) {
			// Wandered past its home range and isn't already heading back: fly back toward home.
			// This is one fixed point, so the dragon keeps a steady course until it's back in range,
			// instead of getting a new random target every tick while it's outside.
			setTarget(homeReturnPoint());
		} else if (arrived || this.dragon.horizontalCollision || BlockPos.ZERO.equals(target)) {
			setTarget(chasing ? DragonUtils.getBlockWithTarget(dragon) : pickWanderPoint());
		}
	}

	private void setTarget(@Nullable BlockPos target) {
		this.dragon.airTarget = target;
		if (target != null) {
			MoveControl control = this.dragon.getMoveControl();
			control.setWantedPosition(target.getX(), target.getY(), target.getZ(), control.getSpeedModifier());
		}
	}

	/**
	 * Whether a position is inside the dragon's home range. Uses the same square, horizontal-only
	 * area that {@link DragonUtils#getBlockInView} picks wander points from, so flight height doesn't
	 * count and every wander point it gives counts as near home.
	 */
	private boolean isNearHome(BlockPos pos) {
		BlockPos home = this.dragon.homePos.getPosition();
		int range = Math.max(1, IafConfig.dragonWanderFromHomeDistance);
		return Math.abs(pos.getX() - home.getX()) <= range && Math.abs(pos.getZ() - home.getZ()) <= range;
	}

	/** Above the home position, at the dragon's current height (kept under the flight ceiling). */
	private BlockPos homeReturnPoint() {
		BlockPos home = this.dragon.homePos.getPosition();
		int ceiling = DragonUtils.getMaximumFlightHeightForPos(dragon.level(), home);
		int y = Math.min(Math.max(this.dragon.getBlockY(), home.getY() + 8), ceiling);
		return new BlockPos(home.getX(), y, home.getZ());
	}

	/**
	 * A new wander point from {@link DragonUtils#getBlockInView}, or null if there's no free spot
	 * right now. After a failed pick it waits {@link #RETRY_TICKS} before raytracing again, and after
	 * {@link #GIVE_UP_TICKS} without a point it uses {@link #escapePoint()} so a boxed-in dragon
	 * (ravine, overhang, cave) can't hover in place forever.
	 */
	@Nullable
	private BlockPos pickWanderPoint() {
		if (this.retryCooldown > 0) {
			this.retryCooldown--;
			return null;
		}
		BlockPos pos = DragonUtils.getBlockInView(dragon);
		if (pos != null) {
			this.failedTicks = 0;
			return pos;
		}
		this.retryCooldown = RETRY_TICKS - 1;
		this.failedTicks += RETRY_TICKS;
		if (this.failedTicks >= GIVE_UP_TICKS) {
			this.failedTicks = 0;
			return escapePoint();
		}
		return null;
	}

	/**
	 * Last resort when no wander point can be found: climb straight up if there's room, otherwise
	 * fly back the way the dragon came. If both are blocked, stop flying so gravity brings it down
	 * and it can take off again from the ground.
	 */
	@Nullable
	private BlockPos escapePoint() {
		int ceiling = DragonUtils.getMaximumFlightHeightForPos(dragon.level(), dragon.blockPosition());

		BlockPos up = BlockPos.containing(dragon.getX(), Math.min(dragon.getY() + 8 + dragon.getDragonStage() * 2, ceiling), dragon.getZ());
		if (up.getY() > dragon.getBlockY() + 2 && isFree(up)) {
			return up;
		}

		float yaw = dragon.getYRot() * Mth.DEG_TO_RAD;
		double dist = 12 + dragon.getRenderSize();
		// Minecraft's forward vector is (-sin(yaw), cos(yaw)), so this is behind the dragon.
		BlockPos back = BlockPos.containing(dragon.getX() + Mth.sin(yaw) * dist, Math.min(dragon.getY() + 2, ceiling), dragon.getZ() - Mth.cos(yaw) * dist);
		if (isFree(back)) {
			return back;
		}

		this.dragon.setFlying(false);
		this.dragon.setHovering(false);
		return null;
	}

	private boolean isFree(BlockPos pos) {
		return this.dragon.level().isEmptyBlock(pos) && !this.dragon.isTargetBlocked(Vec3.atCenterOf(pos));
	}
}
