package com.nexus.echoes.kinetic;

import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.kinetic.sim.KineticSnapshot;
import com.nexus.echoes.kinetic.sim.NodeState;
import com.nexus.echoes.kinetic.sim.SimNetwork;
import com.nexus.echoes.kinetic.sim.SimNode;
import com.nexus.echoes.machines.kinetic.MachineStatus;
import com.nexus.echoes.machines.kinetic.ProcessingGovernor;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3 integration: the full industrial chain as a pure simulation —
 * Generator → Shaft → Crusher, Generator → Gearbox(2:1) → Processor.
 *
 * <p>Proves the Phase 2 network can feed real machines with different
 * mechanical requirements, and that overload/brownout propagate to machine
 * behavior through the governor.
 */
class KineticChainIntegrationTest {

    /**
     * Builds the reference chain:
     * gen(120rpm/50Nm) — shaft — crusher(120/20)
     *                        \— gearbox(2.0) — shaft — processor(240/15)
     */
    private static SimNetwork chain(boolean withResonator) {
        Map<String, SimNode> nodes = new HashMap<>();
        nodes.put("gen", SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                RotationDirection.CLOCKWISE, true, List.of("shaft1")));
        nodes.put("shaft1", SimNode.shaft("shaft1", List.of("gen", "crusher", "gearbox")));
        nodes.put("crusher", SimNode.consumer("crusher", Rpm.of(120), Torque.ofNewtonMeters(20),
                List.of("shaft1")));
        nodes.put("gearbox", SimNode.gearbox("gearbox", 2.0, 1.0, List.of("shaft1", "shaft2")));
        nodes.put("shaft2", SimNode.shaft("shaft2", List.of("gearbox", "processor")));
        nodes.put("processor", SimNode.consumer("processor", Rpm.of(240), Torque.ofNewtonMeters(15),
                List.of("shaft2")));
        if (withResonator) {
            // third consumer on the same trunk: pushes demand past supply
            nodes.put("shaft1", SimNode.shaft("shaft1",
                    List.of("gen", "crusher", "gearbox", "resonator")));
            nodes.put("resonator", SimNode.consumer("resonator", Rpm.of(120), Torque.ofNewtonMeters(30),
                    List.of("shaft1")));
        }
        return new SimNetwork(nodes);
    }

    @Test
    void chainFeedsBothMachines() {
        KineticSnapshot snap = chain(false).simulate(0, 1);

        NodeState crusher = snap.states().get("crusher");
        assertEquals(NodeStatus.OK, crusher.status());
        assertEquals(120.0, crusher.rpm().value(), 1e-9);
        assertEquals(20.0, crusher.torque().newtonMeters(), 1e-9);

        NodeState processor = snap.states().get("processor");
        assertEquals(NodeStatus.OK, processor.status());
        assertEquals(240.0, processor.rpm().value(), 1e-9);
        assertEquals(15.0, processor.torque().newtonMeters(), 1e-9);

        assertFalse(snap.overloaded());
        // demand referred to source: 20 (crusher, gain 1) + 15/0.5 (processor, gain 0.5) = 50 = supply
        assertEquals(1.0, snap.loadFactor(), 1e-9);
    }

    @Test
    void machinesRunAtFullSpeedWhenChainIsHealthy() {
        KineticSnapshot snap = chain(false).simulate(0, 1);
        NodeState crusher = snap.states().get("crusher");
        double factor = ProcessingGovernor.speedFactor(
                crusher.status(), crusher.rpm(), Rpm.of(120),
                crusher.torque(), Torque.ofNewtonMeters(20));
        assertEquals(1.0, factor);
        assertEquals(MachineStatus.RUNNING,
                ProcessingGovernor.deriveStatus(true, false, crusher.status()));
    }

    @Test
    void thirdMachineOverloadsTheNetwork() {
        // demand: 20 + 30 + 15/0.5 = 80 > 50 supply
        KineticSnapshot snap = chain(true).simulate(0, 1);
        assertTrue(snap.overloaded());
        assertEquals(80.0 / 50.0, snap.loadFactor(), 1e-9);

        double scale = 50.0 / 80.0;
        for (String id : List.of("crusher", "processor", "resonator")) {
            NodeState s = snap.states().get(id);
            assertEquals(NodeStatus.UNDERPOWERED, s.status(), id);
        }

        // governor: brownout slows work proportionally
        NodeState crusher = snap.states().get("crusher");
        double factor = ProcessingGovernor.speedFactor(
                crusher.status(), crusher.rpm(), Rpm.of(120),
                crusher.torque(), Torque.ofNewtonMeters(20));
        assertEquals(scale, factor, 1e-9);
        assertEquals(MachineStatus.BROWNOUT,
                ProcessingGovernor.deriveStatus(true, false, crusher.status()));
    }

    @Test
    void processorStallsWithoutGearbox() {
        // direct drive: 120 rpm < 240 required -> UNDERPOWERED, machine stalls
        Map<String, SimNode> nodes = new HashMap<>();
        nodes.put("gen", SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                RotationDirection.CLOCKWISE, true, List.of("processor")));
        nodes.put("processor", SimNode.consumer("processor", Rpm.of(240), Torque.ofNewtonMeters(15),
                List.of("gen")));
        KineticSnapshot snap = new SimNetwork(nodes).simulate(0, 1);

        NodeState p = snap.states().get("processor");
        assertEquals(NodeStatus.UNDERPOWERED, p.status());
        // 120/240 rpm with full torque -> brownout crawls at half speed
        double factor = ProcessingGovernor.speedFactor(
                p.status(), p.rpm(), Rpm.of(240),
                p.torque(), Torque.ofNewtonMeters(15));
        assertEquals(0.5, factor, 1e-9);
    }

    @Test
    void disengagedClutchStarvesMachine() {
        Map<String, SimNode> nodes = new HashMap<>();
        nodes.put("gen", SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                RotationDirection.CLOCKWISE, true, List.of("clutch")));
        nodes.put("clutch", SimNode.clutch("clutch", false, List.of("gen", "crusher")));
        nodes.put("crusher", SimNode.consumer("crusher", Rpm.of(120), Torque.ofNewtonMeters(20),
                List.of("clutch")));
        KineticSnapshot snap = new SimNetwork(nodes).simulate(0, 1);

        NodeState c = snap.states().get("crusher");
        assertEquals(NodeStatus.NO_INPUT, c.status());
        assertEquals(MachineStatus.NO_POWER,
                ProcessingGovernor.deriveStatus(true, false, c.status()));
        assertEquals(0.0, ProcessingGovernor.speedFactor(
                c.status(), c.rpm(), Rpm.of(120),
                c.torque(), Torque.ofNewtonMeters(20)));
    }
}
