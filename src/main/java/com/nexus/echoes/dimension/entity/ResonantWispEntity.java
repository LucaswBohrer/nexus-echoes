package com.nexus.echoes.dimension.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/**
 * Resonant Wisp — hostile, anomaly-associated (ADR-011).
 *
 * <p>A drifting knot of resonant energy given spite. Spawns thinly across the
 * Hollow but clusters near active anomalies. Flies, strafes, and blinks a
 * short distance when struck (never far — it wants to stay angry). Drops
 * echo fiber, the organic-technological material of the Resonant Forest.
 */
public class ResonantWispEntity extends Monster {

    public ResonantWispEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.FLYING_SPEED, 0.55)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && level() instanceof ServerLevel serverLevel) {
            // Blink: a short, angry reposition — server-authoritative, bounded.
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 3.0 + random.nextDouble() * 3.0;
            double nx = getX() + Math.cos(angle) * dist;
            double nz = getZ() + Math.sin(angle) * dist;
            double ny = Math.max(serverLevel.getMinBuildHeight() + 1,
                    Math.min(serverLevel.getMaxBuildHeight() - 1, getY() + (random.nextDouble() - 0.5) * 4));
            serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 0.5, getZ(),
                    12, 0.3, 0.5, 0.3, 0.2);
            teleportTo(nx, ny, nz);
        }
        return damaged;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }
}
