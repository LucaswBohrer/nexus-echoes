package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Research-point accounting: gains, spending, invariants. */
class PlayerResearchStateTest {

    @Test
    void initialStateIsEmpty() {
        PlayerResearchState state = new PlayerResearchState();
        assertEquals(0, state.points());
        assertTrue(state.completed().isEmpty());
        assertTrue(state.claimedSources().isEmpty());
    }

    @Test
    void positiveGainAddsPoints() {
        PlayerResearchState state = new PlayerResearchState();
        assertTrue(state.addPoints(10));
        assertTrue(state.addPoints(5));
        assertEquals(15, state.points());
    }

    @Test
    void zeroGainRejected() {
        PlayerResearchState state = new PlayerResearchState();
        assertFalse(state.addPoints(0));
        assertEquals(0, state.points());
    }

    @Test
    void negativeGainRejected() {
        PlayerResearchState state = new PlayerResearchState();
        assertTrue(state.addPoints(10));
        assertFalse(state.addPoints(-4));
        assertEquals(10, state.points());
    }

    @Test
    void spendExactBalance() {
        PlayerResearchState state = new PlayerResearchState();
        state.addPoints(50);
        assertTrue(state.spendPoints(50));
        assertEquals(0, state.points());
    }

    @Test
    void spendPartialBalance() {
        PlayerResearchState state = new PlayerResearchState();
        state.addPoints(50);
        assertTrue(state.spendPoints(20));
        assertEquals(30, state.points());
    }

    @Test
    void insufficientSpendRejectedWithoutChange() {
        PlayerResearchState state = new PlayerResearchState();
        state.addPoints(10);
        assertFalse(state.spendPoints(11));
        assertEquals(10, state.points());
    }

    @Test
    void spendOnEmptyBalanceRejected() {
        PlayerResearchState state = new PlayerResearchState();
        assertFalse(state.spendPoints(1));
        assertEquals(0, state.points());
    }

    @Test
    void zeroOrNegativeSpendRejected() {
        PlayerResearchState state = new PlayerResearchState();
        state.addPoints(10);
        assertFalse(state.spendPoints(0));
        assertFalse(state.spendPoints(-3));
        assertEquals(10, state.points());
    }

    @Test
    void balanceNeverGoesNegative() {
        PlayerResearchState state = new PlayerResearchState();
        state.addPoints(5);
        for (int i = 0; i < 10; i++) {
            state.spendPoints(5);
        }
        assertTrue(state.points() >= 0);
    }

    @Test
    void duplicateCompletionReturnsFalse() {
        PlayerResearchState state = new PlayerResearchState();
        ResourceLocation id = new ResourceLocation("nexus_echoes", "x");
        assertTrue(state.markCompleted(id));
        assertFalse(state.markCompleted(id));
        assertEquals(1, state.completed().size());
    }

    @Test
    void duplicateSourceClaimReturnsFalse() {
        PlayerResearchState state = new PlayerResearchState();
        assertTrue(state.claimSource("s"));
        assertFalse(state.claimSource("s"));
    }

    @Test
    void completedViewIsUnmodifiable() {
        PlayerResearchState state = new PlayerResearchState();
        assertThrows(UnsupportedOperationException.class,
                () -> state.completed().add(new ResourceLocation("nexus_echoes", "x")));
    }
}
