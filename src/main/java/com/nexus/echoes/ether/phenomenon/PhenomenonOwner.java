package com.nexus.echoes.ether.phenomenon;

/**
 * Ownership of an Ether phenomenon (Phase 6B plan v2, S4).
 *
 * <p>Explicit per the Phase 6A gate (C4): every phenomenon declares its
 * owner. In v1 all phenomena are world-owned — the Ether dimension itself
 * is the owner and the server is authoritative. Player-owned phenomena are
 * not planned for v1; this enum extends if they ever are.
 */
public enum PhenomenonOwner {

    /** Owned by the Ether dimension; managed by the server-side manager. */
    WORLD
}
