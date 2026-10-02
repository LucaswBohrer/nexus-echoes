package com.nexus.echoes.dimension.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hollow datapack integrity (Phase 5): every JSON parses, carries the
 * fields its codec requires, and cross-references resolve to known ids.
 *
 * <p>This is schema-level validation, not runtime worldgen — the codecs
 * themselves only run inside a live game.
 */
class HollowWorldgenJsonTest {

    private static final Path DATA = Path.of("src/main/resources/data/nexus_echoes");
    private static final Path ASSETS = Path.of("src/main/resources/assets/nexus_echoes");

    private static JsonObject read(Path path) {
        assertTrue(Files.isRegularFile(path), "missing file: " + path);
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("unreadable file: " + path, e);
        }
    }

    private static void assertParses(Path path) {
        assertDoesNotThrow(() -> read(path), "invalid JSON: " + path);
    }

    // ------------------------------------------------------------ dimension

    @Test
    void dimensionTypeHasRequiredFields() {
        JsonObject json = read(DATA.resolve("dimension_type/hollow.json"));
        for (String field : List.of("ultrawarm", "natural",
                "respawn_anchor_works", "bed_works",
                "has_skylight", "has_ceiling",
                "coordinate_scale", "ambient_light", "logical_height",
                "effects", "infiniburn", "min_y", "height",
                "monster_settings")) {
            assertTrue(json.has(field), "dimension_type missing: " + field);
        }
        assertTrue(json.get("bed_works").getAsBoolean());
        assertFalse(json.get("respawn_anchor_works").getAsBoolean());
        assertEquals("nexus_echoes:the_hollow", json.get("effects").getAsString());
        JsonObject monsters = json.getAsJsonObject("monster_settings");
        for (String field : List.of("piglin_safe",
                "monster_spawn_light_level", "monster_spawn_block_light_limit")) {
            assertTrue(monsters.has(field), "monster_settings missing: " + field);
        }
        // 1.20.1 DimensionType.MonsterSettings codec has exactly these 3 fields.
        // Unknown fields fail strict registry parsing at runtime
        // (crashed world creation: "Failed to parse dimension_type/hollow.json").
        assertEquals(Set.of("piglin_safe",
                "monster_spawn_light_level", "monster_spawn_block_light_limit"),
                monsters.keySet(), "monster_settings has unknown fields");
    }

    @Test
    void dimensionReferencesType() {
        JsonObject json = read(DATA.resolve("dimension/hollow.json"));
        assertEquals("nexus_echoes:hollow", json.get("type").getAsString());
        assertTrue(json.has("generator"));
    }

    @Test
    void allFourBiomesParseWithEffects() {
        for (String biome : List.of("hollow_wastes", "machine_graveyard",
                "resonant_forest", "deep_hollow")) {
            JsonObject json = read(DATA.resolve("worldgen/biome/" + biome + ".json"));
            assertTrue(json.has("effects"), biome + " missing effects");
            assertTrue(json.has("spawners"), biome + " missing spawners");
            // 1.20.1 biome codec inlines generation settings: flat "carvers"/"features".
            assertTrue(json.has("features"), biome + " missing features");
            assertTrue(json.getAsJsonObject("effects").has("fog_color"),
                    biome + " effects missing fog_color");
        }
    }

    // ------------------------------------------------------------- features

    @Test
    void configuredAndPlacedFeaturesPairUp() {
        Path configured = DATA.resolve("worldgen/configured_feature");
        Path placed = DATA.resolve("worldgen/placed_feature");
        for (String name : List.of("obelisk", "ruin", "vault", "fracture_spire",
                "resonant_growth_patch", "hollow_ore")) {
            JsonObject cfg = read(configured.resolve(name + ".json"));
            String type = cfg.get("type").getAsString();
            // Custom landmarks are nexus_echoes:*; vanilla decorators (e.g. random_patch)
            // legitimately use minecraft:* types.
            assertTrue(type.startsWith("nexus_echoes:") || type.startsWith("minecraft:"),
                    name + " feature type not namespaced: " + type);
            JsonObject plc = read(placed.resolve(name + ".json"));
            assertEquals("nexus_echoes:" + name,
                    plc.get("feature").getAsString());
            assertTrue(plc.has("placement"), name + " missing placement");
        }
    }

    // -------------------------------------------------------------- recipes

    @Test
    void hollowRecipesParseAndReferenceKnownItems() {
        Set<String> knownItems = Set.of(
                "nexus_echoes:resonant_crystal", "minecraft:redstone",
                "minecraft:iron_ingot", "nexus_echoes:nexus_component",
                "nexus_echoes:resonant_shard", "nexus_echoes:nexus_plate",
                "nexus_echoes:memory_fragment",
                "nexus_echoes:dimensional_spire", "nexus_echoes:anomaly_ward",
                "nexus_echoes:resonance_scanner");
        Path dir = DATA.resolve("recipes/hollow");
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> jsons = files.filter(p -> p.toString().endsWith(".json")).toList();
            assertFalse(jsons.isEmpty());
            for (Path path : jsons) {
                JsonObject json = read(path);
                assertTrue(json.has("type"), path + " missing type");
                assertTrue(json.has("result"), path + " missing result");
                String result = json.getAsJsonObject("result").get("item").getAsString();
                assertTrue(knownItems.contains(result),
                        path + " unknown result item: " + result);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void resonatingIntegrationParses() {
        JsonObject json = read(DATA.resolve("recipes/resonating/shard_to_crystal_hollow.json"));
        assertEquals("nexus_echoes:resonating", json.get("type").getAsString());
        assertEquals("nexus_echoes:resonant_shard",
                json.getAsJsonObject("ingredient").get("item").getAsString());
    }

    // -------------------------------------------------------------- research

    @Test
    void hollowResearchBranchIsWellFormed() {
        Set<String> knownTech = Set.of("nexus_echoes:crusher", "nexus_echoes:processor",
                "nexus_echoes:nexus_component", "nexus_echoes:hollow_access",
                "nexus_echoes:anomaly_ward", "nexus_echoes:resonance_scanner");
        for (String name : List.of("hollow_exploration", "anomaly_studies")) {
            JsonObject json = read(DATA.resolve("research/" + name + ".json"));
            for (String field : List.of("id", "title", "description", "category",
                    "cost", "requirements", "unlocks")) {
                assertTrue(json.has(field), name + " missing: " + field);
            }
            json.getAsJsonArray("unlocks").forEach(el ->
                    assertTrue(knownTech.contains(el.getAsString()),
                            name + " unlocks unknown tech: " + el));
        }
        JsonObject exploration = read(DATA.resolve("research/hollow_exploration.json"));
        assertEquals("nexus_echoes:dimensional_resonance",
                exploration.getAsJsonArray("requirements").get(0).getAsString());
    }

    // ----------------------------------------------------------------- codex

    @Test
    void hollowCodexEntriesGateOnKnownDiscoveries() {
        Set<String> knownDiscoveries = Set.of(
                "nexus_echoes:enter_hollow", "nexus_echoes:obelisk",
                "nexus_echoes:ruin_found", "nexus_echoes:anomaly_seen",
                "nexus_echoes:deep_hollow", "nexus_echoes:memory_fragment");
        for (String name : List.of("the_hollow", "obelisks", "anomalies",
                "deep_hollow", "memory_fragments")) {
            JsonObject json = read(DATA.resolve("codex/" + name + ".json"));
            assertTrue(json.has("title") && json.has("content"), name + " malformed");
            assertTrue(json.getAsJsonArray("content").size() > 0);
            if (json.has("requiredDiscovery")) {
                assertTrue(knownDiscoveries.contains(json.get("requiredDiscovery").getAsString()),
                        name + " gates on unknown discovery");
            }
        }
    }

    // ------------------------------------------------------------ loot/lang

    @Test
    void lootTablesParse() {
        for (String name : List.of("hollow_stone", "hollow_ore", "dimensional_spire",
                "obelisk_core", "anomaly_ward", "unstable_fracture", "resonant_growth")) {
            JsonObject json = read(DATA.resolve("loot_tables/blocks/" + name + ".json"));
            assertEquals("minecraft:block", json.get("type").getAsString());
            assertTrue(json.getAsJsonArray("pools").size() > 0);
        }
        JsonObject vault = read(DATA.resolve("loot_tables/chests/vault.json"));
        assertEquals("minecraft:chest", vault.get("type").getAsString());
        for (String mob : List.of("scrap_crawler", "hollow_stalker", "resonant_wisp", "rift_phantom")) {
            JsonObject json = read(DATA.resolve("loot_tables/entities/" + mob + ".json"));
            assertEquals("minecraft:entity", json.get("type").getAsString());
        }
    }

    @Test
    void langCoversDimensionMessages() {
        JsonObject en = read(ASSETS.resolve("lang/en_us.json"));
        JsonObject pt = read(ASSETS.resolve("lang/pt_br.json"));
        for (String key : List.of(
                "block.nexus_echoes.dimensional_spire",
                "block.nexus_echoes.obelisk_core",
                "block.nexus_echoes.anomaly_ward",
                "item.nexus_echoes.memory_fragment",
                "item.nexus_echoes.resonance_scanner",
                "item.nexus_echoes.resonant_shard",
                "entity.nexus_echoes.scrap_crawler",
                "entity.nexus_echoes.rift_phantom",
                "message.nexus_echoes.hollow_locked",
                "message.nexus_echoes.spire_charging",
                "message.nexus_echoes.scanner_silence")) {
            assertTrue(en.has(key), "en missing: " + key);
            assertTrue(pt.has(key), "pt missing: " + key);
        }
    }

    @Test
    void texturesExistForPhase5BlocksAndItems() {
        for (String name : List.of("hollow_stone", "rusted_plating", "ashen_soil",
                "hollow_ore", "dimensional_spire_side", "dimensional_spire_top",
                "obelisk_core", "anomaly_ward_side", "anomaly_ward_top", "anomaly_ward_front",
                "unstable_fracture", "resonant_growth")) {
            assertTrue(Files.isRegularFile(
                    ASSETS.resolve("textures/block/" + name + ".png")), "missing tex: " + name);
        }
        for (String name : List.of("memory_fragment", "resonant_shard", "resonance_scanner")) {
            assertTrue(Files.isRegularFile(
                    ASSETS.resolve("textures/item/" + name + ".png")), "missing tex: " + name);
        }
    }
}
