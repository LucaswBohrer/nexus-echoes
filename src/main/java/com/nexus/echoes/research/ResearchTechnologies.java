package com.nexus.echoes.research;

import com.nexus.echoes.NexusEchoes;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Stable technology identifiers gated by research (ADR-010).
 *
 * <p>A technology id is intentionally decoupled from item/block ids: it names
 * the *capability* ("can use a crusher"), not the registry object. Future
 * systems (Hollow access, structures, codex entries) gate on these ids
 * without touching the research domain.
 */
public final class ResearchTechnologies {

    public static final ResourceLocation CRUSHER =
            new ResourceLocation(NexusEchoes.MOD_ID, "crusher");
    public static final ResourceLocation PROCESSOR =
            new ResourceLocation(NexusEchoes.MOD_ID, "processor");
    public static final ResourceLocation NEXUS_COMPONENT =
            new ResourceLocation(NexusEchoes.MOD_ID, "nexus_component");
    /**
     * Future hook for Phase 5+: no dimension, portal, blocks or worldgen are
     * registered in Phase 4 — only the unlock id exists so later content can
     * depend on research without coupling back into it.
     */
    public static final ResourceLocation HOLLOW_ACCESS =
            new ResourceLocation(NexusEchoes.MOD_ID, "hollow_access");
    /**
     * Phase 5: the access machine itself. Crafting/placing/using the
     * dimensional spire requires the same unlock that grants Hollow travel —
     * the device is the technology.
     */
    public static final ResourceLocation DIMENSIONAL_SPIRE = HOLLOW_ACCESS;
    /**
     * Phase 5: anomaly counterplay device, unlocked by
     * {@code hollow_exploration}.
     */
    public static final ResourceLocation ANOMALY_WARD =
            new ResourceLocation(NexusEchoes.MOD_ID, "anomaly_ward");
    /**
     * Phase 5: exploration tool, unlocked by {@code anomaly_studies}.
     */
    public static final ResourceLocation RESONANCE_SCANNER =
            new ResourceLocation(NexusEchoes.MOD_ID, "resonance_scanner");

    /**
     * Crafted item id → technology required to keep the result.
     * Blocks are additionally gated at placement and use.
     */
    public static final Map<ResourceLocation, ResourceLocation> CRAFT_GATE =
            Map.of(
                    new ResourceLocation(NexusEchoes.MOD_ID, "crusher"), CRUSHER,
                    new ResourceLocation(NexusEchoes.MOD_ID, "processor"), PROCESSOR,
                    new ResourceLocation(NexusEchoes.MOD_ID, "nexus_component"), NEXUS_COMPONENT,
                    new ResourceLocation(NexusEchoes.MOD_ID, "dimensional_spire"), DIMENSIONAL_SPIRE,
                    new ResourceLocation(NexusEchoes.MOD_ID, "anomaly_ward"), ANOMALY_WARD,
                    new ResourceLocation(NexusEchoes.MOD_ID, "resonance_scanner"), RESONANCE_SCANNER);

    /**
     * Placed block id → technology required to place it.
     */
    public static final Map<ResourceLocation, ResourceLocation> PLACE_GATE = CRAFT_GATE;

    private ResearchTechnologies() {
    }
}
