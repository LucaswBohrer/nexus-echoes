package com.nexus.echoes.kinetic.sim;

import com.nexus.echoes.kinetic.KineticMath;
import com.nexus.echoes.kinetic.Power;
import com.nexus.echoes.kinetic.RotationDirection;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.KineticFlow;
import com.nexus.echoes.kinetic.api.NodeRole;
import com.nexus.echoes.kinetic.api.NodeStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simulates one connected kinetic network: propagates mechanical flow from
 * sources through transmissions to consumers, then resolves supply vs demand.
 *
 * <p>The model, in short:
 * <ul>
 *   <li>Sources are ideal constant-RPM producers (Phase 2, ADR-007).</li>
 *   <li>Flow propagates downstream; transmissions transform (rpm, torque,
 *       direction) via {@link KineticMath#applyRatio}. Gears meshing with
 *       gears apply teeth ratio and flip direction.</li>
 *   <li>At branches every downstream path sees the full torque — contention
 *       is resolved by the overload rule, not by splitting.</li>
 *   <li>Overload: if Σ consumer demand (referred to a source) exceeds the
 *       source's rated torque, every claimed consumer is scaled by
 *       supply/demand (brownout) and the source is flagged OVERLOADED.</li>
 *   <li>Consumers are sinks: flow never propagates through them.</li>
 *   <li>Multiple enabled sources feeding the same node = conflict: the
 *       lowest-id source owns the node (deterministic Phase 2 rule).</li>
 * </ul>
 *
 * <p>Pure Java — no Minecraft. The Minecraft layer builds {@link SimNode}s
 * from blocks/block entities; tests build them by hand.
 */
public final class SimNetwork {

    /** Floating-point tolerance for >= comparisons. */
    private static final double EPS = 1e-9;

    private final Map<String, SimNode> nodes;

    public SimNetwork(Map<String, SimNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Network must have at least one node");
        }
        for (SimNode node : nodes.values()) {
            for (String neighbor : node.neighbors) {
                if (!nodes.containsKey(neighbor)) {
                    throw new IllegalArgumentException(
                            "Node '" + node.id + "' references unknown neighbor '" + neighbor + "'");
                }
                if (neighbor.equals(node.id)) {
                    throw new IllegalArgumentException("Node '" + node.id + "' lists itself as neighbor");
                }
            }
        }
        this.nodes = Map.copyOf(nodes);
    }

    /**
     * Runs one full simulation pass.
     *
     * @param networkId id assigned by the caller (manager); -1 when standalone
     * @param simTick   monotonically increasing tick of the caller
     */
    public KineticSnapshot simulate(int networkId, long simTick) {
        Pass pass = new Pass();
        pass.propagate();
        return pass.resolve(networkId, simTick);
    }

    // ------------------------------------------------------------ internals

    /** Flow as seen at one node during propagation. */
    private record Visit(KineticFlow flow, double torqueGain, String sourceId) {
    }

    private record Claim(SimNode consumer, KineticFlow flow, double torqueGain, SimNode source) {
    }

    private final class Pass {
        final Map<String, Visit> visits = new HashMap<>();
        final Map<String, String> owner = new HashMap<>();
        final Map<String, Claim> claims = new HashMap<>();
        final List<SimNode> sources = new ArrayList<>();
        boolean conflict = false;

        void propagate() {
            for (SimNode node : nodes.values()) {
                if (node.role == NodeRole.SOURCE) {
                    sources.add(node);
                }
            }
            sources.sort(Comparator.comparing(s -> s.id));

            for (SimNode source : sources) {
                if (!source.enabled) {
                    continue;
                }
                KineticFlow flow = KineticFlow.of(source.ratedRpm, source.ratedTorque, source.ratedDirection);
                traverse(source, source.id, flow, 1.0, null, new HashSet<>());
            }
        }

        void traverse(SimNode source, String currentId, KineticFlow flow,
                      double torqueGain, String fromId, Set<String> seenThisSource) {
            if (!seenThisSource.add(currentId)) {
                return;
            }
            String other = owner.get(currentId);
            if (other != null && !other.equals(source.id)) {
                conflict = true;
                return; // owned by a higher-priority source: flow stops here
            }
            owner.put(currentId, source.id);
            SimNode node = nodes.get(currentId);
            visits.put(currentId, new Visit(flow, torqueGain, source.id));

            if (node.role == NodeRole.CONSUMER) {
                claims.putIfAbsent(currentId, new Claim(node, flow, torqueGain, source));
                return; // consumers are sinks
            }
            if (node.role == NodeRole.SOURCE && !currentId.equals(source.id)) {
                conflict = true;
                return; // flow never passes through another source
            }

            for (String nextId : node.neighbors) {
                if (nextId.equals(fromId)) {
                    continue;
                }
                // The transform belongs to the node being ENTERED (nextId);
                // the current node is the driver (matters for gear meshing).
                SimNode next = nodes.get(nextId);
                KineticFlow out = transformThrough(next, node, flow);
                if (out == null) {
                    continue; // blocked (disengaged clutch)
                }
                double gain = torqueGain * edgeTorqueGain(next, node);
                traverse(source, nextId, out, gain, currentId, seenThisSource);
            }
        }

        /** Applies this transmission node's transform to a flow arriving from {@code from}. */
        KineticFlow transformThrough(SimNode node, SimNode from, KineticFlow flow) {
            if (node.role != NodeRole.TRANSMISSION) {
                return flow; // sources and consumers don't transform
            }
            return switch (node.kind) {
                case SHAFT -> flow;
                case CLUTCH -> node.engaged ? flow : null;
                case GEARBOX -> {
                    KineticMath.RatioResult r = KineticMath.applyRatio(
                            flow.rpm(), flow.torque(), node.ratio, node.efficiency);
                    yield KineticFlow.of(r.rpm(), r.torque(), flow.direction());
                }
                case GEAR -> {
                    boolean meshing = from != null
                            && from.role == NodeRole.TRANSMISSION
                            && from.kind == SimNode.TransmissionKind.GEAR;
                    double ratio = meshing ? (double) from.teeth / node.teeth : 1.0;
                    RotationDirection dir = meshing ? flow.direction().opposite() : flow.direction();
                    KineticMath.RatioResult r = KineticMath.applyRatio(
                            flow.rpm(), flow.torque(), ratio, node.efficiency);
                    yield KineticFlow.of(r.rpm(), r.torque(), dir);
                }
            };
        }

        /** Torque multiplier contributed by passing through {@code node} from {@code from}. */
        double edgeTorqueGain(SimNode node, SimNode from) {
            if (node.role != NodeRole.TRANSMISSION) {
                return 1.0;
            }
            return switch (node.kind) {
                case SHAFT -> 1.0;
                case CLUTCH -> 1.0;
                case GEARBOX -> node.efficiency / node.ratio;
                case GEAR -> {
                    boolean meshing = from != null
                            && from.role == NodeRole.TRANSMISSION
                            && from.kind == SimNode.TransmissionKind.GEAR;
                    double ratio = meshing ? (double) from.teeth / node.teeth : 1.0;
                    yield node.efficiency / ratio;
                }
            };
        }

        KineticSnapshot resolve(int networkId, long simTick) {
            Map<String, NodeState> states = new HashMap<>();

            // per-source demand referred back to the source shaft
            Map<String, Double> demandBySource = new HashMap<>();
            Map<String, Double> scaleBySource = new HashMap<>();
            Map<String, Boolean> overloadedBySource = new HashMap<>();
            for (SimNode source : sources) {
                demandBySource.put(source.id, 0.0);
            }
            for (Claim claim : claims.values()) {
                double demand = KineticMath.demandAtSource(
                        claim.consumer.requiredTorque, claim.torqueGain).newtonMeters();
                demandBySource.merge(claim.source.id, demand, Double::sum);
            }
            for (SimNode source : sources) {
                if (!source.enabled) {
                    overloadedBySource.put(source.id, false);
                    scaleBySource.put(source.id, 1.0);
                    continue;
                }
                double supply = source.ratedTorque.newtonMeters();
                double demand = demandBySource.get(source.id);
                boolean overloaded = demand > supply + EPS;
                overloadedBySource.put(source.id, overloaded);
                scaleBySource.put(source.id, overloaded ? supply / demand : 1.0);
            }

            double totalSupplyW = 0.0;
            double totalDemandW = 0.0;

            for (SimNode node : nodes.values()) {
                states.put(node.id, resolveNode(node, scaleBySource, overloadedBySource));
                if (node.role == NodeRole.SOURCE && node.enabled) {
                    totalSupplyW += KineticMath.toPower(node.ratedRpm, node.ratedTorque).watts();
                }
            }
            for (Claim claim : claims.values()) {
                totalDemandW += KineticMath.toPower(
                        claim.consumer.requiredRpm, claim.consumer.requiredTorque).watts();
            }

            boolean anyOverloaded = overloadedBySource.values().stream().anyMatch(b -> b);
            double loadFactor = totalSupplyW > EPS
                    ? totalDemandW / totalSupplyW
                    : (totalDemandW > EPS ? Double.POSITIVE_INFINITY : 0.0);

            return new KineticSnapshot(networkId, simTick, states,
                    totalSupplyW, totalDemandW, loadFactor, anyOverloaded, conflict);
        }

        NodeState resolveNode(SimNode node, Map<String, Double> scaleBySource,
                              Map<String, Boolean> overloadedBySource) {
            return switch (node.role) {
                case SOURCE -> {
                    if (!node.enabled) {
                        yield NodeState.of(Rpm.ZERO, Torque.ZERO, node.ratedDirection, NodeStatus.DISABLED);
                    }
                    NodeStatus s = overloadedBySource.get(node.id) ? NodeStatus.OVERLOADED : NodeStatus.OK;
                    yield NodeState.of(node.ratedRpm, node.ratedTorque, node.ratedDirection, s);
                }
                case TRANSMISSION -> {
                    Visit v = visits.get(node.id);
                    if (v == null) {
                        yield NodeState.of(Rpm.ZERO, Torque.ZERO,
                                RotationDirection.CLOCKWISE, NodeStatus.NO_INPUT);
                    }
                    yield NodeState.of(v.flow.rpm(), v.flow.torque(), v.flow.direction(), NodeStatus.OK);
                }
                case CONSUMER -> {
                    Claim c = claims.get(node.id);
                    if (c == null) {
                        yield NodeState.of(Rpm.ZERO, Torque.ZERO,
                                RotationDirection.CLOCKWISE, NodeStatus.NO_INPUT);
                    }
                    double scale = scaleBySource.get(c.source.id);
                    // Brownout scales the consumer's requirement; the transmission
                    // caps what can physically arrive from the source shaft.
                    Torque deliveredTorque = Torque.ofNewtonMeters(
                            Math.min(c.consumer.requiredTorque.newtonMeters() * scale,
                                    c.source.ratedTorque.newtonMeters() * c.torqueGain));
                    boolean rpmOk = c.flow.rpm().value() + EPS >= node.requiredRpm.value();
                    boolean torqueOk = deliveredTorque.newtonMeters() + EPS >= node.requiredTorque.newtonMeters();
                    NodeStatus s = (rpmOk && torqueOk) ? NodeStatus.OK : NodeStatus.UNDERPOWERED;
                    yield NodeState.of(c.flow.rpm(), deliveredTorque, c.flow.direction(), s);
                }
            };
        }
    }
}
