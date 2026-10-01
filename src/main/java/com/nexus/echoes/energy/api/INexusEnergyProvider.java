package com.nexus.echoes.energy.api;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented by block entities that expose NEXUS energy to neighbors.
 *
 * <p>Phase 1 transfer is neighbor-push only (see ADR-005); this interface is
 * the single lookup point so the future network graph can reuse it.
 */
public interface INexusEnergyProvider {

    /**
     * @param direction the side being queried from, or null for internal access
     * @return the energy storage, or null if this side exposes none
     */
    @Nullable
    INexusEnergy getNexusEnergy(@Nullable Direction direction);
}
