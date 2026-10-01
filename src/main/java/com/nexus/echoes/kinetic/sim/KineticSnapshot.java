package com.nexus.echoes.kinetic.sim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The complete result of one network simulation pass: per-node resolved state
 * plus network aggregates (supply/demand power, load factor, overload and
 * conflict flags). Immutable.
 */
public final class KineticSnapshot {

    private final int networkId;
    private final long simTick;
    private final Map<String, NodeState> states;
    private final double totalSupplyPowerW;
    private final double totalDemandPowerW;
    private final double loadFactor;
    private final boolean overloaded;
    private final boolean conflict;

    public KineticSnapshot(int networkId, long simTick, Map<String, NodeState> states,
                           double totalSupplyPowerW, double totalDemandPowerW,
                           double loadFactor, boolean overloaded, boolean conflict) {
        this.networkId = networkId;
        this.simTick = simTick;
        this.states = Collections.unmodifiableMap(new HashMap<>(states));
        this.totalSupplyPowerW = totalSupplyPowerW;
        this.totalDemandPowerW = totalDemandPowerW;
        this.loadFactor = loadFactor;
        this.overloaded = overloaded;
        this.conflict = conflict;
    }

    public int networkId() {
        return networkId;
    }

    public long simTick() {
        return simTick;
    }

    public Map<String, NodeState> states() {
        return states;
    }

    public NodeState stateOf(String nodeId) {
        return states.get(Objects.requireNonNull(nodeId));
    }

    /** Σ rated power of enabled sources, in watts. */
    public double totalSupplyPowerW() {
        return totalSupplyPowerW;
    }

    /** Σ operating-point power of consumers reached by flow, in watts. */
    public double totalDemandPowerW() {
        return totalDemandPowerW;
    }

    /** demand / supply power ratio. &gt; 1 means the network is overloaded. */
    public double loadFactor() {
        return loadFactor;
    }

    public boolean overloaded() {
        return overloaded;
    }

    public boolean conflict() {
        return conflict;
    }

    // ---------------------------------------------------------- serialization

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("networkId", networkId);
        map.put("simTick", simTick);
        Map<String, Object> s = new HashMap<>();
        for (Map.Entry<String, NodeState> e : states.entrySet()) {
            s.put(e.getKey(), e.getValue().toMap());
        }
        map.put("states", s);
        map.put("totalSupplyPowerW", totalSupplyPowerW);
        map.put("totalDemandPowerW", totalDemandPowerW);
        map.put("loadFactor", loadFactor);
        map.put("overloaded", overloaded);
        map.put("conflict", conflict);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static KineticSnapshot fromMap(Map<String, Object> map) {
        Map<String, NodeState> s = new HashMap<>();
        for (Map.Entry<String, Object> e : ((Map<String, Object>) map.get("states")).entrySet()) {
            s.put(e.getKey(), NodeState.fromMap((Map<String, Object>) e.getValue()));
        }
        return new KineticSnapshot(
                ((Number) map.get("networkId")).intValue(),
                ((Number) map.get("simTick")).longValue(),
                s,
                ((Number) map.get("totalSupplyPowerW")).doubleValue(),
                ((Number) map.get("totalDemandPowerW")).doubleValue(),
                ((Number) map.get("loadFactor")).doubleValue(),
                (Boolean) map.get("overloaded"),
                (Boolean) map.get("conflict"));
    }
}
