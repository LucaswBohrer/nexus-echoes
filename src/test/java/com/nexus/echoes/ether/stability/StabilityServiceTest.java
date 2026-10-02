package com.nexus.echoes.ether.stability;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stability composition rules (Phase 6, S3).
 *
 * <p>Covers: baseline on empty input, additive composition, expired
 * modifiers ignored, clamping, order-independence (deterministic
 * overlap), and storm derate bounds.
 */
class StabilityServiceTest {

    @Test
    void emptyModifiersYieldBaseline() {
        assertEquals(StabilityService.BASELINE,
                StabilityService.effectiveStability(List.of(), 1000L));
    }

    @Test
    void modifiersComposeAdditively() {
        List<StabilityModifier> mods = List.of(
                new StabilityModifier("a", -40, 5000L),
                new StabilityModifier("b", 10, 5000L));
        assertEquals(StabilityService.BASELINE - 30,
                StabilityService.effectiveStability(mods, 1000L));
    }

    @Test
    void expiredModifiersAreIgnored() {
        List<StabilityModifier> mods = List.of(
                new StabilityModifier("old", -40, 999L));
        assertEquals(StabilityService.BASELINE,
                StabilityService.effectiveStability(mods, 1000L));
    }

    @Test
    void compositionIsClamped() {
        List<StabilityModifier> down = List.of(
                new StabilityModifier("a", -500, 5000L));
        assertEquals(StabilityService.MIN,
                StabilityService.effectiveStability(down, 1000L));

        List<StabilityModifier> up = List.of(
                new StabilityModifier("a", 500, 5000L));
        assertEquals(StabilityService.MAX,
                StabilityService.effectiveStability(up, 1000L));
    }

    @Test
    void compositionIsOrderIndependent() {
        StabilityModifier a = new StabilityModifier("a", -40, 5000L);
        StabilityModifier b = new StabilityModifier("b", -25, 6000L);
        int forward = StabilityService.effectiveStability(List.of(a, b), 1000L);
        int backward = StabilityService.effectiveStability(List.of(b, a), 1000L);
        assertEquals(forward, backward,
                "overlapping phenomena must compose deterministically");
        assertEquals(StabilityService.BASELINE - 65, forward);
    }

    @Test
    void stormDerateIsBoundedAndMonotonic() {
        double calm = StabilityService.stormDerate(StabilityService.BASELINE);
        double unstable = StabilityService.stormDerate(0);
        assertTrue(unstable > calm, "lower stability means stronger derate");
        assertTrue(calm >= 0.25 && calm <= 0.75);
        assertTrue(unstable >= 0.25 && unstable <= 0.75);
        assertEquals(0.25, StabilityService.stormDerate(1000), 1e-9);
        assertEquals(0.75, StabilityService.stormDerate(-1000), 1e-9);
    }

    @Test
    void modifierExpiryBoundary() {
        StabilityModifier m = new StabilityModifier("a", -40, 1000L);
        assertTrue(m.isExpired(1000L), "expires exactly at expiresTick");
        assertFalse(m.isExpired(999L));
    }
}
