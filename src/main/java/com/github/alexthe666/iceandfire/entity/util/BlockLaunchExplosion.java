package com.github.alexthe666.iceandfire.entity.util;

import com.github.alexthe666.iceandfire.block.IDragonProof;
import com.github.alexthe666.iceandfire.compat.ModCompatList;
import com.github.alexthe666.iceandfire.datagen.tags.IafBlockTags;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.IafDragonDestructionManager;
import com.github.alexthe666.iceandfire.util.IafExplosionRays;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import team.creative.enhancedvisuals.EnhancedVisuals;

import java.util.List;
import java.util.Set;


public class BlockLaunchExplosion extends Explosion {
	protected final EntityDragonBase dragon;
    private final float size;
    private final Level level;
    private final BlockInteraction mode;
    public final ObjectArrayList<BlockPos> blocks = new ObjectArrayList<>();
	public final ObjectArrayList<Entity> entities = new ObjectArrayList<>();
	public final BlockPos center;
    
    public BlockLaunchExplosion(Level world, EntityDragonBase entity, double x, double y, double z, float size) {
        this(world, entity, x, y, z, size, BlockInteraction.DESTROY);
    }

    public BlockLaunchExplosion(Level world, EntityDragonBase entity, double x, double y, double z, float size, BlockInteraction mode) {
        this(world, entity, null, x, y, z, size, mode);
    }

    public BlockLaunchExplosion(Level world, EntityDragonBase entity, DamageSource source, double x, double y, double z, float size, BlockInteraction mode) {
        super(world, entity, source, null, x, y, z, size, false, mode);
        this.dragon = entity;
        this.level = world;
        this.size = size;
        this.mode = mode;
        this.center = BlockPos.containing(x, y, z).immutable();
    }

    @Override
	public void explode() {
		level.gameEvent(dragon, GameEvent.EXPLODE, center);
		level.playSound(null, center, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F + this.size * 0.2f, 1.0F);
		Set<BlockPos> set = getExplosionBlocks(level, center, this.size);
		this.blocks.addAll(set);
		this.entities.addAll(level.getEntities(this.dragon,
				new AABB((double) center.getX() - this.size, (double) center.getY() - this.size,
						(double) center.getZ() - this.size, (double) center.getX() + this.size,
						(double) center.getY() + this.size, (double) center.getZ() + this.size)));
		ForgeEventFactory.onExplosionDetonate(this.level, this, this.entities, this.size*2f);
		if (ModCompatList.isEVon) {
			EnhancedVisuals.EVENTS.explosion(this, (List<Entity>) this.entities);
		}
		
	}
    
    /**
     * Does the second part of the explosion (sound, particles, drop spawn)
     */
    @Override
    public void finalizeExplosion(boolean spawnParticles) {
        if (level.isClientSide()) {
            this.level.playLocalSound(center.getX(), center.getY(), center.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
        }

        boolean flag = this.mode != BlockInteraction.KEEP;
        if (spawnParticles) {
            if (!(this.size < 2.0F) && flag) {
                this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, center.getX(), center.getY(), center.getZ(), 1.0D, 0.0D, 0.0D);
            } else {
                this.level.addParticle(ParticleTypes.EXPLOSION, center.getX(), center.getY(), center.getZ(), 1.0D, 0.0D, 0.0D);
            }
        }

        if (flag) {
            ObjectArrayList<Pair<ItemStack, BlockPos>> objectarraylist = new ObjectArrayList<>();
            for (BlockPos blockpos : this.blocks) {
                BlockState blockstate = this.level.getBlockState(blockpos);
                if (!blockstate.isAir() && !blockstate.hasBlockEntity()) {
                	int d = (int)this.size - blockpos.distManhattan(this.center);
                	if (d > 0) {
                		blockstate.onBlockExploded(this.level, blockpos, this);
                	} else if (d == 0) {
                		if (dragon.getRandom().nextBoolean()) {
                			IafDragonDestructionManager.attackBlock(level, dragon, blockpos);
                		}
                    	blockstate = this.level.getBlockState(blockpos);
                    	if (dragon.getRandom().nextBoolean()) {
                    		BlockPos blockpos1 = blockpos.immutable();
                            this.level.getProfiler().push("explosion_blocks");
                            
                            blockstate.onBlockExploded(this.level, blockpos, this);
                            FallingBlockEntity fallingBlockEntity = FallingBlockEntity.fall(level, blockpos, blockstate);
                            fallingBlockEntity.setStartPos(blockpos1);
                            fallingBlockEntity.setPos(blockpos1.getX() + 0.5D, blockpos1.getY() + 0.5D, blockpos1.getZ() + 0.5D);
                            double angle = Math.PI * 2 * dragon.getRandom().nextDouble();
                            double distance = 3 - (2  * dragon.getRandom().nextDouble());
                            fallingBlockEntity.setDeltaMovement(
                                    distance * Math.sin(angle),
                                    1 + dragon.getRandom().nextDouble(),
                                    distance * Math.cos(angle)
                            );
                            fallingBlockEntity.dropItem = false;
                            fallingBlockEntity.setInvulnerable(true);
                            this.level.getProfiler().pop();
                    	}
                	}
                }
            }

            for (Pair<ItemStack, BlockPos> pair : objectarraylist) {
                Block.popResource(this.level, pair.getSecond(), pair.getFirst());
            }
        }
    }
    
	public static Set<BlockPos> getExplosionBlocks(Level level, BlockPos center, float radius) {
		Set<BlockPos> affectedBlocks = Sets.newHashSet();

		if (radius <= 0.0F) {
			return affectedBlocks;
		}
		
		if (canExplosionDestroy(level, center, radius, 0)) {
			affectedBlocks.add(center);
		}

	    for (int[] dir : IafExplosionRays.RAYS) {
	        int dx = dir[0], dy = dir[1], dz = dir[2];
	        int maxSteps = (int) Math.ceil(radius);
	        for (int k = 1; k <= maxSteps; k++) {
	            int x = center.getX() + dx * k;
	            int y = center.getY() + dy * k;
	            int z = center.getZ() + dz * k;

	            // Cheap distance² check (no sqrt)
	            int dist2 = (dx*k)*(dx*k) + (dy*k)*(dy*k) + (dz*k)*(dz*k);
	            if (dist2 > radius * radius) break;

	            BlockPos pos = new BlockPos(x, y, z);

	            if (!canExplosionDestroy(level, pos, radius, k)) {
	                break;   // this ray is finished
	            }

	            affectedBlocks.add(pos);
	        }
	    }
		return affectedBlocks;
	}
    
	private static boolean canExplosionDestroy(Level level, BlockPos pos, float radius, int distance) {
		BlockState state = level.getBlockState(pos);
		float hardness = state.getDestroySpeed(level, pos) + (level.getRandom().nextFloat() * 0.5f);
		float ratio = radius - distance;
		float threshold = (ratio * ratio);
		return hardness > 0.0F && hardness <= threshold && !state.is(IafBlockTags.DRAGON_BLOCK_BREAK_BLACKLIST) && state.getFluidState().isEmpty() && !(state.getBlock() instanceof IDragonProof);
	}

}
