package com.nexus.echoes.dimension.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The spire's charge rule is a pure function (Phase 5):
 * proportional accumulation under power, slow decay without, clamped [0,1].
 */
class SpireChargeTest {

    private static final double RPM = SpireBlockEntity.REQUIRED_RPM;
    private static final double TORQUE = SpireBlockEntity.REQUIRED_TORQUE_NM;

    @Test
    void fullPowerChargesToFullInSixtySeconds() {
        double charge = 0.0;
        for (int tick = 0; tick < 20 * 60; tick++) {
            charge = SpireBlockEntity.nextCharge(charge, RPM, TORQUE, true);
        }
        assertEquals(1.0, charge, 1e-9);
    }

    @Test
    void brownoutChargesProportionally() {
        double full = SpireBlockEntity.nextCharge(0.0, RPM, TORQUE, true);
        double half = SpireBlockEntity.nextCharge(0.0, RPM / 2, TORQUE, true);
        assertEquals(full / 2, half, 1e-12);
    }

    @Test
    void noPowerDecaysSlowly() {
        double charge = SpireBlockEntity.nextCharge(1.0, 0.0, 0.0, false);
        assertTrue(charge < 1.0 && charge > 0.99);
    }

    @Test
    void chargeStaysClamped() {
        assertEquals(1.0, SpireBlockEntity.nextCharge(1.0, RPM * 4, TORQUE * 4, true));
        assertEquals(0.0, SpireBlockEntity.nextCharge(0.0, 0.0, 0.0, false));
    }

    @Test
    void decayEmptiesInFiveMinutes() {
        double charge = 1.0;
        for (int tick = 0; tick < 20 * 300; tick++) {
            charge = SpireBlockEntity.nextCharge(charge, 0.0, 0.0, false);
        }
        assertEquals(0.0, charge, 1e-9);
    }
}
