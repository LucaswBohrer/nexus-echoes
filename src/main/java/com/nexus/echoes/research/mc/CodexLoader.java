package com.nexus.echoes.research.mc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nexus.echoes.research.codex.CodexEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven Codex entries: {@code data/<ns>/codex/*.json}.
 *
 * <p>Same loading contract as research definitions: both sides load, malformed
 * entries fail fast, entry ids come from the file path.
 */
public class CodexLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOG = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private static volatile Map<ResourceLocation, CodexEntry> entries = Map.of();

    public CodexLoader() {
        super(GSON, "codex");
    }

    /** Last successfully loaded entries (insertion order), never null. */
    public static Map<ResourceLocation, CodexEntry> get() {
        return entries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resources,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, CodexEntry> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            try {
                CodexEntry parsed = parse(entry.getKey(), entry.getValue().getAsJsonObject());
                if (loaded.put(parsed.id(), parsed) != null) {
                    throw new IllegalArgumentException("duplicate codex id: " + parsed.id());
                }
            } catch (Exception e) {
                LOG.error("[nexus_echoes] invalid codex entry {}: {}", entry.getKey(), e.getMessage());
                throw new IllegalStateException(
                        "invalid codex entry " + entry.getKey() + ": " + e.getMessage(), e);
            }
        }
        entries = Collections.unmodifiableMap(loaded);
        LOG.info("[nexus_echoes] loaded {} codex entries", loaded.size());
    }

    /**
     * Parses one entry; id from file path, optional {@code requiredResearch}
     * must be a valid namespaced id when present.
     *
     * @throws IllegalArgumentException on any malformed field
     */
    public static CodexEntry parse(ResourceLocation fileId, JsonObject json) {
        String dir = "codex/";
        if (!fileId.getPath().startsWith(dir)) {
            throw new IllegalArgumentException("codex file outside codex/ directory: " + fileId);
        }
        ResourceLocation id = new ResourceLocation(
                fileId.getNamespace(), fileId.getPath().substring(dir.length()));
        String title = stringField(json, "title");
        String category = stringField(json, "category");
        if (!json.has("content")) {
            throw new IllegalArgumentException("missing content for " + id);
        }
        JsonArray arr = json.getAsJsonArray("content");
        List<String> content = new ArrayList<>();
        for (JsonElement el : arr) {
            String line = el.getAsString();
            if (!line.isBlank()) {
                content.add(line);
            }
        }
        ResourceLocation required = null;
        if (json.has("requiredResearch")) {
            String raw = json.get("requiredResearch").getAsString();
            if (!ResourceLocation.isValidResourceLocation(raw)) {
                throw new IllegalArgumentException("malformed requiredResearch: " + raw);
            }
            required = new ResourceLocation(raw);
        }
        return new CodexEntry(id, title, category, content, required);
    }

    private static String stringField(JsonObject json, String name) {
        if (!json.has(name) || json.get(name).getAsString().isBlank()) {
            throw new IllegalArgumentException("missing or blank \"" + name + "\"");
        }
        return json.get(name).getAsString();
    }
}
