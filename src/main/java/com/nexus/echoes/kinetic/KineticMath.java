package com.nexus.echoes.kinetic;

/**
 * The physics of the kinetic system, in one place.
 *
 * <p>Every RPM/torque/power conversion in the mod goes through here, so the
 * math is defined once, tested once, and never re-derived ad hoc in machine
 * code. Plain Java — no Minecraft classes — so the whole thing is unit
 * tested as ordinary Java.
 *
 * <p>Conventions:
 * <ul>
 *   <li>RPM → angular velocity: ω = RPM × 2π / 60 (rad/s).</li>
 *   <li>Power: P = τ × ω (watts).</li>
 *   <li>Gear ratio is expressed as an <b>output:input speed multiplier</b>:
 *       {@code ratio = 2.0} means the output shaft spins twice as fast.</li>
 *   <li>Torque transforms inversely to speed, scaled by efficiency
 *       (power conservation): {@code τ_out = τ_in × efficiency / ratio}.</li>
 * </ul>
 */
public final class KineticMath {

    /** 2π — one revolution in radians. */
    public static final double TWO_PI = Math.PI * 2.0;

    /** RPM per rad/s, the inverse of {@link #toAngularVelocity(Rpm)}. */
    public static final double RPM_PER_RAD_PER_S = 60.0 / TWO_PI;

    private KineticMath() {
    }

    /**
     * Converts RPM to angular velocity (rad/s).
     *
     * @throws IllegalArgumentException if rpm is null
     */
    public static double toAngularVelocity(Rpm rpm) {
        requireNonNull(rpm, "rpm");
        return rpm.value() * TWO_PI / 60.0;
    }

    /**
     * Converts angular velocity (rad/s) back to RPM.
     *
     * @throws IllegalArgumentException if omega is negative, NaN or infinite
     */
    public static Rpm toRpm(double omegaRadPerS) {
        if (Double.isNaN(omegaRadPerS) || Double.isInfinite(omegaRadPerS) || omegaRadPerS < 0.0) {
            throw new IllegalArgumentException("Angular velocity must be finite and >= 0, got: " + omegaRadPerS);
        }
        return Rpm.of(omegaRadPerS * RPM_PER_RAD_PER_S);
    }

    /**
     * Derives power from a mechanical state. This is the only sanctioned
     * P = τ × ω computation.
     */
    public static Power toPower(Rpm rpm, Torque torque) {
        requireNonNull(rpm, "rpm");
        requireNonNull(torque, "torque");
        return Power.from(rpm, torque);
    }

    /**
     * Applies a gear ratio to a (rpm, torque) pair.
     *
     * @param ratio      output:input speed multiplier, must be &gt; 0.
     *                   2.0 doubles speed (halves torque); 0.5 halves speed (doubles torque).
     * @param efficiency fraction of power surviving the transmission, (0, 1].
     *                   Phase 2 uses the ideal 1.0; the parameter exists so
     *                   losses can be introduced without changing call sites.
     * @return the transformed (rpm, torque) pair; power is conserved modulo efficiency
     * @throws IllegalArgumentException on non-positive ratio or out-of-range efficiency
     */
    public static RatioResult applyRatio(Rpm inRpm, Torque inTorque, double ratio, double efficiency) {
        requireNonNull(inRpm, "inRpm");
        requireNonNull(inTorque, "inTorque");
        if (Double.isNaN(ratio) || Double.isInfinite(ratio) || ratio <= 0.0) {
            throw new IllegalArgumentException("Gear ratio must be finite and > 0, got: " + ratio);
        }
        if (Double.isNaN(efficiency) || efficiency <= 0.0 || efficiency > 1.0) {
            throw new IllegalArgumentException("Efficiency must be in (0, 1], got: " + efficiency);
        }
        Rpm outRpm = Rpm.of(inRpm.value() * ratio);
        Torque outTorque = Torque.ofNewtonMeters(inTorque.newtonMeters() * efficiency / ratio);
        return new RatioResult(outRpm, outTorque);
    }

    /**
     * Torque felt at the source for a consumer demanding {@code consumerTorque}
     * behind transmissions with a combined torque gain of {@code torqueGain}
     * (torqueGain = Π(efficiencyᵢ / ratioᵢ) along the path).
     */
    public static Torque demandAtSource(Torque consumerTorque, double torqueGain) {
        requireNonNull(consumerTorque, "consumerTorque");
        if (Double.isNaN(torqueGain) || Double.isInfinite(torqueGain) || torqueGain <= 0.0) {
            throw new IllegalArgumentException("Torque gain must be finite and > 0, got: " + torqueGain);
        }
        return Torque.ofNewtonMeters(consumerTorque.newtonMeters() / torqueGain);
    }

    private static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    /** Result of {@link #applyRatio(Rpm, Torque, double, double)}. */
    public record RatioResult(Rpm rpm, Torque torque) {
    }
}
