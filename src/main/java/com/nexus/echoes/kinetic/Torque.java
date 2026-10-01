package com.nexus.echoes.kinetic;

/**
 * Torque in newton-meters — the rotational "push" a shaft delivers.
 *
 * <p>Independent from RPM: a network can spin fast with little torque or slow
 * with enormous torque. Value type with the same validation rules as
 * {@link Rpm}.
 */
public final class Torque {

    public static final Torque ZERO = new Torque(0.0);

    private final double newtonMeters;

    private Torque(double newtonMeters) {
        this.newtonMeters = newtonMeters;
    }

    public static Torque ofNewtonMeters(double nm) {
        if (Double.isNaN(nm) || Double.isInfinite(nm) || nm < 0.0) {
            throw new IllegalArgumentException("Torque must be a finite value >= 0 Nm, got: " + nm);
        }
        return nm == 0.0 ? ZERO : new Torque(nm);
    }

    public double newtonMeters() {
        return newtonMeters;
    }

    public boolean isZero() {
        return newtonMeters == 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Torque other)) {
            return false;
        }
        return Double.compare(newtonMeters, other.newtonMeters) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(newtonMeters);
    }

    @Override
    public String toString() {
        return newtonMeters + " Nm";
    }
}
