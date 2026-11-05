package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.util.IFlyingMount;
import com.github.alexthe666.iceandfire.util.IAFMath;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * MoveControl
 * No longer using flyaround method
 * this handle the dragon moving to its air target block position
 */
public class IafDragonFlightManager {
	
	public static float approach(float number, float max, float min) {
        min = Math.abs(min);
        return number < max ? Mth.clamp(number + min, number, max) : Mth.clamp(number - min, max, number);
    }

    public static float approachDegrees(float number, float max, float min) {
        float add = Mth.wrapDegrees(max - number);
        return approach(number, number + add, min);
    }

    public static float degreesDifferenceAbs(float f1, float f2) {
        return Math.abs(Mth.wrapDegrees(f2 - f1));
    }

    protected static class GroundMoveHelper extends MoveControl {
        public GroundMoveHelper(Mob LivingEntityIn) {
            super(LivingEntityIn);
        }

        public float distance(float rotateAngleFrom, float rotateAngleTo) {
            return (float) IAFMath.atan2_accurate(Mth.sin(rotateAngleTo - rotateAngleFrom), Mth.cos(rotateAngleTo - rotateAngleFrom));
        }

        @Override
        public void tick() {
            if (this.operation == Operation.STRAFE) {
                float f = (float) this.mob.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
                float f1 = (float) this.speedModifier * f;
                float f2 = this.strafeForwards;
                float f3 = this.strafeRight;
                float f4 = Mth.sqrt(f2 * f2 + f3 * f3);

                if (f4 < 1.0F) {
                    f4 = 1.0F;
                }

                f4 = f1 / f4;
                f2 = f2 * f4;
                f3 = f3 * f4;
                float f5 = Mth.sin(this.mob.getYRot() * 0.017453292F);
                float f6 = Mth.cos(this.mob.getYRot() * 0.017453292F);
                float f7 = f2 * f6 - f3 * f5;
                float f8 = f3 * f6 + f2 * f5;
                PathNavigation pathnavigate = this.mob.getNavigation();
                if (pathnavigate != null) {
                    NodeEvaluator nodeprocessor = pathnavigate.getNodeEvaluator();
                    if (nodeprocessor != null && nodeprocessor.getBlockPathType(this.mob.level(), Mth.floor(this.mob.getX() + (double) f7), Mth.floor(this.mob.getY()), Mth.floor(this.mob.getZ() + (double) f8)) != BlockPathTypes.WALKABLE) {
                        this.strafeForwards = 1.0F;
                        this.strafeRight = 0.0F;
                        f1 = f;
                    }
                }
                this.mob.setSpeed(f1);
                this.mob.setZza(this.strafeForwards);
                this.mob.setXxa(this.strafeRight);
                this.operation = Operation.WAIT;
            } else if (this.operation == Operation.MOVE_TO) {
                this.operation = Operation.WAIT;
                EntityDragonBase dragonBase = (EntityDragonBase) mob;
                double d0 = this.getWantedX() - this.mob.getX();
                double d1 = this.getWantedZ() - this.mob.getZ();
                double d2 = this.getWantedY() - this.mob.getY();
                double d3 = d0 * d0 + d2 * d2 + d1 * d1;

                if (d3 < 2.500000277905201E-7D) {
                    this.mob.setZza(0.0F);
                    return;
                }
                float targetDegree = (float) (Mth.atan2(d1, d0) * (180D / Math.PI)) - 90.0F;
                float changeRange = 70F;
                if (Math.ceil(dragonBase.getBbWidth()) > 2F) {
                    float ageMod = 1F - Math.min(dragonBase.getAgeInDays(), 125) / 125F;
                    changeRange = 5 + ageMod * 10;
                }
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetDegree, changeRange));
                this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                if (d2 > (double) this.mob.maxUpStep() && d0 * d0 + d1 * d1 < (double) Math.max(1.0F, this.mob.getBbWidth() / 2)) {
                    this.mob.getJumpControl().jump();
                    this.operation = Operation.JUMPING;
                }
            } else if (this.operation == Operation.JUMPING) {
                this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));

                if (this.mob.onGround()) {
                    this.operation = Operation.WAIT;
                }
            } else {
                this.mob.setZza(0.0F);
            }
        }

    }
    
    // Flight MoveControl for Kettle5000's flight rework. Not used yet: it goes with
    // DragonFlyingPathNavigation, which isn't in the repo. See EntityDragonBase#switchNavigator.
    
    protected static class DragonFlyingMoveControl extends MoveControl {

        private final EntityDragonBase dragon;

        public DragonFlyingMoveControl(EntityDragonBase dragonBase) {
            super(dragonBase);
            this.dragon = dragonBase;
        }

        
        public void tick() {
            if (dragon.horizontalCollision) {
                this.speedModifier = 0.02D;
                dragon.airTarget = null;
                return;
            }

            if (dragon.airTarget == null) {
                return;
            }
            final double dx = dragon.airTarget.getX() - dragon.getX();
            final double dy = dragon.airTarget.getY() - dragon.getY();
            final double dz = dragon.airTarget.getZ() - dragon.getZ();

            final double distanceSqr = dx * dx + dy * dy + dz * dz;

            if (distanceSqr < 1.0D) {
                return;
            }

            // ----------------------------------------
            // ROTATION: Slowly face the target (yaw only)
            // ----------------------------------------

            final float targetYaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);

            dragon.setYRot(
                Mth.approachDegrees(dragon.getYRot(), targetYaw, 10.0F)
            );

            dragon.yBodyRot = dragon.getYRot();

            // ----------------------------------------
            // MOVEMENT: Always move toward target
            // ----------------------------------------

            final double movementSpeed = (0.01D / Math.sqrt(distanceSqr)) + 0.001D;
            Vec3 delta = dragon.getDeltaMovement().add(
                    dx * movementSpeed,
                    dy * movementSpeed,
                    dz * movementSpeed
                );
            double intensity = delta.length();
            double maxspeed = 0.15 + (dragon.getDragonStage() * 0.05);
            if (intensity > maxspeed) {
            	delta = delta.scale(maxspeed/intensity);
            }
            dragon.setDeltaMovement(delta);
        }
    }

    protected static class PlayerFlightMoveHelper<T extends Mob & IFlyingMount> extends MoveControl {

        private final T dragon;

        public PlayerFlightMoveHelper(T dragon) {
            super(dragon);
            this.dragon = dragon;
        }

        @Override
        public void tick() {
            if (dragon instanceof EntityDragonBase theDragon && theDragon.getControllingPassenger() != null) {
                // New ride system doesn't need move controller
                // The flight move control is disabled here, the walking move controller will stay Operation.WAIT so nothing will happen too
                return;
            }

            double flySpeed = speedModifier * speedMod() * 3;
            Vec3 dragonVec = dragon.position();
            Vec3 moveVec = new Vec3(wantedX, wantedY, wantedZ);
            Vec3 normalized = moveVec.subtract(dragonVec).normalize();
            double dist = dragonVec.distanceTo(moveVec);
            dragon.setDeltaMovement(normalized.x * flySpeed, normalized.y * flySpeed, normalized.z * flySpeed);
            if (dist > 2.5E-7) {
                float yaw = (float) Math.toDegrees(Math.PI * 2 - Math.atan2(normalized.x, normalized.y));
                dragon.setYRot(rotlerp(dragon.getYRot(), yaw, 5));
                dragon.setSpeed((float) (speedModifier));
            }
            dragon.move(MoverType.SELF, dragon.getDeltaMovement());
        }

        public double speedMod() {
            return (dragon instanceof EntityAmphithere ? 0.6D : 1.25D) * IafConfig.dragonFlightSpeedMod * dragon.getAttributeValue(Attributes.MOVEMENT_SPEED);
        }
    }
}
