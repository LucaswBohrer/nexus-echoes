package com.nexus.echoes.kinetic;

/**
 * Revolutions per minute — the speed of a rotating shaft.
 *
 * <p>A value type, not a raw double: RPM has units and a domain (>= 0).
 * Negative, NaN or infinite values are rejected at construction so invalid
 * physics can never silently enter the network simulation.
 */
public final class Rpm {

    public static final Rpm ZERO = new Rpm(0.0);

    private final double value;

    private Rpm(double value) {
        this.value = value;
    }

    public static Rpm of(double rpm) {
        if (Double.isNaN(rpm) || Double.isInfinite(rpm) || rpm < 0.0) {
            throw new IllegalArgumentException("RPM must be a finite value >= 0, got: " + rpm);
        }
        return rpm == 0.0 ? ZERO : new Rpm(rpm);
    }

    public double value() {
        return value;
    }

    public boolean isZero() {
        return value == 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rpm other)) {
            return false;
        }
        return Double.compare(value, other.value) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    @Override
    public String toString() {
        return value + " RPM";
    }
}
