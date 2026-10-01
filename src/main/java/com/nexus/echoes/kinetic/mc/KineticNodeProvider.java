package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.kinetic.RotationDirection;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeRole;
import com.nexus.echoes.kinetic.sim.NodeState;
import com.nexus.echoes.kinetic.sim.SimNode;

/**
 * Implemented by block entities that take part in a kinetic network.
 *
 * <p>The {@link KineticManager} discovers providers by block position, builds
 * {@link SimNode}s from them and feeds back the resulting {@link NodeState}
 * after every re-simulation. Only the role-relevant getters need overriding;
 * the rest have harmless defaults.
 */
public interface KineticNodeProvider {

    NodeRole getKineticRole();

    // ---------------------------------------------------------------- sources

    default Rpm getRatedRpm() {
        return Rpm.ZERO;
    }

    default Torque getRatedTorque() {
        return Torque.ZERO;
    }

    default RotationDirection getRatedDirection() {
        return RotationDirection.CLOCKWISE;
    }

    /** False while e.g. powered by redstone. */
    default boolean isKineticEnabled() {
        return true;
    }

    // ----------------------------------------------------------- transmission

    default SimNode.TransmissionKind getTransmissionKind() {
        return SimNode.TransmissionKind.SHAFT;
    }

    /** Gearbox speed ratio (output:input). 2.0 doubles speed, 0.5 halves it. */
    default double getGearboxRatio() {
        return 1.0;
    }

    /** Gearbox efficiency in (0, 1]; 1.0 = ideal (Phase 2 default). */
    default double getEfficiency() {
        return 1.0;
    }

    default int getGearTeeth() {
        return 12;
    }

    default boolean isClutchEngaged() {
        return true;
    }

    // -------------------------------------------------------------- consumers

    default Rpm getRequiredRpm() {
        return Rpm.ZERO;
    }

    default Torque getRequiredTorque() {
        return Torque.ZERO;
    }

    /**
     * Called on the server after every network re-simulation with this node's
     * fresh state. The provider persists it and uses it for logic/animation.
     */
    void onKineticSnapshot(NodeState state);
}
