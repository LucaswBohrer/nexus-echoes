package com.nexus.echoes.kinetic.sim;

import com.nexus.echoes.kinetic.KineticMath;
import com.nexus.echoes.kinetic.Power;
import com.nexus.echoes.kinetic.RotationDirection;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The resolved mechanical state of one node after simulation. Immutable.
 */
public final class NodeState {

    private final Rpm rpm;
    private final Torque torque;
    private final RotationDirection direction;
    private final NodeStatus status;

    private NodeState(Rpm rpm, Torque torque, RotationDirection direction, NodeStatus status) {
        this.rpm = rpm;
        this.torque = torque;
        this.direction = direction;
        this.status = status;
    }

    public static NodeState of(Rpm rpm, Torque torque, RotationDirection direction, NodeStatus status) {
        return new NodeState(
                Objects.requireNonNull(rpm, "rpm"),
                Objects.requireNonNull(torque, "torque"),
                Objects.requireNonNull(direction, "direction"),
                Objects.requireNonNull(status, "status"));
    }

    public Rpm rpm() {
        return rpm;
    }

    public Torque torque() {
        return torque;
    }

    public RotationDirection direction() {
        return direction;
    }

    public NodeStatus status() {
        return status;
    }

    /** Derived power at this node. */
    public Power power() {
        return KineticMath.toPower(rpm, torque);
    }

    public boolean isRunning() {
        return status == NodeStatus.OK;
    }

    // ---------------------------------------------------------- serialization

    /** Plain-map form (string/double keys) for persistence round-trip tests. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("rpm", rpm.value());
        map.put("torque", torque.newtonMeters());
        map.put("direction", direction.name());
        map.put("status", status.name());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static NodeState fromMap(Map<String, Object> map) {
        return NodeState.of(
                Rpm.of(((Number) map.get("rpm")).doubleValue()),
                Torque.ofNewtonMeters(((Number) map.get("torque")).doubleValue()),
                RotationDirection.valueOf((String) map.get("direction")),
                NodeStatus.valueOf((String) map.get("status")));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeState other)) {
            return false;
        }
        return rpm.equals(other.rpm) && torque.equals(other.torque)
                && direction == other.direction && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rpm, torque, direction, status);
    }

    @Override
    public String toString() {
        return rpm + " / " + torque + " / " + direction + " [" + status + "]";
    }
}
