package com.nexus.echoes.dimension.mc;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Transient per-player cooldown lifecycle (Phase 5.5 audit RISK-2).
 *
 * <p>Covers: cooldown windows, per-player isolation, and logout cleanup —
 * entries must never accumulate for players who are gone.
 */
class PlayerCooldownsTest {

    @Test
    void cooldownWindowIsRespected() {
        PlayerCooldowns cooldowns = new PlayerCooldowns();
        UUID player = UUID.randomUUID();
        assertFalse(cooldowns.isOnCooldown(player, 100L, 300L));
        cooldowns.mark(player, 100L);
        assertTrue(cooldowns.isOnCooldown(player, 200L, 300L));
        assertTrue(cooldowns.isOnCooldown(player, 399L, 300L));
        assertFalse(cooldowns.isOnCooldown(player, 400L, 300L),
                "cooldown expires exactly at now - last == cooldownTicks");
    }

    @Test
    void cooldownsAreIsolatedPerPlayer() {
        PlayerCooldowns cooldowns = new PlayerCooldowns();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        cooldowns.mark(alice, 100L);
        assertTrue(cooldowns.isOnCooldown(alice, 200L, 300L));
        assertFalse(cooldowns.isOnCooldown(bob, 200L, 300L));
    }

    @Test
    void remarkRefreshesTheWindow() {
        PlayerCooldowns cooldowns = new PlayerCooldowns();
        UUID player = UUID.randomUUID();
        cooldowns.mark(player, 100L);
        cooldowns.mark(player, 500L);
        assertTrue(cooldowns.isOnCooldown(player, 700L, 300L),
                "re-mark moves the window start to the latest mark");
        assertFalse(cooldowns.isOnCooldown(player, 800L, 300L));
    }

    @Test
    void clearDropsOnlyThatPlayer() {
        PlayerCooldowns cooldowns = new PlayerCooldowns();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        cooldowns.mark(alice, 100L);
        cooldowns.mark(bob, 100L);
        assertTrue(cooldowns.clear(alice));
        assertFalse(cooldowns.clear(alice), "second clear reports nothing to drop");
        assertEquals(1, cooldowns.size());
        assertFalse(cooldowns.isOnCooldown(alice, 200L, 300L));
        assertTrue(cooldowns.isOnCooldown(bob, 200L, 300L));
    }
}
