package com.nexus.echoes.energy.api;

/**
 * THE energy contract of NEXUS: Echoes of Reality.
 *
 * <p>Every energy type (resonant, kinetic, ether, …) is expressed through this
 * interface. Implementations decide the physics; callers only rely on these
 * semantics:
 *
 * <ul>
 *   <li>{@code simulate = true} must never change state.</li>
 *   <li>Amounts are integers (no floats — deterministic, multiplayer-safe).</li>
 *   <li>Returned values are the amounts actually moved, already clamped.</li>
 * </ul>
 */
public interface INexusEnergy {

    /** Which energy type this storage holds. Transfers require a type match. */
    EnergyType getEnergyType();

    /**
     * @return energy actually accepted (0 … maxReceive), clamped by free capacity.
     */
    int receiveEnergy(int maxReceive, boolean simulate);

    /**
     * @return energy actually extracted (0 … maxExtract), clamped by stored amount.
     */
    int extractEnergy(int maxExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canReceive();

    boolean canExtract();

    /** Two storages can transfer only when types match and sides allow it. */
    default boolean canTransferTo(INexusEnergy other) {
        return other != null
                && this.getEnergyType() == other.getEnergyType()
                && this.canExtract()
                && other.canReceive();
    }
}
