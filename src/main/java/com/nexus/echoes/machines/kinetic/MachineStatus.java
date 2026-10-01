package com.nexus.echoes.machines.kinetic;

/**
 * Machine-level processing status, derived (never persisted) from the work
 * queue and the kinetic network state.
 *
 * <p>Distinct from {@code NodeStatus} (network-level: what the shaft delivers).
 * A machine can have {@code NodeStatus.OK} on its shaft and still be
 * {@code IDLE} because it has nothing to process.
 */
public enum MachineStatus {
    /** Shaft delivers usable power and the machine is converting input. */
    RUNNING,
    /** Nothing to do: no matching recipe for the input slot. */
    IDLE,
    /** No shaft power at all (disconnected, disabled source, no network). */
    NO_POWER,
    /** Shaft power is present but below requirement — work slows proportionally. */
    BROWNOUT,
    /** Output (or byproduct) slot cannot accept the recipe result. */
    BLOCKED;

    /** Short, stable display token for GUIs and diagnostics. */
    public String display() {
        return switch (this) {
            case RUNNING -> "RUNNING";
            case IDLE -> "IDLE";
            case NO_POWER -> "NO POWER";
            case BROWNOUT -> "BROWNOUT";
            case BLOCKED -> "BLOCKED";
        };
    }
}
