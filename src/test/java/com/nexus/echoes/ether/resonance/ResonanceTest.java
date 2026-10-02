package com.nexus.echoes.ether.resonance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resonance value semantics (Phase 6, S1).
 *
 * <p>Covers: bounds, clamping, immutability, threshold checks.
 */
class ResonanceTest {

    @Test
    void boundsAreZeroToThousand() {
        assertEquals(0, Resonance.of(0).value());
        assertEquals(1000, Resonance.of(1000).value());
        assertEquals(500, Resonance.of(500).value());
    }

    @Test
    void outOfRangeValuesAreClamped() {
        assertEquals(0, Resonance.of(-50).value());
        assertEquals(1000, Resonance.of(5000).value());
    }

    @Test
    void withDeltaReturnsNewClampedInstance() {
        Resonance base = Resonance.of(900);
        Resonance grown = base.withDelta(200);
        assertEquals(900, base.value(), "original must be immutable");
        assertEquals(1000, grown.value(), "delta result is clamped");
        assertEquals(850, base.withDelta(-50).value());
        assertEquals(0, Resonance.of(10).withDelta(-999).value());
    }

    @Test
    void thresholdCheck() {
        Resonance r = Resonance.of(250);
        assertTrue(r.isAtLeast(ResonanceThresholds.TRAVEL_MIN));
        assertFalse(r.isAtLeast(ResonanceThresholds.TRAVEL_MIN + 1));
        assertFalse(Resonance.zero().isAtLeast(ResonanceThresholds.TRAVEL_MIN));
    }

    @Test
    void equalityIsByValue() {
        assertEquals(Resonance.of(123), Resonance.of(123));
        assertNotEquals(Resonance.of(123), Resonance.of(124));
    }

    @Test
    void tuningConstantsAreSane() {
        assertEquals(250, ResonanceThresholds.TRAVEL_MIN);
        assertEquals(600, ResonanceThresholds.STALKER_AGGRO);
        assertEquals(50, ResonanceThresholds.FIRST_EXPOSURE_GRANT);
        assertTrue(ResonanceThresholds.TRAVEL_MIN <= ResonanceThresholds.STALKER_AGGRO);
        assertTrue(ResonanceThresholds.FIRST_EXPOSURE_GRANT < ResonanceThresholds.TRAVEL_MIN,
                "first exposure alone must not unlock stabilized travel");
    }
}
