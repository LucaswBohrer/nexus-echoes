package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeRole;
import com.nexus.echoes.kinetic.sim.KineticSnapshot;
import com.nexus.echoes.kinetic.sim.SimNetwork;
import com.nexus.echoes.kinetic.sim.SimNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-side owner of every kinetic network in one {@link ServerLevel}.
 *
 * <p>Providers register on load and unregister on removal; the manager keeps a
 * <b>dirty flag</b> and only rebuilds when the topology (or a node parameter)
 * changed — plus a slow periodic refresh as a safety net. A rebuild:
 * <ol>
 *   <li>splits providers into connected components (6-neighborhood),</li>
 *   <li>runs one {@link SimNetwork} per component,</li>
 *   <li>caches the {@link KineticSnapshot}s,</li>
 *   <li>dispatches each node's {@code NodeState} back to its provider.</li>
 * </ol>
 * The client never simulates: block entities receive their state through the
 * snapshots and sync it to the client with vanilla update packets.
 */
public final class KineticManager {

    private static final Map<ServerLevel, KineticManager> INSTANCES = new WeakHashMap<>();

    /** Slow safety-net refresh; topology changes rebuild immediately. */
    private static final int REFRESH_INTERVAL_TICKS = 100;

    public static KineticManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, KineticManager::new);
    }

    private final ServerLevel level;
    private final Map<BlockPos, KineticNodeProvider> nodes = new HashMap<>();
    private final Map<Integer, KineticSnapshot> snapshots = new HashMap<>();
    /**
     * Environmental derate per source position (0..1, default 1). Set by
     * external systems (e.g. Hollow static-field anomalies) at the adapter
     * layer: the pure simulator only ever sees a source with reduced ratings,
     * so physics rules are unchanged. Transient — never persisted; owners
     * re-apply after reload and clear on expiry.
     */
    private final Map<BlockPos, Double> derates = new HashMap<>();
    private boolean dirty = true;
    private int tick;

    private KineticManager(ServerLevel level) {
        this.level = level;
    }

    public void register(BlockPos pos, KineticNodeProvider provider) {
        nodes.put(pos.immutable(), provider);
        dirty = true;
    }

    public void unregister(BlockPos pos) {
        if (nodes.remove(pos) != null) {
            dirty = true;
        }
    }

    public void markDirty() {
        dirty = true;
    }

    /** Number of registered kinetic nodes (diagnostics). */
    public int nodeCount() {
        return nodes.size();
    }

    /** Provider registered at a position, or {@code null}. Read-only diagnostics. */
    public KineticNodeProvider providerAt(BlockPos pos) {
        return nodes.get(pos);
    }

    /** Last simulated snapshots by network id (diagnostics). */
    public Map<Integer, KineticSnapshot> snapshots() {
        return Collections.unmodifiableMap(snapshots);
    }

    /** Network id of the component containing {@code pos}, or -1. */
    public int networkIdAt(BlockPos pos) {
        String id = nodeId(pos);
        for (Map.Entry<Integer, KineticSnapshot> e : snapshots.entrySet()) {
            if (e.getValue().states().containsKey(id)) {
                return e.getKey();
            }
        }
        return -1;
    }

    public void tick() {
        tick++;
        if (!dirty && tick % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        dirty = false;
        rebuild();
    }

    // ------------------------------------------------------- environmental fx

    /**
     * Sets a temporary output derate for the source at {@code pos}.
     * Factor is clamped to [0, 1]; 1 = no derate. Triggers a rebuild.
     * Owned by the calling system (e.g. the Hollow anomaly manager), which
     * must clear it when the cause ends.
     */
    public void setDerate(BlockPos pos, double factor) {
        double clamped = Math.max(0.0, Math.min(1.0, factor));
        BlockPos key = pos.immutable();
        if (clamped >= 1.0) {
            if (derates.remove(key) != null) {
                dirty = true;
            }
            return;
        }
        if (!Double.valueOf(clamped).equals(derates.put(key, clamped))) {
            dirty = true;
        }
    }

    /** Clears any derate at {@code pos}. */
    public void clearDerate(BlockPos pos) {
        if (derates.remove(pos) != null) {
            dirty = true;
        }
    }

    /** Current derate factor at {@code pos}, 1.0 when none. */
    public double derateAt(BlockPos pos) {
        return derates.getOrDefault(pos, 1.0);
    }

    // -------------------------------------------------------------- internals

    static String nodeId(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private void rebuild() {
        snapshots.clear();
        if (nodes.isEmpty()) {
            return;
        }

        // adjacency over the 6-neighborhood
        Map<String, BlockPos> idToPos = new HashMap<>();
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (BlockPos pos : nodes.keySet()) {
            String id = nodeId(pos);
            idToPos.put(id, pos);
            Set<String> neighbors = adjacency.computeIfAbsent(id, k -> new HashSet<>());
            for (Direction d : Direction.values()) {
                BlockPos np = pos.relative(d);
                if (nodes.containsKey(np)) {
                    neighbors.add(nodeId(np));
                }
            }
        }

        // connected components, deterministic order
        List<String> sorted = new ArrayList<>(adjacency.keySet());
        Collections.sort(sorted);
        Set<String> visited = new HashSet<>();
        int networkId = 0;
        for (String start : sorted) {
            if (!visited.contains(start)) {
                List<String> component = new ArrayList<>();
                Deque<String> queue = new ArrayDeque<>();
                queue.add(start);
                visited.add(start);
                while (!queue.isEmpty()) {
                    String current = queue.poll();
                    component.add(current);
                    for (String next : adjacency.get(current)) {
                        if (visited.add(next)) {
                            queue.add(next);
                        }
                    }
                }
                simulateComponent(networkId++, component, adjacency, idToPos);
            }
        }
    }

    private void simulateComponent(int networkId, List<String> component,
                                   Map<String, Set<String>> adjacency,
                                   Map<String, BlockPos> idToPos) {
        Map<String, SimNode> simNodes = new HashMap<>();
        for (String id : component) {
            BlockPos nodePos = idToPos.get(id);
            KineticNodeProvider provider = nodes.get(nodePos);
            List<String> neighbors = new ArrayList<>(adjacency.get(id));
            Collections.sort(neighbors);
            simNodes.put(id, toSimNode(id, nodePos, provider, neighbors));
        }
        KineticSnapshot snapshot = new SimNetwork(simNodes).simulate(networkId, level.getGameTime());
        snapshots.put(networkId, snapshot);
        for (String id : component) {
            nodes.get(idToPos.get(id)).onKineticSnapshot(snapshot.stateOf(id));
        }
    }

    private SimNode toSimNode(String id, BlockPos pos, KineticNodeProvider p, List<String> neighbors) {
        double derate = derates.getOrDefault(pos, 1.0);
        return switch (p.getKineticRole()) {
            case SOURCE -> SimNode.source(id,
                    Rpm.of(p.getRatedRpm().value() * derate),
                    Torque.ofNewtonMeters(p.getRatedTorque().newtonMeters() * derate),
                    p.getRatedDirection(), p.isKineticEnabled(), neighbors);
            case TRANSMISSION -> switch (p.getTransmissionKind()) {
                case SHAFT -> SimNode.shaft(id, neighbors);
                case GEAR -> SimNode.gear(id, p.getGearTeeth(), neighbors);
                case GEARBOX -> SimNode.gearbox(id, p.getGearboxRatio(), p.getEfficiency(), neighbors);
                case CLUTCH -> SimNode.clutch(id, p.isClutchEngaged(), neighbors);
            };
            case CONSUMER -> SimNode.consumer(id, p.getRequiredRpm(), p.getRequiredTorque(), neighbors);
        };
    }
}
