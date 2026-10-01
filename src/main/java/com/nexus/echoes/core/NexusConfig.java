package com.nexus.echoes.core;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Central configuration for NEXUS: Echoes of Reality.
 *
 * Rule: no critical numeric literal lives in machine code. Everything tunable
 * is here, with a comment explaining its unit.
 */
public final class NexusConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue RESONATOR_CAPACITY;
    public static final ForgeConfigSpec.IntValue RESONATOR_MAX_RECEIVE;
    public static final ForgeConfigSpec.IntValue RESONATOR_ENERGY_PER_TICK;
    public static final ForgeConfigSpec.IntValue RESONATOR_DEFAULT_PROCESS_TIME;
    public static final ForgeConfigSpec.IntValue CELL_TRANSFER_PER_TICK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("NEXUS: Echoes of Reality — Phase 1 (CORE) settings").push("phase1");

        RESONATOR_CAPACITY = builder
                .comment("Resonator internal resonant-energy buffer, in energy units.")
                .defineInRange("resonatorCapacity", 20_000, 1_000, 1_000_000);

        RESONATOR_MAX_RECEIVE = builder
                .comment("Max resonant energy the Resonator accepts per transfer, in energy units.")
                .defineInRange("resonatorMaxReceive", 500, 1, 100_000);

        RESONATOR_ENERGY_PER_TICK = builder
                .comment("Resonant energy consumed per tick while the Resonator is processing.")
                .defineInRange("resonatorEnergyPerTick", 20, 0, 10_000);

        RESONATOR_DEFAULT_PROCESS_TIME = builder
                .comment("Fallback processing time in ticks when a recipe does not define one.")
                .defineInRange("resonatorDefaultProcessTime", 200, 1, 72_000);

        CELL_TRANSFER_PER_TICK = builder
                .comment("Energy units the creative cell pushes per neighbor per tick (x10 per transfer pulse).")
                .defineInRange("cellTransferPerTick", 100, 1, 100_000);

        builder.pop();
        SPEC = builder.build();
    }

    private NexusConfig() {
    }
}
