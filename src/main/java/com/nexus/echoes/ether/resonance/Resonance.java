package com.nexus.echoes.ether.resonance;

/**
 * Ether Resonance: a player's interaction/exposure state with the Ether
 * (Phase 6A gate, C1).
 *
 * <p>Pure domain value object — no Minecraft imports. Immutable; every
 * mutation returns a new instance clamped to {@code [0, 1000]}.
 *
 * <p>Resonance is <b>not</b> Nexus Energy, <b>not</b> Kinetic energy,
 * <b>not</b> a battery, and <b>not</b> a generic energy API. In v1 it is a
 * progression/interactivity state and a threshold for Ether interactions.
 * There are no decrease mechanics in v1: resonance is monotonic.
 */
public final class Resonance {

    /** Lower bound (inclusive). */
    public static final int MIN = 0;

    /** Upper bound (inclusive). */
    public static final int MAX = 1000;

    private final int value;

    private Resonance(int value) {
        this.value = value;
    }

    /**
     * Creates a Resonance, clamping into range.
     *
     * @param value raw value; clamped to {@code [MIN, MAX]}
     */
    public static Resonance of(int value) {
        return new Resonance(clamp(value));
    }

    /** Zero resonance. */
    public static Resonance zero() {
        return new Resonance(MIN);
    }

    /**
     * Returns a new Resonance shifted by {@code delta}, clamped.
     * Negative deltas are representable but unused in v1 (no decrease
     * mechanics); the clamp keeps the type total regardless.
     */
    public Resonance withDelta(int delta) {
        return new Resonance(clamp(this.value + delta));
    }

    /** True when this resonance meets or exceeds {@code threshold}. */
    public boolean isAtLeast(int threshold) {
        return this.value >= threshold;
    }

    public int value() {
        return value;
    }

    private static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Resonance other)) {
            return false;
        }
        return value == other.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return "Resonance{" + value + '}';
    }
}
