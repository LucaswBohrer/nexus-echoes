package com.nexus.echoes.dimension.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Unstable Fracture — environmental hazard block (ADR-011).
 *
 * <p>Warning: the block visibly cracks (particles) and hums. Risk: standing
 * on it deals steady damage as dimensional shear. Counterplay: step off,
 * bridge over, or place an anomaly ward nearby (wards suppress the damage).
 * Reward: fractures cluster around resonant ore veins, so they mark
 * prospecting spots.
 */
public class UnstableFractureBlock extends Block {

    public UnstableFractureBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            super.stepOn(level, pos, state, entity);
            return;
        }
        if (level instanceof ServerLevel serverLevel
                && !com.nexus.echoes.dimension.mc.AnomalySavedData.get(serverLevel)
                        .isWarded(pos, WardBlockEntity.RADIUS)) {
            // 1 damage (half heart) per second while standing on it.
            if (living.tickCount % 20 == 0) {
                living.hurt(living.damageSources().magic(), 1.0F);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Client-side warning: the fracture visibly seeps.
        if (random.nextInt(6) == 0) {
            Vec3 p = Vec3.atCenterOf(pos).add(
                    (random.nextDouble() - 0.5) * 0.8, 0.55, (random.nextDouble() - 0.5) * 0.8);
            level.addParticle(ParticleTypes.PORTAL, p.x(), p.y(), p.z(), 0, 0.05, 0);
        }
    }
}
