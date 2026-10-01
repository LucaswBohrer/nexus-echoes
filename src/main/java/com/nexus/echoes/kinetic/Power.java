package com.nexus.echoes.kinetic;

/**
 * Mechanical power in watts.
 *
 * <p>Power is <b>derived</b>, never an independent input: {@code P = torque × ω}.
 * It exists as a type so balance checks, GUIs and diagnostics speak watts
 * instead of recomputing (and miscomputing) the formula everywhere.
 */
public final class Power {

    public static final Power ZERO = new Power(0.0);

    private final double watts;

    private Power(double watts) {
        this.watts = watts;
    }

    public static Power ofWatts(double watts) {
        if (Double.isNaN(watts) || Double.isInfinite(watts) || watts < 0.0) {
            throw new IllegalArgumentException("Power must be a finite value >= 0 W, got: " + watts);
        }
        return watts == 0.0 ? ZERO : new Power(watts);
    }

    /** The one sanctioned way to obtain power: from a real mechanical state. */
    public static Power from(Rpm rpm, Torque torque) {
        return ofWatts(torque.newtonMeters() * KineticMath.toAngularVelocity(rpm));
    }

    public double watts() {
        return watts;
    }

    public boolean isZero() {
        return watts == 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Power other)) {
            return false;
        }
        return Double.compare(watts, other.watts) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(watts);
    }

    @Override
    public String toString() {
        return watts + " W";
    }
}
