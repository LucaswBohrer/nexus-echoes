package com.nexus.echoes.dimension.block;

import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeRole;
import com.nexus.echoes.kinetic.mc.KineticBlockEntity;
import com.nexus.echoes.kinetic.sim.NodeState;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The spire's charge state. A kinetic consumer: while the delivered snapshot
 * meets the requirement, charge accumulates proportionally (brownout slows
 * charging instead of stalling it). Charge decays slowly without power.
 *
 * <p>The charge rule itself is a pure function ({@link #nextCharge}) so the
 * progression math is unit-testable; the BE only wires it to the kinetic
 * snapshot and NBT.
 */
public class SpireBlockEntity extends KineticBlockEntity {

    public static final double REQUIRED_RPM = 240.0;
    public static final double REQUIRED_TORQUE_NM = 25.0;

    /** Full charge after 60 s at full power. */
    static final double CHARGE_PER_TICK = 1.0 / (20.0 * 60.0);
    static final double DECAY_PER_TICK = 1.0 / (20.0 * 300.0);

    private double charge;

    public SpireBlockEntity(BlockPos pos, BlockState state) {
        super(NexusRegistries.SPIRE_BE.get(), pos, state);
    }

    // ---------------------------------------------------------- kinetic role

    @Override
    public NodeRole getKineticRole() {
        return NodeRole.CONSUMER;
    }

    @Override
    public Rpm getRequiredRpm() {
        return Rpm.of(REQUIRED_RPM);
    }

    @Override
    public Torque getRequiredTorque() {
        return Torque.ofNewtonMeters(REQUIRED_TORQUE_NM);
    }

    // ------------------------------------------------------------------ tick

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpireBlockEntity be) {
        NodeState snapshot = be.getKineticState();
        be.charge = nextCharge(be.charge,
                snapshot.rpm().value(), snapshot.torque().newtonMeters(), snapshot.isRunning());
        be.setChanged();
    }

    /**
     * Pure charge rule: proportional accumulation while powered, slow decay
     * otherwise. Charge stays in [0, 1].
     */
    public static double nextCharge(double charge, double deliveredRpm,
                                    double deliveredTorqueNm, boolean running) {
        double factor = Math.min(deliveredRpm / REQUIRED_RPM,
                deliveredTorqueNm / REQUIRED_TORQUE_NM);
        if (factor > 0.01 && running) {
            return Math.min(1.0, charge + CHARGE_PER_TICK * Math.min(1.0, factor));
        }
        return Math.max(0.0, charge - DECAY_PER_TICK);
    }

    /** 0..1 charge. Travel requires exactly 1.0. */
    public double getCharge() {
        return charge;
    }

    public boolean isCharged() {
        return charge >= 1.0;
    }

    /** Travel consumes the full charge — each crossing must be earned. */
    public void discharge() {
        charge = 0.0;
        setChanged();
    }

    // -------------------------------------------------------------------- nbt

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("Charge", charge);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        charge = tag.contains("Charge")
                ? Math.max(0.0, Math.min(1.0, tag.getDouble("Charge")))
                : 0.0;
    }
}
