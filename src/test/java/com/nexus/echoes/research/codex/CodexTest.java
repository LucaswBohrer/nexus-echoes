package com.nexus.echoes.research.codex;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nexus.echoes.research.PlayerResearchState;
import com.nexus.echoes.research.mc.CodexLoader;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Codex entry loading and visibility rules. */
class CodexTest {

    private static final Gson GSON = new Gson();

    private static JsonObject json(String raw) {
        return GSON.fromJson(raw, JsonObject.class);
    }

    private static ResourceLocation file(String name) {
        return new ResourceLocation("nexus_echoes", "codex/" + name);
    }

    private static String entry(String extra) {
        return "{\"title\":\"T\",\"category\":\"c\"," + extra
                + "\"content\":[\"line one\",\"line two\"]}";
    }

    @Test
    void validEntryParses() {
        CodexEntry e = CodexLoader.parse(file("welcome"), json(entry("")));
        assertEquals(new ResourceLocation("nexus_echoes", "welcome"), e.id());
        assertEquals(2, e.content().size());
        assertNull(e.requiredResearch());
    }

    @Test
    void entryWithRequirementParses() {
        CodexEntry e = CodexLoader.parse(file("x"),
                json(entry("\"requiredResearch\":\"nexus_echoes:foundations\",")));
        assertEquals(new ResourceLocation("nexus_echoes", "foundations"), e.requiredResearch());
    }

    @Test
    void missingContentRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> CodexLoader.parse(file("x"), json("{\"title\":\"T\",\"category\":\"c\"}")));
    }

    @Test
    void blankTitleRejected() {
        assertThrows(IllegalArgumentException.class, () -> CodexLoader.parse(file("x"),
                json("{\"title\":\"\",\"category\":\"c\",\"content\":[\"x\"]}")));
    }

    @Test
    void malformedRequiredResearchRejected() {
        assertThrows(IllegalArgumentException.class, () -> CodexLoader.parse(file("x"),
                json(entry("\"requiredResearch\":\"bogus!!!\","))));
    }

    @Test
    void blankLinesFiltered() {
        CodexEntry e = CodexLoader.parse(file("x"),
                json("{\"title\":\"T\",\"category\":\"c\",\"content\":[\"a\",\" \",\"b\"]}"));
        assertEquals(List.of("a", "b"), e.content());
    }

    @Test
    void entryWithoutRequirementAlwaysVisible() {
        CodexEntry e = CodexLoader.parse(file("x"), json(entry("")));
        assertTrue(CodexVisibility.isVisible(e, new PlayerResearchState()));
    }

    @Test
    void entryHiddenUntilRequirementMet() {
        CodexEntry e = CodexLoader.parse(file("x"),
                json(entry("\"requiredResearch\":\"nexus_echoes:foundations\",")));
        PlayerResearchState state = new PlayerResearchState();
        assertFalse(CodexVisibility.isVisible(e, state));
        state.markCompleted(new ResourceLocation("nexus_echoes", "foundations"));
        assertTrue(CodexVisibility.isVisible(e, state));
    }

    @Test
    void unknownRequirementStaysHidden() {
        CodexEntry e = CodexLoader.parse(file("x"),
                json(entry("\"requiredResearch\":\"nexus_echoes:ghost\",")));
        assertFalse(CodexVisibility.isVisible(e, new PlayerResearchState()));
    }

    @Test
    void shippedEntriesLoadCleanly() throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of("src/main/resources/data/nexus_echoes/codex");
        int count = 0;
        try (var stream = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path p : stream.filter(q -> q.toString().endsWith(".json")).sorted().toList()) {
                String name = p.getFileName().toString().replace(".json", "");
                JsonObject obj = GSON.fromJson(java.nio.file.Files.readString(p), JsonObject.class);
                CodexEntry e = CodexLoader.parse(file(name), obj);
                assertFalse(e.content().isEmpty());
                count++;
            }
        }
        assertEquals(6, count);
    }

    @Test
    void shippedLoreEntriesAreGated() throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of("src/main/resources/data/nexus_echoes/codex");
        PlayerResearchState fresh = new PlayerResearchState();
        int gated = 0;
        int open = 0;
        try (var stream = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path p : stream.filter(q -> q.toString().endsWith(".json")).toList()) {
                String name = p.getFileName().toString().replace(".json", "");
                JsonObject obj = GSON.fromJson(java.nio.file.Files.readString(p), JsonObject.class);
                CodexEntry e = CodexLoader.parse(file(name), obj);
                if (CodexVisibility.isVisible(e, fresh)) {
                    open++;
                } else {
                    gated++;
                }
            }
        }
        assertTrue(open > 0, "some entries must be readable immediately");
        assertTrue(gated > 0, "some entries must be locked behind research");
    }
}
