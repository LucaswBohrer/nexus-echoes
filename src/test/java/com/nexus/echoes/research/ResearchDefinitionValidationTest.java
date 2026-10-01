package com.nexus.echoes.research;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nexus.echoes.research.mc.ResearchDefinitionLoader;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Data-driven definition validation: malformed JSON fails clearly. */
class ResearchDefinitionValidationTest {

    private static final Gson GSON = new Gson();

    private static JsonObject json(String raw) {
        return GSON.fromJson(raw, JsonObject.class);
    }

    private static ResourceLocation file(String path) {
        return new ResourceLocation("nexus_echoes", "research/" + path);
    }

    private static String valid(String idPath) {
        return "{\"title\":\"T\",\"description\":\"D\",\"category\":\"c\",\"cost\":50,"
                + "\"requirements\":[],\"unlocks\":[\"nexus_echoes:tech\"]}";
    }

    @Test
    void validDefinitionParses() {
        ResearchDefinition def = ResearchDefinitionLoader.parse(file("foundations"), json(valid("foundations")));
        assertEquals(new ResourceLocation("nexus_echoes", "foundations"), def.id());
        assertEquals(50, def.cost());
        assertTrue(def.prerequisites().isEmpty());
        assertEquals(List.of(new ResourceLocation("nexus_echoes", "tech")), def.unlocks());
    }

    @Test
    void idDerivedFromFilePath() {
        ResearchDefinition def = ResearchDefinitionLoader.parse(
                file("my_research"), json(valid("my_research")));
        assertEquals("nexus_echoes:my_research", def.id().toString());
    }

    @Test
    void declaredIdMustMatchFilePath() {
        String raw = "{\"id\":\"nexus_echoes:other\",\"title\":\"T\",\"description\":\"D\","
                + "\"category\":\"c\",\"cost\":50,\"requirements\":[],\"unlocks\":[]}";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ResearchDefinitionLoader.parse(file("foundations"), json(raw)));
        assertTrue(e.getMessage().contains("does not match"));
    }

    @Test
    void matchingDeclaredIdAccepted() {
        String raw = "{\"id\":\"nexus_echoes:foundations\",\"title\":\"T\",\"description\":\"D\","
                + "\"category\":\"c\",\"cost\":50,\"requirements\":[],\"unlocks\":[]}";
        ResearchDefinition def = ResearchDefinitionLoader.parse(file("foundations"), json(raw));
        assertEquals(new ResourceLocation("nexus_echoes", "foundations"), def.id());
    }

    @Test
    void missingCostRejected() {
        String raw = "{\"title\":\"T\",\"description\":\"D\",\"category\":\"c\","
                + "\"requirements\":[],\"unlocks\":[]}";
        assertThrows(IllegalArgumentException.class,
                () -> ResearchDefinitionLoader.parse(file("x"), json(raw)));
    }

    @Test
    void zeroCostRejected() {
        String raw = "{\"title\":\"T\",\"description\":\"D\",\"category\":\"c\",\"cost\":0,"
                + "\"requirements\":[],\"unlocks\":[]}";
        assertThrows(IllegalArgumentException.class,
                () -> ResearchDefinitionLoader.parse(file("x"), json(raw)));
    }

    @Test
    void negativeCostRejected() {
        String raw = "{\"title\":\"T\",\"description\":\"D\",\"category\":\"c\",\"cost\":-10,"
                + "\"requirements\":[],\"unlocks\":[]}";
        assertThrows(IllegalArgumentException.class,
                () -> ResearchDefinitionLoader.parse(file("x"), json(raw)));
    }

    @Test
    void blankTitleRejected() {
        String raw = "{\"title\":\" \",\"description\":\"D\",\"category\":\"c\",\"cost\":10,"
                + "\"requirements\":[],\"unlocks\":[]}";
        assertThrows(IllegalArgumentException.class,
                () -> ResearchDefinitionLoader.parse(file("x"), json(raw)));
    }

    @Test
    void malformedPrerequisiteIdRejected() {
        String raw = "{\"title\":\"T\",\"description\":\"D\",\"category\":\"c\",\"cost\":10,"
                + "\"requirements\":[\"not an id!!!\"],\"unlocks\":[]}";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ResearchDefinitionLoader.parse(file("x"), json(raw)));
        assertTrue(e.getMessage().contains("malformed id"));
    }

    @Test
    void malformedUnlockIdRejected() {
        String raw = "{\"title\":\"T\",\"description\":\"D\",\"category\":\"c\",\"cost\":10,"
                + "\"requirements\":[],\"unlocks\":[\"UPPERCASE\"]}";
        assertThrows(IllegalArgumentException.class,
                () -> ResearchDefinitionLoader.parse(file("x"), json(raw)));
    }

    @Test
    void unknownPrerequisiteRejectedAtGraphLevel() {
        ResearchDefinition def = ResearchDefinitionLoader.parse(
                file("x"), json("{\"title\":\"T\",\"description\":\"D\",\"category\":\"c\",\"cost\":10,"
                        + "\"requirements\":[\"nexus_echoes:ghost\"],\"unlocks\":[]}"));
        assertThrows(IllegalArgumentException.class,
                () -> ResearchGraph.validate(List.of(def)));
    }

    @Test
    void fileOutsideResearchDirRejected() {
        assertThrows(IllegalArgumentException.class, () -> ResearchDefinitionLoader.parse(
                new ResourceLocation("nexus_echoes", "codex/x"), json(valid("x"))));
    }

    @Test
    void duplicateDefinitionsRejected() {
        ResearchDefinition a = ResearchDefinitionLoader.parse(file("x"), json(valid("x")));
        ResearchDefinition b = ResearchDefinitionLoader.parse(file("x"), json(valid("x")));
        assertThrows(IllegalArgumentException.class,
                () -> ResearchGraph.validate(List.of(a, b)));
    }

    @Test
    void shippedDefinitionsLoadCleanly() throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of(
                "src/main/resources/data/nexus_echoes/research");
        List<ResearchDefinition> defs = new java.util.ArrayList<>();
        try (var stream = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path p : stream.filter(q -> q.toString().endsWith(".json")).sorted().toList()) {
                String name = p.getFileName().toString().replace(".json", "");
                JsonObject obj = GSON.fromJson(java.nio.file.Files.readString(p), JsonObject.class);
                defs.add(ResearchDefinitionLoader.parse(file(name), obj));
            }
        }
        ResearchGraph graph = ResearchGraph.validate(defs);
        // every shipped file parses and validates: the graph holds exactly the
        // files on disk (6 after Phase 5 added the hollow branch).
        assertEquals(defs.size(), graph.size());
        assertTrue(graph.size() >= 6, "expected at least the 6 shipped researches");
        // the intended progression chain holds
        List<ResourceLocation> order = graph.topologicalOrder();
        assertTrue(order.indexOf(new ResourceLocation("nexus_echoes:industrial_foundations"))
                < order.indexOf(new ResourceLocation("nexus_echoes:kinetic_transmission")));
        assertTrue(order.indexOf(new ResourceLocation("nexus_echoes:kinetic_transmission"))
                < order.indexOf(new ResourceLocation("nexus_echoes:advanced_processing")));
        assertTrue(order.indexOf(new ResourceLocation("nexus_echoes:advanced_processing"))
                < order.indexOf(new ResourceLocation("nexus_echoes:dimensional_resonance")));
    }
}
