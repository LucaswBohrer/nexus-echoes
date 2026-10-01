package com.nexus.echoes.kinetic;

/**
 * Sense of rotation of a shaft, viewed along its axis.
 *
 * <p>Meshing gears invert it; shafts, gearboxes and clutches preserve it.
 * Sources can invert their output (generator GUI).
 */
public enum RotationDirection {
    CLOCKWISE,
    COUNTERCLOCKWISE;

    public RotationDirection opposite() {
        return this == CLOCKWISE ? COUNTERCLOCKWISE : CLOCKWISE;
    }
}
