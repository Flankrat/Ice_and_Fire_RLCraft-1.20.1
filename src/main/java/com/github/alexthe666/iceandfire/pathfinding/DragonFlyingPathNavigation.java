package com.github.alexthe666.iceandfire.pathfinding;

import javax.annotation.Nullable;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

public class DragonFlyingPathNavigation extends FlyingPathNavigation {

    private final EntityDragonBase dragon;

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
		MoveControl control = this.dragon.getMoveControl();
		if (this.dragon.airTarget == null) {
			this.dragon.airTarget = DragonUtils.getBlockInView(dragon);
		}
		BlockPos target = this.dragon.airTarget;
		BlockPos dragonpos = this.dragon.blockPosition();
		int distance = target.distManhattan(dragonpos);
		//if home then restrict the dragon to go away regardless of targeting, technically this is not needed if DragonsUtils.getBlockInView already chooses a position inside its home
		if (this.dragon.hasHomePosition && this.dragon.homePos != null) {
			if (this.dragon.homePos.getPosition().distManhattan(dragonpos) >= IafConfig.dragonWanderFromHomeDistance) {  // manhattan is very useful for those approximations
				this.dragon.airTarget = DragonUtils.getBlockInView(dragon);
				target = this.dragon.airTarget;
				control.setWantedPosition(target.getX(), target.getY(), target.getZ(), control.getSpeedModifier()); // i wonder if this is necessary
			} else { 
				if (distance <= dragon.getRenderSize() + 2) { // dragon is close to the target so we choose a new one, no idea what threshold is good enough
					this.dragon.airTarget = this.dragon.getTarget() != null ? DragonUtils.getBlockWithTarget(dragon): DragonUtils.getBlockInView(dragon);
					target = this.dragon.airTarget;
					control.setWantedPosition(target.getX(), target.getY(), target.getZ(), control.getSpeedModifier());
				}
			}
		} else { //if dragon doesnt have a patrol area
			if (distance <= dragon.getRenderSize() + 2 || this.dragon.horizontalCollision || target == BlockPos.ZERO) {
				this.dragon.airTarget = this.dragon.getTarget() != null ? DragonUtils.getBlockWithTarget(dragon): DragonUtils.getBlockInView(dragon);
				target = this.dragon.airTarget;
				control.setWantedPosition(target.getX(), target.getY(), target.getZ(), control.getSpeedModifier());
			} 
		}
	}
}
