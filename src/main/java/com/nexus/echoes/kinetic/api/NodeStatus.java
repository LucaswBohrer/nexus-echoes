package com.nexus.echoes.kinetic.api;

/**
 * Operational status of a single node after a network simulation pass.
 */
public enum NodeStatus {
    /** Powered and within spec (consumer running, source feeding, transmission linked). */
    OK,
    /** No mechanical flow reaches this node (disconnected, or upstream clutch open). */
    NO_INPUT,
    /** Flow arrives but is insufficient (RPM too low or torque below requirement). */
    UNDERPOWERED,
    /** Source whose total downstream demand exceeds its rated torque (brownout). */
    OVERLOADED,
    /** Deliberately switched off (source disabled, clutch disengaged path). */
    DISABLED,
    /** Two or more enabled sources feed this node; lowest-id source wins (Phase 2 rule). */
    CONFLICT
}
