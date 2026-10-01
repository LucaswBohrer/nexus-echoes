package com.nexus.echoes.dimension.mc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Transient per-player cooldown timestamps (Phase 5.5 audit RISK-2).
 *
 * <p>Pure — no level, no Minecraft classes — so the lifecycle is unit-testable.
 * Entries are keyed by player UUID and must be dropped on logout via
 * {@link #clear(UUID)}; they are never persisted and never consulted after
 * the owning system stops ticking.
 */
final class PlayerCooldowns {

    private final Map<UUID, Long> lastTrigger = new HashMap<>();

    /** True when the player triggered within {@code cooldownTicks} of {@code now}. */
    boolean isOnCooldown(UUID playerId, long now, long cooldownTicks) {
        Long last = lastTrigger.get(playerId);
        return last != null && now - last < cooldownTicks;
    }

    void mark(UUID playerId, long now) {
        lastTrigger.put(playerId, now);
    }

    /**
     * Drops all transient state for a player (logout). Returns true when an
     * entry actually existed.
     */
    boolean clear(UUID playerId) {
        return lastTrigger.remove(playerId) != null;
    }

    /** Number of tracked players (diagnostics / tests). */
    int size() {
        return lastTrigger.size();
    }
}
