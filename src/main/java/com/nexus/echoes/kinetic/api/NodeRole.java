package com.nexus.echoes.kinetic.api;

/**
 * What a node does in a kinetic network.
 */
public enum NodeRole {
    /** Produces mechanical flow (rated RPM/torque). Ideal constant-RPM source in Phase 2. */
    SOURCE,
    /** Transforms mechanical flow (ratio, direction, efficiency, switching). */
    TRANSMISSION,
    /** Consumes mechanical flow (requires RPM + torque to run). Sinks flow. */
    CONSUMER
}
