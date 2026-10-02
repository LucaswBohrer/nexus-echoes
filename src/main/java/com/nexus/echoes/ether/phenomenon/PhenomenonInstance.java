package com.nexus.echoes.ether.phenomenon;

import java.util.Objects;
import java.util.UUID;

/**
 * One active Ether phenomenon (Phase 6B plan v2, S4).
 *
 * <p>Pure domain: positions are plain ints, ticks are longs, the owner is
 * explicit. Instances are <b>transient</b> — they live in the manager's
 * memory and are discarded on server restart (Phase 6A gate, C4).
 * Immutable except for lifecycle queries.
 */
public final class PhenomenonInstance {

    private final UUID instanceId;
    private final PhenomenonDefinition definition;
    private final PhenomenonOwner owner;
    private final int x;
    private final int y;
    private final int z;
    private final long createdTick;
    private final long expiresTick;

    public PhenomenonInstance(UUID instanceId, PhenomenonDefinition definition,
                              PhenomenonOwner owner,
                              int x, int y, int z,
                              long createdTick) {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdTick = createdTick;
        this.expiresTick = createdTick + definition.durationTicks();
    }

    public UUID instanceId() {
        return instanceId;
    }

    public PhenomenonDefinition definition() {
        return definition;
    }

    public PhenomenonOwner owner() {
        return owner;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public long createdTick() {
        return createdTick;
    }

    public long expiresTick() {
        return expiresTick;
    }

    /** True once the phenomenon has reached the end of its duration. */
    public boolean isExpired(long nowTick) {
        return nowTick >= expiresTick;
    }

    /**
     * True when the given position is inside this phenomenon's radius.
     * Plain integer math; owned by the phenomenon system so the stability
     * adapter only has to ask.
     */
    public boolean affects(int px, int py, int pz) {
        long dx = (long) px - x;
        long dy = (long) py - y;
        long dz = (long) pz - z;
        long radius = definition.radius();
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    @Override
    public String toString() {
        return "PhenomenonInstance{" + definition.id() + " @ " + x + "," + y + "," + z + '}';
    }
}
