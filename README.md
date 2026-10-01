# NEXUS: Echoes of Reality

> *The player should feel they are discovering something that existed before them.*

**NEXUS: Echoes of Reality** is an ambitious Minecraft mod (Forge 1.20.1) that plays like a
complete game inside Minecraft: exploration, technology, automation, energy, physics,
dimensions, investigation, lore, anomalies, AI, robotics, structures, progression, bosses,
multiplayer, and integration with other mods.

This repository is the engineering foundation. We are **not** trying to implement the whole
mod at once — we are building the foundation of a large, complex, extensible, technological,
dimensional and narrative mod, with enough engineering quality to sustain years of growth.

## Current status

**Phase 1 — CORE** ✅ (technically complete; native Forge client/server runtime not yet homologated — see **POST-PHASE-1 RUNTIME VALIDATION** below).

- [x] Forge 1.20.1 project, modular package structure
- [x] Registries (blocks, items, block entities, menus, recipe types)
- [x] Config (ForgeConfigSpec)
- [x] Networking (SimpleChannel, server-authoritative sync)
- [x] Energy architecture (`INexusEnergy` API + storage implementation)
- [x] First resource: Nexus Shard / Resonant Crystal
- [x] First block: Nexus Ore
- [x] First machine: Resonator (energy-consuming processor with GUI)
- [x] First GUI: Resonator screen (energy bar + progress)
- [x] Creative energy cell (test energy source; real generators arrive in Phase 2)
- [x] Data-driven resonator recipes (JSON)
- [x] Unit tests (energy storage logic)
- [ ] Full build validation (`./gradlew build`)

**Phase 2 — KINETIC ENERGY** ✅ (implemented 2026-10-01).

- [x] Pure-Java kinetic core: `Rpm`, `Torque`, `Power` domain types, `KineticMath` (ADR-007)
- [x] `SimNetwork`: topology → simulation with demand referred to source, proportional brownout, source conflicts
- [x] Per-`ServerLevel` `KineticManager`: dirty-topology caching, snapshots, server-authoritative, client never simulates
- [x] Kinetic Generator (source: 120 RPM / 50 Nm, redstone-disabled, display-only GUI)
- [x] Transmission: Shaft, Gear (12t, flips direction), Gearbox (0.5/1/2/4 ratios, right-click cycles), Clutch (engage/disengage)
- [x] Resonator migrated to a kinetic consumer (120 RPM / 30 Nm); no parallel energy system
- [x] Sync: vanilla BE update packets + `ContainerData`; NBT persistence of kinetic state
- [x] `/nexus kinetic` diagnostic command
- [x] 50/50 unit tests green (11 Phase 1 + 39 kinetic); Forge sources compile green (71 classes)
- [x] JAR `nexus_echoes-0.1.0.jar` rebuilt with Phase 2 content

**Phase 3 — INDUSTRIAL PROCESSING** ✅ (implemented 2026-10-01, ADR-009).

- [x] `AbstractKineticMachineBlockEntity`: shared kinetic-machine base (registration, snapshots, NBT, inventory, recipe cache, progress + fractional accumulator, proportional brownout, 6-index `ContainerData`)
- [x] `ProcessingGovernor` (pure): brownout speed = `min(rpmRatio, torqueRatio)`; states `RUNNING / IDLE / NO_POWER / BROWNOUT / BLOCKED`
- [x] Crusher (120 RPM / 20 N·m) and Processor (240 RPM / 15 N·m) — the Processor genuinely needs a 2:1 gearbox off the 120 RPM / 50 N·m generator
- [x] Resonator refactored onto the new base; its Phase 2 configured energy storage restored verbatim (Phase 1 API contract unchanged)
- [x] `ProcessingRecipe` API (`crushing` / `processing` types): input/output/processingTime/optional byproduct+chance, fail-fast validation
- [x] Chain: `nexus_ore → Crusher → 2 nexus_dust (+30% cobblestone) → Processor → refined_nexus → nexus_plate → nexus_component`; machine crafting recipes (no circular dependency)
- [x] Data-driven worldgen: configured + placed features + biome modifier (`forge/biome_modifier/`, vein 7, count 7, -32..48)
- [x] Per-face sprites (`orientable`: front/side/top) + player `facing`; GUIs show input/output/progress/RPM/torque/power/status; `/nexus kinetic` shows consumer demand
- [x] Automation: input accepts insertion, output/byproduct extract-only (menu + capability)
- [x] 78/78 unit tests green; Forge sources compile green (93 classes); JAR rebuilt

**Phase 4 — RESEARCH & TECHNOLOGICAL PROGRESSION** ✅ (implemented 2026-10-01, ADR-010).

- [x] Pure research domain: `ResearchDefinition`/`ResearchGraph` (duplicate/unknown/self/cycle
  rejection, deterministic topological order), `PlayerResearchState` (points/completed/claimed
  sources, NBT round-trip with clamping + malformed-ID skipping), `ResearchService` (single
  authority: sources, eligibility, atomic completion, unlock queries), `ResearchGating` (pure
  craft/place/use rules)
- [x] Persistence: `ResearchSavedData` in the Overworld `DimensionDataStorage`, keyed by player
  UUID — survives death/clone/dimension/log-out/restart
