package com.nexus.echoes.kinetic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the kinetic math core: RPM/torque/power conversions,
 * gear ratios, direction and invalid-input rejection.
 */
class KineticMathTest {

    private static final double DELTA = 1e-9;

    @Test
    void rpmToAngularVelocity() {
        assertEquals(2 * Math.PI, KineticMath.toAngularVelocity(Rpm.of(60)), DELTA);
        assertEquals(4 * Math.PI, KineticMath.toAngularVelocity(Rpm.of(120)), DELTA);
        assertEquals(0.0, KineticMath.toAngularVelocity(Rpm.ZERO), DELTA);
    }

    @Test
    void angularVelocityBackToRpm() {
        assertEquals(60.0, KineticMath.toRpm(2 * Math.PI).value(), DELTA);
        assertEquals(120.0, KineticMath.toRpm(4 * Math.PI).value(), DELTA);
    }

    @Test
    void torqueTimesAngularVelocityIsPower() {
        // 120 RPM + 50 Nm -> P = 50 * (120 * 2pi/60) = 200*pi W
        Power p = KineticMath.toPower(Rpm.of(120), Torque.ofNewtonMeters(50));
        assertEquals(200 * Math.PI, p.watts(), 1e-6);
    }

    @Test
    void sixtyRpmHundredNmIsKnownPower() {
        // 60 RPM + 100 Nm -> P = 100 * 2pi = 200pi W
        Power p = KineticMath.toPower(Rpm.of(60), Torque.ofNewtonMeters(100));
        assertEquals(200 * Math.PI, p.watts(), 1e-6);
    }

    @Test
    void gearRatioTwoToOneDoublesSpeedHalvesTorque() {
        KineticMath.RatioResult r = KineticMath.applyRatio(
                Rpm.of(60), Torque.ofNewtonMeters(100), 2.0, 1.0);
        assertEquals(120.0, r.rpm().value(), DELTA);
        assertEquals(50.0, r.torque().newtonMeters(), DELTA);
    }

    @Test
    void gearRatioOneToTwoHalvesSpeedDoublesTorque() {
        KineticMath.RatioResult r = KineticMath.applyRatio(
                Rpm.of(120), Torque.ofNewtonMeters(50), 0.5, 1.0);
        assertEquals(60.0, r.rpm().value(), DELTA);
        assertEquals(100.0, r.torque().newtonMeters(), DELTA);
    }

    @Test
    void efficiencyScalesTorqueNotSpeed() {
        KineticMath.RatioResult r = KineticMath.applyRatio(
                Rpm.of(60), Torque.ofNewtonMeters(100), 2.0, 0.9);
        assertEquals(120.0, r.rpm().value(), DELTA);
        assertEquals(45.0, r.torque().newtonMeters(), DELTA);
        // power out = 0.9 * power in
        Power in = KineticMath.toPower(Rpm.of(60), Torque.ofNewtonMeters(100));
        Power out = KineticMath.toPower(r.rpm(), r.torque());
        assertEquals(0.9 * in.watts(), out.watts(), 1e-6);
    }

    @Test
    void idealTransmissionConservesPower() {
        KineticMath.RatioResult r = KineticMath.applyRatio(
                Rpm.of(60), Torque.ofNewtonMeters(100), 2.0, 1.0);
        Power in = KineticMath.toPower(Rpm.of(60), Torque.ofNewtonMeters(100));
        Power out = KineticMath.toPower(r.rpm(), r.torque());
        assertEquals(in.watts(), out.watts(), 1e-6);
    }

    @Test
    void directionFlips() {
        assertEquals(RotationDirection.COUNTERCLOCKWISE,
                RotationDirection.CLOCKWISE.opposite());
        assertEquals(RotationDirection.CLOCKWISE,
                RotationDirection.COUNTERCLOCKWISE.opposite());
        assertEquals(RotationDirection.CLOCKWISE,
                RotationDirection.CLOCKWISE.opposite().opposite());
    }

    @Test
    void zeroRpmGivesZeroPower() {
        assertEquals(0.0, KineticMath.toPower(Rpm.ZERO, Torque.ofNewtonMeters(100)).watts(), DELTA);
    }

    @Test
    void zeroTorqueGivesZeroPower() {
        assertEquals(0.0, KineticMath.toPower(Rpm.of(120), Torque.ZERO).watts(), DELTA);
    }

    @Test
    void demandAtSourceAccountsForTorqueGain() {
        // consumer needs 100 Nm behind a 2:1 reducer (gain 2.0): source feels 50 Nm
        Torque demand = KineticMath.demandAtSource(Torque.ofNewtonMeters(100), 2.0);
        assertEquals(50.0, demand.newtonMeters(), DELTA);
    }

    @Test
    void negativeRpmIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Rpm.of(-1));
        assertThrows(IllegalArgumentException.class, () -> Rpm.of(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Rpm.of(Double.POSITIVE_INFINITY));
    }

    @Test
    void negativeTorqueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Torque.ofNewtonMeters(-0.5));
        assertThrows(IllegalArgumentException.class, () -> Torque.ofNewtonMeters(Double.NaN));
    }

    @Test
    void negativePowerIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Power.ofWatts(-10));
    }

    @Test
    void invalidRatioIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> KineticMath.applyRatio(Rpm.of(60), Torque.ofNewtonMeters(10), 0.0, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> KineticMath.applyRatio(Rpm.of(60), Torque.ofNewtonMeters(10), -2.0, 1.0));
    }

    @Test
    void invalidEfficiencyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> KineticMath.applyRatio(Rpm.of(60), Torque.ofNewtonMeters(10), 2.0, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> KineticMath.applyRatio(Rpm.of(60), Torque.ofNewtonMeters(10), 2.0, 1.5));
    }

    @Test
    void nullInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> KineticMath.toPower(null, Torque.ZERO));
        assertThrows(IllegalArgumentException.class, () -> KineticMath.toAngularVelocity(null));
    }
}
