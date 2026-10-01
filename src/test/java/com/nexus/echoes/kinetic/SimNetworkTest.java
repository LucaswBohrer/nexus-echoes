package com.nexus.echoes.kinetic;

import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.kinetic.sim.KineticSnapshot;
import com.nexus.echoes.kinetic.sim.NodeState;
import com.nexus.echoes.kinetic.sim.SimNetwork;
import com.nexus.echoes.kinetic.sim.SimNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the network simulator: propagation, ratios, direction,
 * branching, overload, edge cases and snapshot persistence.
 *
 * <p>Networks are built by hand from {@link SimNode}s — no Minecraft needed.
 */
class SimNetworkTest {

    private static final double DELTA = 1e-6;

    private static Map<String, SimNode> net(SimNode... nodes) {
        Map<String, SimNode> map = new HashMap<>();
        for (SimNode n : nodes) {
            map.put(n.id, n);
        }
        return map;
    }

    // ---------------------------------------------------------- happy paths

    @Test
    void sourceShaftConsumerRunsAtRatedValues() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, true, List.of("shaft")),
                SimNode.shaft("shaft", List.of("gen", "machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(30),
                        List.of("shaft"))));

        KineticSnapshot snap = network.simulate(-1, 0);

        assertEquals(NodeStatus.OK, snap.stateOf("gen").status());
        assertEquals(NodeStatus.OK, snap.stateOf("shaft").status());
        NodeState m = snap.stateOf("machine");
        assertEquals(NodeStatus.OK, m.status());
        assertEquals(120.0, m.rpm().value(), DELTA);
        // source has headroom: consumer gets its full required torque
        assertEquals(30.0, m.torque().newtonMeters(), DELTA);
        assertEquals(RotationDirection.CLOCKWISE, m.direction());
        assertFalse(snap.overloaded());
        assertFalse(snap.conflict());
    }

    @Test
    void gearboxRatioTransformsSpeedAndTorque() {
        // 60 RPM + 100 Nm -> 2:1 step-up -> 120 RPM + 50 Nm at the consumer
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(60), Torque.ofNewtonMeters(100),
                        RotationDirection.CLOCKWISE, true, List.of("box")),
                SimNode.gearbox("box", 2.0, 1.0, List.of("gen", "machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(50),
                        List.of("box"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        NodeState m = snap.stateOf("machine");
        assertEquals(NodeStatus.OK, m.status());
        assertEquals(120.0, m.rpm().value(), DELTA);
        assertEquals(50.0, m.torque().newtonMeters(), DELTA);
    }

    @Test
    void gearboxReductionDoublesTorque() {
        // 120 RPM + 50 Nm -> 1:2 reduction -> 60 RPM + 100 Nm
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, true, List.of("box")),
                SimNode.gearbox("box", 0.5, 1.0, List.of("gen", "machine")),
                SimNode.consumer("machine", Rpm.of(60), Torque.ofNewtonMeters(100),
                        List.of("box"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        NodeState m = snap.stateOf("machine");
        assertEquals(NodeStatus.OK, m.status());
        assertEquals(60.0, m.rpm().value(), DELTA);
        assertEquals(100.0, m.torque().newtonMeters(), DELTA);
    }

    @Test
    void meshingGearsApplyTeethRatioAndFlipDirection() {
        // 12-tooth gear driving a 24-tooth gear: halve speed, double torque, flip
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(25),
                        RotationDirection.CLOCKWISE, true, List.of("ga")),
                SimNode.gear("ga", 12, List.of("gen", "gb")),
                SimNode.gear("gb", 24, List.of("ga", "machine")),
                SimNode.consumer("machine", Rpm.of(60), Torque.ofNewtonMeters(40),
                        List.of("gb"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        NodeState gb = snap.stateOf("gb");
        assertEquals(60.0, gb.rpm().value(), DELTA);
        assertEquals(50.0, gb.torque().newtonMeters(), DELTA);
        assertEquals(RotationDirection.COUNTERCLOCKWISE, gb.direction());
        NodeState m = snap.stateOf("machine");
        assertEquals(NodeStatus.OK, m.status());
        assertEquals(RotationDirection.COUNTERCLOCKWISE, m.direction());
    }

    @Test
    void gearOnShaftDoesNotFlipDirection() {
        // gear carried on the same axle (driven by a shaft, not meshing): no flip
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(25),
                        RotationDirection.CLOCKWISE, true, List.of("shaft")),
                SimNode.shaft("shaft", List.of("gen", "ga")),
                SimNode.gear("ga", 24, List.of("shaft", "machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(20),
                        List.of("ga"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertEquals(RotationDirection.CLOCKWISE, snap.stateOf("ga").direction());
        assertEquals(NodeStatus.OK, snap.stateOf("machine").status());
    }

    // -------------------------------------------------------------- overload

    @Test
    void sourceWeakerThanDemandOverloadsWithBrownout() {
        // spec example: motor 120 RPM / 20 Nm, machine needs 120 RPM / 40 Nm
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(20),
                        RotationDirection.CLOCKWISE, true, List.of("machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(40),
                        List.of("gen"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertTrue(snap.overloaded());
        assertEquals(NodeStatus.OVERLOADED, snap.stateOf("gen").status());
        NodeState m = snap.stateOf("machine");
        assertEquals(NodeStatus.UNDERPOWERED, m.status());
        // brownout: 20/40 of the required torque
        assertEquals(20.0, m.torque().newtonMeters(), DELTA);
        assertTrue(snap.loadFactor() > 1.0);
    }

    @Test
    void sourceExactlyMeetingDemandIsNotOverloaded() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(40),
                        RotationDirection.CLOCKWISE, true, List.of("machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(40),
                        List.of("gen"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertFalse(snap.overloaded());
        assertEquals(NodeStatus.OK, snap.stateOf("machine").status());
        assertEquals(1.0, snap.loadFactor(), DELTA);
    }

    @Test
    void sourceStrongerThanDemandRunsFine() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(100),
                        RotationDirection.CLOCKWISE, true, List.of("machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(40),
                        List.of("gen"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertFalse(snap.overloaded());
        assertEquals(NodeStatus.OK, snap.stateOf("machine").status());
        assertEquals(0.4, snap.loadFactor(), DELTA);
    }

    @Test
    void multipleConsumersShareSourceTorque() {
        // 60 Nm source, two 20 Nm consumers: fine
        SimNetwork ok = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(60),
                        RotationDirection.CLOCKWISE, true, List.of("gear")),
                SimNode.gear("gear", 12, List.of("gen", "a", "b")),
                SimNode.consumer("a", Rpm.of(120), Torque.ofNewtonMeters(20), List.of("gear")),
                SimNode.consumer("b", Rpm.of(120), Torque.ofNewtonMeters(20), List.of("gear"))));
        KineticSnapshot okSnap = ok.simulate(-1, 0);
        assertFalse(okSnap.overloaded());
        assertEquals(NodeStatus.OK, okSnap.stateOf("a").status());
        assertEquals(NodeStatus.OK, okSnap.stateOf("b").status());

        // 60 Nm source, two 40 Nm consumers: demand 80 > 60 -> both brown out
        SimNetwork over = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(60),
                        RotationDirection.CLOCKWISE, true, List.of("gear")),
                SimNode.gear("gear", 12, List.of("gen", "a", "b")),
                SimNode.consumer("a", Rpm.of(120), Torque.ofNewtonMeters(40), List.of("gear")),
                SimNode.consumer("b", Rpm.of(120), Torque.ofNewtonMeters(40), List.of("gear"))));
        KineticSnapshot overSnap = over.simulate(-1, 0);
        assertTrue(overSnap.overloaded());
        assertEquals(NodeStatus.UNDERPOWERED, overSnap.stateOf("a").status());
        assertEquals(NodeStatus.UNDERPOWERED, overSnap.stateOf("b").status());
        assertEquals(30.0, overSnap.stateOf("a").torque().newtonMeters(), DELTA);
    }

    // ------------------------------------------------------------ edge cases

    @Test
    void noConsumerMeansZeroLoad() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, true, List.of("shaft")),
                SimNode.shaft("shaft", List.of("gen"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertEquals(NodeStatus.OK, snap.stateOf("gen").status());
        assertEquals(0.0, snap.loadFactor(), DELTA);
        assertEquals(0.0, snap.totalDemandPowerW(), DELTA);
        assertFalse(snap.overloaded());
    }

    @Test
    void zeroRpmSourceStallsConsumersButKeepsLink() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(0), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, true, List.of("shaft")),
                SimNode.shaft("shaft", List.of("gen", "machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(30),
                        List.of("shaft"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertEquals(0.0, snap.stateOf("shaft").rpm().value(), DELTA);
        assertEquals(NodeStatus.OK, snap.stateOf("shaft").status()); // linked, not spinning
        assertEquals(NodeStatus.UNDERPOWERED, snap.stateOf("machine").status());
    }

    @Test
    void zeroTorqueSourceCannotDriveAnything() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(0),
                        RotationDirection.CLOCKWISE, true, List.of("machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(30),
                        List.of("gen"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertEquals(NodeStatus.UNDERPOWERED, snap.stateOf("machine").status());
        assertEquals(0.0, snap.stateOf("machine").power().watts(), DELTA);
    }

    @Test
    void disabledSourceGivesNoInput() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, false, List.of("machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(30),
                        List.of("gen"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertEquals(NodeStatus.DISABLED, snap.stateOf("gen").status());
        assertEquals(NodeStatus.NO_INPUT, snap.stateOf("machine").status());
        assertEquals(0.0, snap.totalSupplyPowerW(), DELTA);
    }

    @Test
    void disengagedClutchBlocksFlow() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, true, List.of("clutch")),
                SimNode.clutch("clutch", false, List.of("gen", "machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(30),
                        List.of("clutch"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertEquals(NodeStatus.NO_INPUT, snap.stateOf("machine").status());

        SimNetwork engaged = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, true, List.of("clutch")),
                SimNode.clutch("clutch", true, List.of("gen", "machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(30),
                        List.of("clutch"))));
        assertEquals(NodeStatus.OK, engaged.simulate(-1, 0).stateOf("machine").status());
    }

    @Test
    void consumerNeedingMoreRpmThanDeliveredStalls() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(60), Torque.ofNewtonMeters(200),
                        RotationDirection.CLOCKWISE, true, List.of("machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(30),
                        List.of("gen"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertEquals(NodeStatus.UNDERPOWERED, snap.stateOf("machine").status());
    }

    @Test
    void consumerAcceptsOverspeed() {
        // Phase 2 rule: machines run at or above their rated RPM
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(240), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, true, List.of("machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(30),
                        List.of("gen"))));

        assertEquals(NodeStatus.OK, network.simulate(-1, 0).stateOf("machine").status());
    }

    @Test
    void twoSourcesOnOneNodeIsAConflict() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("a_gen", Rpm.of(60), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, true, List.of("shaft")),
                SimNode.source("b_gen", Rpm.of(240), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, true, List.of("shaft")),
                SimNode.shaft("shaft", List.of("a_gen", "b_gen"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertTrue(snap.conflict());
        // deterministic: lowest id wins
        assertEquals(60.0, snap.stateOf("shaft").rpm().value(), DELTA);
    }

    @Test
    void danglingNeighborIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SimNetwork(net(
                SimNode.shaft("shaft", List.of("ghost")))));
    }

    @Test
    void invalidGearTeethRejected() {
        assertThrows(IllegalArgumentException.class, () -> SimNode.gear("g", 0, List.of()));
    }

    // ---------------------------------------------------------- persistence

    @Test
    void snapshotSerializesAndRestores() {
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                        RotationDirection.COUNTERCLOCKWISE, true, List.of("box")),
                SimNode.gearbox("box", 2.0, 1.0, List.of("gen", "machine")),
                SimNode.consumer("machine", Rpm.of(240), Torque.ofNewtonMeters(20),
                        List.of("box"))));

        KineticSnapshot original = network.simulate(7, 1234);
        KineticSnapshot restored = KineticSnapshot.fromMap(original.toMap());

        assertEquals(original.networkId(), restored.networkId());
        assertEquals(original.simTick(), restored.simTick());
        assertEquals(original.states(), restored.states());
        assertEquals(original.totalSupplyPowerW(), restored.totalSupplyPowerW(), DELTA);
        assertEquals(original.totalDemandPowerW(), restored.totalDemandPowerW(), DELTA);
        assertEquals(original.loadFactor(), restored.loadFactor(), DELTA);
        assertEquals(original.overloaded(), restored.overloaded());
        assertEquals(original.conflict(), restored.conflict());
    }

    @Test
    void powerIsDerivedConsistently() {
        // 120 RPM + 50 Nm at the source = 200*pi W supply
        SimNetwork network = new SimNetwork(net(
                SimNode.source("gen", Rpm.of(120), Torque.ofNewtonMeters(50),
                        RotationDirection.CLOCKWISE, true, List.of("machine")),
                SimNode.consumer("machine", Rpm.of(120), Torque.ofNewtonMeters(50),
                        List.of("gen"))));

        KineticSnapshot snap = network.simulate(-1, 0);
        assertEquals(200 * Math.PI, snap.totalSupplyPowerW(), 1e-6);
        assertEquals(200 * Math.PI, snap.totalDemandPowerW(), 1e-6);
        assertEquals(200 * Math.PI, snap.stateOf("gen").power().watts(), 1e-6);
    }
}