- [x] Server authority + multiplayer: per-player state, no global static state, no client
  authority; three packets (`ResearchSyncPacket` S2C, `OpenResearchPacket` + `BuyResearchPacket`
  C2S), all mutations revalidated server-side
- [x] Data-driven: 4 research definitions (`industrial_foundations` 50 → `kinetic_transmission`
  100 → `advanced_processing` 150 → `dimensional_resonance` 250) + 6 codex entries with
  `requiredResearch` visibility, all under `data/nexus_echoes/` with fail-fast reload validation
- [x] GUI: research screen (keybind **R**, points, list with COMPLETE/RESEARCH/NEED PTS/LOCKED,
  tooltips, buy via C2S) + codex screen (visible entries only); locked Crusher/Processor refuse
  GUI/craft/placement with an explanation
- [x] `/nexus research` command (status, list, add/complete/reset with permission level 2)
- [x] 174/174 unit tests green (78 previous + 96 research); Forge sources compile green
  (122 classes); JAR rebuilt

**Phase 5 — THE HOLLOW** ✅ (implemented 2026-10-01, ADR-011).

- [x] Dimension module (`dimension/`): pure domain (`api`, `travel`, `anomaly`, `discovery`) +
  thin MC adapters (`HollowTravel`, `AnomalyManager`, `AnomalySavedData`,
  `HollowDiscoveryData`, `PlayerTravelData`, `HollowCommand`, `HollowForgeEvents`)
- [x] The Hollow: custom dimension (`the_hollow`), 4 biomes (`hollow_wastes`,
  `machine_graveyard`, `resonant_forest`, `deep_hollow`), reuses vanilla overworld noise
  settings; 4 custom features (`obelisk`, `ruin`, `vault`, `fracture_spire`) + ore/growth
  patches, all data-driven configured/placed JSON
- [x] Travel: **Dimensional Spire** (kinetic consumer 240 RPM/25 N·m, 60 s charge, drains on
  crossing) as the only entry; **obelisk** as the only return (per-player origin links);
  server-authoritative via `ITeleporter` + pure safe-spawn search; gated on `hollow_access`
- [x] Anomalies: 4 types (static field, gravity ripple, temporal echo, fracture surge),
  server-owned, 20-tick sweeps, cap 8, vanilla-particle presentation, transient kinetic
  derate that never touches the pure simulator; **Anomaly Ward** (24-block suppression)
- [x] Content: 9 blocks, `memory_fragment` + `resonance_scanner` items, 4 entities
  (scrap crawler, hollow stalker, resonant wisp, rift phantom) with models/renderers,
  procedural 16×16 placeholder textures
- [x] Progression: research branch `dimensional_resonance` → `hollow_exploration` (200) →
  `anomaly_studies` (250); 5 discovery-gated codex entries (`requiredDiscovery` is
  additive); discoveries/research per-UUID — shared dimension, private progression
- [x] Unit tests green (225/225: 215 baseline preserved + 10 new audit regression
  tests for derate clearing and transient cooldown cleanup); Forge sources compile green
  (131 classes, via javac against the ForgeGradle-prepared `forge-official.jar` — the
  Gradle daemon could not start in this sandbox); JAR rebuilt

> **Phase 5.5 — Dimensional Architecture & Runtime Readiness Audit** (2026-10-01,
> ADR-012): every dimensional class traced and classified; 4 genuine low-risk defects
> fixed with regression tests (stale kinetic derates, transient cooldown leak on logout,
> unguarded chunk access in discovery scan, 3 missing lang keys); zero blockers;
> gate **READY WITH CONDITIONS** (native runtime validation pending on real hardware).
> Full report: `docs/PHASE_5_5_AUDIT.md`.

> Visual acceptance of the Hollow (terrain, fog, structures, entities, textures) is
> explicitly pending Lucas's in-game review — see POST-PHASE-1 RUNTIME VALIDATION below.

### POST-PHASE-1 RUNTIME VALIDATION

Native Forge client/server runtime was **not** homologated in this sandbox (no real
Minecraft client/server executed here). This is a standing validation item: the mod must
still be loaded in a real Forge 1.20.1 client and on a dedicated server / multiplayer
session to confirm registration, GUIs, sync and the kinetic loop end-to-end.

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — module map, contracts, energy model, networking, multiplayer rules
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — phased plan (Phase 1 → The Architect)
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — architectural decision records (ADRs)

## Build

Requires JDK 17.

```bash
./gradlew build        # compile + tests + jar
./gradlew runClient    # launch test client
./gradlew runServer    # launch test server
```

## Project principles

1. **Modularity** — `core/`, `content/`, `energy/`, `machines/`, `dimensions/`, `worldgen/`,
   `entities/`, `ai/`, `network/`, `research/`, `codex/`, `integration/`, `client/`,
   `rendering/`, `config/`, `data/` grow independently behind interfaces.
2. **Multiplayer-first** — server authority, explicit sync packets, no client logic in simulation.
3. **Performance** — no wasteful ticks; caching, dirty flags, periodic updates.
4. **Data-driven** — JSON/datapacks over hardcode wherever content is involved.
5. **Standalone, integration-optional** — other mods are detected dynamically, never hard dependencies.
6. **A feature is done** only when code + gameplay + integration + multiplayer + performance +
   UX + tests + documentation are acceptable for its phase.

## License

All Rights Reserved (placeholder — to be decided before any public release).
