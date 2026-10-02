package com.nexus.echoes.ether;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-domain boundary guard (Phase 6, S0–S4; Audit A).
 *
 * <p>The pure packages ({@code resonance}, {@code stability},
 * {@code phenomenon}, {@code travel}) must contain zero
 * {@code net.minecraft} and zero Forge imports. This is a source-level
 * check: it fails loudly if the sources cannot be located, never silently.
 */
class EtherPureBoundaryTest {

    /**
     * Pure packages present in this commit. Extended to include
     * {@code travel} (and others) as later stages land.
     */
    private static final List<String> PURE_PACKAGES = List.of(
            "resonance", "stability", "phenomenon");

    @Test
    void purePackagesHaveNoMinecraftOrForgeImports() throws IOException {
        Path root = Path.of(System.getProperty("user.dir"))
                .resolve("src/main/java/com/nexus/echoes/ether");
        assertTrue(Files.isDirectory(root),
                "ether sources not found under user.dir=" + System.getProperty("user.dir"));

        for (String pkg : PURE_PACKAGES) {
            Path dir = root.resolve(pkg);
            assertTrue(Files.isDirectory(dir), "pure package missing: " + pkg);
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    List<String> lines = Files.readAllLines(file);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        assertFalse(trimmed.startsWith("import net.minecraft"),
                                file.getFileName() + " must not import net.minecraft: " + trimmed);
                        assertFalse(trimmed.startsWith("import net.minecraftforge"),
                                file.getFileName() + " must not import Forge: " + trimmed);
                    }
                }
            }
        }
    }

    // NOTE: the EtherClientState display-only check (no net.minecraft.client
    // imports) is restored with S2, when the client holder lands.
}
