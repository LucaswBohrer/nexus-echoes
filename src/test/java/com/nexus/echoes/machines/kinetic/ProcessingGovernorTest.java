package com.nexus.echoes.machines.kinetic;

import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for the brownout governor (Phase 3).
 * No Minecraft classes — the rules are validated without a game instance.
 */
class ProcessingGovernorTest {

    private static final Rpm REQ_RPM = Rpm.of(120);
    private static final Torque REQ_TQ = Torque.ofNewtonMeters(20);

    private static double factor(NodeStatus s, double rpm, double tq) {
        return ProcessingGovernor.speedFactor(s, Rpm.of(rpm), REQ_RPM, Torque.ofNewtonMeters(tq), REQ_TQ);
    }

    @Test
    void okStatusRunsAtFullSpeed() {
        assertEquals(1.0, factor(NodeStatus.OK, 120, 20));
    }

    @Test
    void brownoutScalesWithTorque() {
        assertEquals(0.5, factor(NodeStatus.UNDERPOWERED, 120, 10), 1e-9);
        assertEquals(0.25, factor(NodeStatus.UNDERPOWERED, 120, 5), 1e-9);
    }

    @Test
    void brownoutScalesWithRpm() {
        // torque fine, half the RPM -> half speed
        assertEquals(0.5, factor(NodeStatus.UNDERPOWERED, 60, 20), 1e-9);
    }

    @Test
    void brownoutTakesTheScarcestResource() {
        // half torque AND quarter rpm -> quarter speed
        assertEquals(0.25, factor(NodeStatus.UNDERPOWERED, 30, 10), 1e-9);
    }

    @Test
    void brownoutNeverExceedsFullSpeed() {
        assertEquals(1.0, factor(NodeStatus.UNDERPOWERED, 240, 40));
    }

    @Test
    void noInputStalls() {
        assertEquals(0.0, factor(NodeStatus.NO_INPUT, 0, 0));
        assertEquals(0.0, factor(NodeStatus.DISABLED, 0, 0));
        assertEquals(0.0, factor(NodeStatus.CONFLICT, 120, 20));
    }

    @Test
    void deriveRunning() {
        assertEquals(MachineStatus.RUNNING,
                ProcessingGovernor.deriveStatus(true, false, NodeStatus.OK));
    }

    @Test
    void deriveIdleWhenNoWork() {
        assertEquals(MachineStatus.IDLE,
                ProcessingGovernor.deriveStatus(false, false, NodeStatus.OK));
        assertEquals(MachineStatus.IDLE,
                ProcessingGovernor.deriveStatus(false, true, NodeStatus.OK));
    }

    @Test
    void deriveBlockedBeatsPowerStates() {
        assertEquals(MachineStatus.BLOCKED,
                ProcessingGovernor.deriveStatus(true, true, NodeStatus.OK));
        assertEquals(MachineStatus.BLOCKED,
                ProcessingGovernor.deriveStatus(true, true, NodeStatus.NO_INPUT));
    }

    @Test
    void deriveNoPower() {
        assertEquals(MachineStatus.NO_POWER,
                ProcessingGovernor.deriveStatus(true, false, NodeStatus.NO_INPUT));
        assertEquals(MachineStatus.NO_POWER,
                ProcessingGovernor.deriveStatus(true, false, NodeStatus.DISABLED));
    }

    @Test
    void deriveBrownout() {
        assertEquals(MachineStatus.BROWNOUT,
                ProcessingGovernor.deriveStatus(true, false, NodeStatus.UNDERPOWERED));
    }
}
