package com.nexus.echoes.energy.api;

/**
 * The distinct energy types of the NEXUS progression.
 *
 * <p>Phase 1 ships only {@link #RESONANT}. New types are added as the mod grows
 * (kinetic in Phase 2, ether/void/core later) WITHOUT changing {@link INexusEnergy}.
 */
public enum EnergyType {
    /** Phase 1 generic stored energy. Proves producer → transmission → consumer. */
    RESONANT,
    /** Phase 2: RPM / torque / power mechanical energy. */
    KINETIC
}
