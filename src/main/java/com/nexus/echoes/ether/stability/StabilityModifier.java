package com.nexus.echoes.ether.stability;

import java.util.Objects;

/**
 * A temporary local stability modifier contributed by an active Ether
 * phenomenon (Phase 6, S3).
 *
 * <p>Pure domain: the id is a plain {@code String} (never a
 * {@code ResourceLocation}) and there is no position knowledge here.
 * Modifiers are <b>derived on demand</b> from active phenomena by the
 * adapter layer; they are never stored per-block.
 */
public record StabilityModifier(String id, int magnitude, long expiresTick) {

    public StabilityModifier {
        Objects.requireNonNull(id, "id");
    }

    /** True when this modifier no longer applies at {@code nowTick}. */
    public boolean isExpired(long nowTick) {
        return expiresTick <= nowTick;
    }
}
