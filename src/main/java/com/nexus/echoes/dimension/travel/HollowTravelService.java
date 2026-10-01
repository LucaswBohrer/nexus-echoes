package com.nexus.echoes.dimension.travel;

import com.nexus.echoes.research.PlayerResearchState;
import com.nexus.echoes.research.ResearchGraph;
import com.nexus.echoes.research.ResearchService;
import com.nexus.echoes.research.ResearchTechnologies;

import java.util.Optional;

/**
 * Pure dimensional-travel rules (ADR-011).
 *
 * <p>No players, no levels, no packets: permission is a research query and
 * safe-spawn is a search over an injected height function, so all of it is
 * unit-testable without Minecraft.
 */
public final class HollowTravelService {

    /** How far the spiral search may wander from the requested column. */
    public static final int MAX_SPAWN_SEARCH_RADIUS = 16;
    /** How far above the surface the search tolerates air before giving up. */
    public static final int MAX_SPAWN_HEADROOM = 12;

    private HollowTravelService() {
    }

    /**
     * May this player cross into The Hollow? This is the technology gate for
     * dimensional travel — the same choke point as every other technology
     * (ADR-010). The MC adapter must call it server-side.
     */
    public static boolean canTravel(ResearchGraph graph, PlayerResearchState state) {
        return ResearchService.isUnlocked(graph, state, ResearchTechnologies.HOLLOW_ACCESS);
    }

    /** World-shape probe supplied by the MC adapter (or a test fake). */
    public interface HeightQuery {
        /** Highest solid-or-liquid surface Y at the column (motion blocking). */
        int surfaceY(int x, int z);

        /** True when the block at (x, y, z) would suffocate or trap a player. */
        boolean isObstructed(int x, int y, int z);
    }

    /** A validated arrival point: feet Y with two free blocks above solid ground. */
    public record SafeSpawn(int x, int y, int z) {
    }

    /**
     * Finds a safe arrival column near (x, z). Checks the requested column
     * first, then spirals outward. A column is safe when the surface block is
     * not obstructed and the two blocks above it are free. Empty when nothing
     * within the search radius is safe — the adapter must then refuse travel
     * rather than bury the player.
     */
    public static Optional<SafeSpawn> findSafeSpawn(int x, int z, HeightQuery query) {
        if (query == null) {
            return Optional.empty();
        }
        Optional<SafeSpawn> direct = checkColumn(x, z, query);
        if (direct.isPresent()) {
            return direct;
        }
        for (int r = 1; r <= MAX_SPAWN_SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue; // ring only
                    }
                    Optional<SafeSpawn> found = checkColumn(x + dx, z + dz, query);
                    if (found.isPresent()) {
                        return found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<SafeSpawn> checkColumn(int x, int z, HeightQuery query) {
        int surface = query.surfaceY(x, z);
        // feet may stand on the surface block or a few blocks above it
        for (int dy = 1; dy <= MAX_SPAWN_HEADROOM; dy++) {
            int feet = surface + dy;
            if (!query.isObstructed(x, feet, z)
                    && !query.isObstructed(x, feet + 1, z)) {
                return Optional.of(new SafeSpawn(x, feet, z));
            }
        }
        return Optional.empty();
    }
}
