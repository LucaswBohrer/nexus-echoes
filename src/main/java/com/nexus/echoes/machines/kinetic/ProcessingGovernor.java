package com.nexus.echoes.machines.kinetic;

import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeStatus;

/**
 * Pure decision logic for kinetic machines (Phase 3, ADR-009).
 *
 * <p>Maps the network-simulated state ({@link NodeStatus}, delivered torque)
 * to machine behavior: how fast progress advances and which high-level status
 * the GUI shows. Pure Java — no Minecraft — so the brownout rules are unit
 * tested without a game instance.
 *
 * <p>Brownout rule: progress advances proportionally to the scarcest resource —
 * {@code min(delivered/required torque, delivered/required RPM)}. Full power →
 * full speed; half torque or half RPM → half speed; no shaft input → stalled.
 * RPM is a hard gate in the sim (below requirement the node reports
 * UNDERPOWERED); the governor turns that into proportional slowdown instead
 * of a binary stall, so a 120 RPM shaft on a 240 RPM machine crawls at 50%.
 */
public final class ProcessingGovernor {

    private ProcessingGovernor() {
    }

    /**
     * Progress ticks earned per game tick.
     *
     * @return 1.0 at full power, a fraction in (0,1) under brownout, 0.0 stalled
     */
    public static double speedFactor(NodeStatus kineticStatus,
                                     Rpm deliveredRpm, Rpm requiredRpm,
                                     Torque deliveredTorque, Torque requiredTorque) {
        return switch (kineticStatus) {
            case OK -> 1.0;
            case UNDERPOWERED -> Math.min(ratio(deliveredTorque.newtonMeters(), requiredTorque.newtonMeters()),
                    ratio(deliveredRpm.value(), requiredRpm.value()));
            default -> 0.0; // NO_INPUT, DISABLED, OVERLOADED, CONFLICT: stalled
        };
    }

    private static double ratio(double delivered, double required) {
        if (required <= 0) {
            return delivered > 0 ? 1.0 : 0.0;
        }
        return Math.min(1.0, Math.max(0.0, delivered / required));
    }

    /**
     * Derives the machine-level status for GUIs/diagnostics.
     *
     * @param hasWork       a recipe matches the current input
     * @param outputBlocked the result (or byproduct) cannot fit the output slots
     */
    public static MachineStatus deriveStatus(boolean hasWork, boolean outputBlocked, NodeStatus kineticStatus) {
        if (!hasWork) {
            return MachineStatus.IDLE;
        }
        if (outputBlocked) {
            return MachineStatus.BLOCKED;
        }
        return switch (kineticStatus) {
            case OK -> MachineStatus.RUNNING;
            case UNDERPOWERED -> MachineStatus.BROWNOUT;
            case NO_INPUT, DISABLED -> MachineStatus.NO_POWER;
            default -> MachineStatus.NO_POWER; // OVERLOADED, CONFLICT: treat as no usable power
        };
    }
}
