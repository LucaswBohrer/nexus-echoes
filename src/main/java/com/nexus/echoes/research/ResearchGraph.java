package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validated, immutable set of research definitions with a cycle-safe
 * dependency graph.
 *
 * <p>Validation is fail-fast and deterministic: duplicate ids, unknown
 * prerequisites and dependency cycles are rejected with clear messages at
 * load time, never during gameplay. Iteration order is the definition order
 * given to {@link #validate(Collection)}, so GUI listings are stable.
 */
public final class ResearchGraph {

    private final Map<ResourceLocation, ResearchDefinition> definitions;
    private final List<ResourceLocation> topologicalOrder;

    private ResearchGraph(Map<ResourceLocation, ResearchDefinition> definitions,
                          List<ResourceLocation> topologicalOrder) {
        this.definitions = definitions;
        this.topologicalOrder = topologicalOrder;
    }

    /**
     * Validates and indexes the given definitions.
     *
     * @throws IllegalArgumentException on duplicate id, unknown prerequisite,
     *                                  self-dependency or dependency cycle
     */
    public static ResearchGraph validate(Collection<ResearchDefinition> defs) {
        Map<ResourceLocation, ResearchDefinition> byId = new LinkedHashMap<>();
        for (ResearchDefinition def : defs) {
            if (byId.put(def.id(), def) != null) {
                throw new IllegalArgumentException("duplicate research id: " + def.id());
            }
        }
        for (ResearchDefinition def : byId.values()) {
            for (ResourceLocation prereq : def.prerequisites()) {
                if (!byId.containsKey(prereq)) {
                    throw new IllegalArgumentException(
                            "research " + def.id() + " requires unknown research " + prereq);
                }
                if (prereq.equals(def.id())) {
                    throw new IllegalArgumentException(
                            "research " + def.id() + " cannot require itself");
                }
            }
        }
        return new ResearchGraph(
                Collections.unmodifiableMap(byId), topologicalSort(byId));
    }

    /** Deterministic topological order (definition order breaks ties). */
    private static List<ResourceLocation> topologicalSort(
            Map<ResourceLocation, ResearchDefinition> byId) {
        List<ResourceLocation> order = new ArrayList<>();
        Map<ResourceLocation, Integer> mark = new LinkedHashMap<>(); // 1 = visiting, 2 = done
        for (ResourceLocation id : byId.keySet()) {
            visit(id, byId, mark, order, new ArrayList<>());
        }
        return List.copyOf(order);
    }

    private static void visit(ResourceLocation id,
                              Map<ResourceLocation, ResearchDefinition> byId,
                              Map<ResourceLocation, Integer> mark,
                              List<ResourceLocation> order,
                              List<ResourceLocation> path) {
        Integer state = mark.get(id);
        if (state != null) {
            if (state == 1) {
                List<ResourceLocation> cycle = new ArrayList<>(path.subList(path.indexOf(id), path.size()));
                cycle.add(id);
                throw new IllegalArgumentException(
                        "research dependency cycle detected: " + cycle);
            }
            return;
        }
        mark.put(id, 1);
        path.add(id);
        for (ResourceLocation prereq : byId.get(id).prerequisites()) {
            visit(prereq, byId, mark, order, path);
        }
        path.remove(path.size() - 1);
        mark.put(id, 2);
        order.add(id);
    }

    public ResearchDefinition get(ResourceLocation id) {
        return definitions.get(id);
    }

    public boolean contains(ResourceLocation id) {
        return definitions.containsKey(id);
    }

    /** All definitions in definition order. */
    public Collection<ResearchDefinition> all() {
        return definitions.values();
    }

    /** Definitions in dependency order (prerequisites before dependents). */
    public List<ResourceLocation> topologicalOrder() {
        return topologicalOrder;
    }

    public int size() {
        return definitions.size();
    }
}
