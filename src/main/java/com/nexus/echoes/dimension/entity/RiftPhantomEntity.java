package com.nexus.echoes.dimension.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Rift Phantom — rare, anomaly-manifested (ADR-011).
 *
 * <p>Does not spawn from the world at all. A spatial-rift anomaly tears one
 * through for the anomaly's lifetime; when the rift closes, the phantom
 * goes with it (the anomaly manager discards it). Fast, fragile, and worth
 * killing: its loot table is the most reliable source of memory fragments.
 */
public class RiftPhantomEntity extends Monster {

    /** Hard lifetime cap in ticks, refreshed only by the spawning anomaly. */
    public static final int MAX_LIFETIME_TICKS = 20 * 150;

    private int age;

    public RiftPhantomEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.age = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, false));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (++age > MAX_LIFETIME_TICKS) {
                discard();
            } else if (level() instanceof ServerLevel serverLevel && age % 20 == 0) {
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        getX(), getY() + 1.0, getZ(), 3, 0.2, 0.6, 0.2, 0.05);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PhantomAge", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        age = tag.getInt("PhantomAge");
    }

    /** Anomalies never cross dimensions; neither do their phantoms. */
    @Override
    public boolean canChangeDimensions() {
        return false;
    }
}
