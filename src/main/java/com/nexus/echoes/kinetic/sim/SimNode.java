package com.nexus.echoes.kinetic.sim;

import com.nexus.echoes.kinetic.RotationDirection;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeRole;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One node of a simulated kinetic network. Pure data — no Minecraft, no
 * behavior. The Minecraft layer ({@code KineticNetworkManager}) builds these
 * from blocks and block entities; tests build them by hand.
 *
 * <p>Only the fields relevant to the node's {@link NodeRole} are meaningful;
 * the static factories make that explicit.
 */
public final class SimNode {

    /** Transmission mechanism. Only meaningful for {@link NodeRole#TRANSMISSION}. */
    public enum TransmissionKind {
        /** Rigid axle. Identity transform, connects along its axis. */
        SHAFT,
        /** Toothed wheel. Meshes with adjacent gears (ratio + direction flip). */
        GEAR,
        /** Configurable ratio box. No direction flip. */
        GEARBOX,
        /** Switchable coupling. Disengaged = flow stops here. */
        CLUTCH
    }

    public final String id;
    public final NodeRole role;
    public final List<String> neighbors;

    // source params
    public final Rpm ratedRpm;
    public final Torque ratedTorque;
    public final RotationDirection ratedDirection;
    public final boolean enabled;

    // consumer params
    public final Rpm requiredRpm;
    public final Torque requiredTorque;

    // transmission params
    public final TransmissionKind kind;
    public final int teeth;
    public final double ratio;
    public final double efficiency;
    public final boolean engaged;

    private SimNode(String id, NodeRole role, List<String> neighbors,
                    Rpm ratedRpm, Torque ratedTorque, RotationDirection ratedDirection, boolean enabled,
                    Rpm requiredRpm, Torque requiredTorque,
                    TransmissionKind kind, int teeth, double ratio, double efficiency, boolean engaged) {
        this.id = Objects.requireNonNull(id, "id");
        this.role = Objects.requireNonNull(role, "role");
        this.neighbors = Collections.unmodifiableList(Objects.requireNonNull(neighbors, "neighbors"));
        this.ratedRpm = ratedRpm;
        this.ratedTorque = ratedTorque;
        this.ratedDirection = ratedDirection;
        this.enabled = enabled;
        this.requiredRpm = requiredRpm;
        this.requiredTorque = requiredTorque;
        this.kind = kind;
        this.teeth = teeth;
        this.ratio = ratio;
        this.efficiency = efficiency;
        this.engaged = engaged;
    }

    public static SimNode source(String id, Rpm ratedRpm, Torque ratedTorque,
                                 RotationDirection direction, boolean enabled, List<String> neighbors) {
        return new SimNode(id, NodeRole.SOURCE, neighbors,
                Objects.requireNonNull(ratedRpm), Objects.requireNonNull(ratedTorque),
                Objects.requireNonNull(direction), enabled,
                null, null, null, 0, 1.0, 1.0, true);
    }

    public static SimNode consumer(String id, Rpm requiredRpm, Torque requiredTorque, List<String> neighbors) {
        return new SimNode(id, NodeRole.CONSUMER, neighbors,
                null, null, null, true,
                Objects.requireNonNull(requiredRpm), Objects.requireNonNull(requiredTorque),
                null, 0, 1.0, 1.0, true);
    }

    public static SimNode shaft(String id, List<String> neighbors) {
        return transmission(id, TransmissionKind.SHAFT, neighbors, 0, 1.0, 1.0, true);
    }

    public static SimNode gear(String id, int teeth, List<String> neighbors) {
        if (teeth <= 0) {
            throw new IllegalArgumentException("Gear teeth must be > 0, got: " + teeth);
        }
        return transmission(id, TransmissionKind.GEAR, neighbors, teeth, 1.0, 1.0, true);
    }

    public static SimNode gearbox(String id, double ratio, double efficiency, List<String> neighbors) {
        if (ratio <= 0.0 || Double.isNaN(ratio) || Double.isInfinite(ratio)) {
            throw new IllegalArgumentException("Gearbox ratio must be finite and > 0, got: " + ratio);
        }
        if (efficiency <= 0.0 || efficiency > 1.0 || Double.isNaN(efficiency)) {
            throw new IllegalArgumentException("Efficiency must be in (0, 1], got: " + efficiency);
        }
        return transmission(id, TransmissionKind.GEARBOX, neighbors, 0, ratio, efficiency, true);
    }

    public static SimNode clutch(String id, boolean engaged, List<String> neighbors) {
        return transmission(id, TransmissionKind.CLUTCH, neighbors, 0, 1.0, 1.0, engaged);
    }

    private static SimNode transmission(String id, TransmissionKind kind, List<String> neighbors,
                                        int teeth, double ratio, double efficiency, boolean engaged) {
        return new SimNode(id, NodeRole.TRANSMISSION, neighbors,
                null, null, null, true,
                null, null, kind, teeth, ratio, efficiency, engaged);
    }
}
