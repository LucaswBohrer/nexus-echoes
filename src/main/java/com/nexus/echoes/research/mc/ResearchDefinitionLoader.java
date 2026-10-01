package com.nexus.echoes.research.mc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nexus.echoes.research.ResearchDefinition;
import com.nexus.echoes.research.ResearchGraph;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data-driven research definitions: {@code data/<ns>/research/*.json}.
 *
 * <p>Loaded on both logical sides (datapacks sync to clients); the server
 * copy is authoritative for progression logic, the client copy feeds the GUI.
 * Malformed definitions fail fast with a clear log + exception — a broken
 * datapack must never produce silent partial progression.
 */
public class ResearchDefinitionLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOG = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private static volatile ResearchGraph graph;

    public ResearchDefinitionLoader() {
        super(GSON, "research");
    }

    /** Last successfully loaded graph, or {@code null} before first reload. */
    public static ResearchGraph get() {
        return graph;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resources,
                         ProfilerFiller profiler) {
        List<ResearchDefinition> defs = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            try {
                defs.add(parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            } catch (Exception e) {
                LOG.error("[nexus_echoes] invalid research definition {}: {}",
                        entry.getKey(), e.getMessage());
                throw new IllegalStateException(
                        "invalid research definition " + entry.getKey() + ": " + e.getMessage(), e);
            }
        }
        graph = ResearchGraph.validate(defs);
        LOG.info("[nexus_echoes] loaded {} research definitions", defs.size());
    }

    /**
     * Parses one definition. The definition id is derived from the file path
     * ({@code data/<ns>/research/<path>.json} → {@code <ns>:<path>}); an
     * explicit {@code "id"} field must match when present.
     *
     * @throws IllegalArgumentException on any malformed field
     */
    public static ResearchDefinition parse(ResourceLocation fileId, JsonObject json) {
        String dir = "research/";
        if (!fileId.getPath().startsWith(dir)) {
            throw new IllegalArgumentException("research file outside research/ directory: " + fileId);
        }
        ResourceLocation id = new ResourceLocation(
                fileId.getNamespace(), fileId.getPath().substring(dir.length()));
        if (json.has("id")) {
            ResourceLocation declared = parseId(json.get("id").getAsString(), "id");
            if (!declared.equals(id)) {
                throw new IllegalArgumentException(
                        "declared id " + declared + " does not match file path id " + id);
            }
        }
        String title = stringField(json, "title");
        String description = stringField(json, "description");
        String category = stringField(json, "category");
        if (!json.has("cost")) {
            throw new IllegalArgumentException("missing cost for " + id);
        }
        int cost = json.get("cost").getAsInt();
        List<ResourceLocation> prerequisites = idList(json, "requirements", id);
        List<ResourceLocation> unlocks = idList(json, "unlocks", id);
        return new ResearchDefinition(id, title, description, category, cost, prerequisites, unlocks);
    }

    private static String stringField(JsonObject json, String name) {
        if (!json.has(name) || json.get(name).getAsString().isBlank()) {
            throw new IllegalArgumentException("missing or blank \"" + name + "\"");
        }
        return json.get(name).getAsString();
    }

    private static List<ResourceLocation> idList(JsonObject json, String name, ResourceLocation owner) {
        List<ResourceLocation> ids = new ArrayList<>();
        if (!json.has(name)) {
            return ids;
        }
        JsonArray arr = json.getAsJsonArray(name);
        for (JsonElement el : arr) {
            ids.add(parseId(el.getAsString(), name + " of " + owner));
        }
        return ids;
    }

    private static ResourceLocation parseId(String raw, String what) {
        if (raw == null || !ResourceLocation.isValidResourceLocation(raw)) {
            throw new IllegalArgumentException("malformed id in " + what + ": " + raw);
        }
        return new ResourceLocation(raw);
    }
}
