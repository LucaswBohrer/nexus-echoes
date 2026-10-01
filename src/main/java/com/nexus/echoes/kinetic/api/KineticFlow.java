package com.nexus.echoes.kinetic.api;

import com.nexus.echoes.kinetic.KineticMath;
import com.nexus.echoes.kinetic.Power;
import com.nexus.echoes.kinetic.RotationDirection;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;

/**
 * The mechanical state carried along a network path: RPM, torque and rotation
 * direction at one point of the drivetrain. Immutable.
 *
 * <p>Power is always derived ({@link KineticMath#toPower}), never stored.
 */
public final class KineticFlow {

    private final Rpm rpm;
    private final Torque torque;
    private final RotationDirection direction;

    private KineticFlow(Rpm rpm, Torque torque, RotationDirection direction) {
        this.rpm = rpm;
        this.torque = torque;
        this.direction = direction;
    }

    public static KineticFlow of(Rpm rpm, Torque torque, RotationDirection direction) {
        if (rpm == null || torque == null || direction == null) {
            throw new IllegalArgumentException("KineticFlow fields must not be null");
        }
        return new KineticFlow(rpm, torque, direction);
    }

    public Rpm rpm() {
        return rpm;
    }

    public Torque torque() {
        return torque;
    }

    public RotationDirection direction() {
        return direction;
    }

    /** Derived power at this point of the drivetrain. */
    public Power power() {
        return KineticMath.toPower(rpm, torque);
    }

    @Override
    public String toString() {
        return rpm + " / " + torque + " / " + direction + " (" + power() + ")";
    }
}
