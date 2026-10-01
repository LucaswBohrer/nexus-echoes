package com.nexus.echoes.research;

/** Machine-readable outcome of a research service operation. */
public enum ResearchOutcome {
    OK,
    UNKNOWN_RESEARCH,
    ALREADY_COMPLETED,
    MISSING_PREREQUISITE,
    INSUFFICIENT_POINTS,
    INVALID_AMOUNT
}
