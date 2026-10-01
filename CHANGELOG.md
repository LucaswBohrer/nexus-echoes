# Changelog

All notable changes to **NEXUS: Echoes of Reality** (`nexus_echoes`) are recorded here.
Format follows *Keep a Changelog* loosely; versions are mod versions.

## [0.1.0] — 2026-10-01

### Phase 1 — CORE

- Forge 1.20.1 project, modular package structure
- Registries (blocks, items, block entities, menus, recipe types), `ForgeConfigSpec`
- `SimpleChannel` networking (server-authoritative sync)
- `INexusEnergy` API + int-based storage; Nexus Ore / Shard / Resonant Crystal
- Resonator (energy consumer with GUI: energy bar + progress)
- Creative energy cell (test producer), data-driven `resonating` recipes
- 11 unit tests

### Phase 2 — KINETIC ENERGY

- Pure-Java kinetic core: `Rpm` / `Torque` / `Power`, `KineticMath` (ADR-007)
- `SimNetwork`: topology → simulation, demand referred to source, proportional
  brownout, deterministic source conflicts
- `KineticManager` per `ServerLevel`: dirty-topology caching, server-authoritative
- Generator (120 RPM / 50 N·m), Shaft, Gear (12t), Gearbox (0.5/1/2/4),
  Clutch; Resonator migrated to kinetic consumer (120 RPM / 30 N·m)
- Vanilla BE update packets + `ContainerData` sync; NBT persistence
- `/nexus kinetic` diagnostics
- 39 new tests (50/50 green); 71 classes compiled

### Phase 3 — INDUSTRIAL PROCESSING

- `AbstractKineticMachineBlockEntity` (inventory, recipe cache, progress +
  fractional accumulator, brownout, 6-index `ContainerData`)
- `ProcessingGovernor` (pure): `RUNNING / IDLE / NO_POWER / BROWNOUT / BLOCKED`
- Crusher (120 RPM / 20 N·m) + Processor (240 RPM / 15 N·m); chain
  `nexus_ore → Crusher → 2 nexus_dust (+30% cobblestone) → Processor →
  refined_nexus → nexus_plate → nexus_component`
- Data-driven `crushing` / `processing` recipes; data-driven ore worldgen
  (vein 7, count 7, -32..48)
- Per-face sprites, machine GUIs, input/output automation, `/nexus kinetic`
  consumer demand
- 28 new tests (78/78 green); 93 classes compiled

### Phase 4 — RESEARCH & TECHNOLOGICAL PROGRESSION

- Research as a separate domain: definitions, dependency-graph validation
  (duplicates / unknown prerequisites / self-dependencies / cycles rejected),
  per-player state (points, completions, once-only sources), centralized
  `ResearchService`, pure gating rules, data-driven codex
- UUID-keyed `SavedData` persistence (survives death/clone/dimension/logout/restart)
- Server-authoritative multiplayer: per-player state, 3 packets
  (`ResearchSyncPacket` S2C, `OpenResearchPacket` + `BuyResearchPacket` C2S),
  all mutations revalidated server-side
- Research screen (keybind **R**) + codex screen; `/nexus research` command
- 4 research definitions (50 → 100 → 150 → 250) gating Crusher / Processor /
  component / `hollow_access` (future hook — no dimension code); 6 codex entries
- Locked machines refuse craft (stack voided with explanation), placement and
  GUI use — machine block entities untouched
- 96 new tests (174/174 green); 122 classes compiled

### Phase 5 — THE HOLLOW

- New `dimension/` module: pure domain (`api`, `travel`, `anomaly`, `discovery`)
  + thin MC adapters (travel, anomaly manager, SavedData, discovery sync,
  travel-data, command, forge events)
- The Hollow dimension (`the_hollow`): dimension type + 4 biomes
  (`hollow_wastes`, `machine_graveyard`, `resonant_forest`, `deep_hollow`);
  reuses vanilla overworld noise settings
- Landmarks as deterministic custom features (not Jigsaw structures): obelisk
  (rarity 1/60), ruin, vault (loot chest), fracture spire; ore/growth patches
- Travel: Dimensional Spire (kinetic consumer, 240 RPM / 25 N·m, 60 s charge,
  charge drains on crossing) is the only entry; obelisk is the only return
  (per-player origin links in `PlayerTravelData`); server-authoritative
  `ITeleporter` + pure safe-spawn search; gated on `hollow_access`
- Anomalies: 4 types (static field, gravity ripple, temporal echo, fracture
  surge); server-owned, 20-tick sweeps, cap 8, vanilla-particle presentation;
  transient kinetic derate in `KineticManager` (pure simulator untouched);
  Anomaly Ward (24-block suppression)
- Content: 9 blocks, memory fragment + resonance scanner items, 4 entities with
  models/renderers (scrap crawler, hollow stalker, resonant wisp, rift phantom),
  procedural 16×16 placeholder textures (no entity textures yet)
- Progression: research branch `dimensional_resonance` → `hollow_exploration`
  (200) → `anomaly_studies` (250); 5 discovery-gated codex entries
  (additive `requiredDiscovery`); per-UUID discovery isolation
- New tests: travel rules, safe-spawn, anomaly lifecycle, discovery gating,
  codex discovery gate, spire charge/brownout/decay, worldgen JSON integrity
  (174-test baseline preserved)

### Phase 5.5 — DIMENSIONAL ARCHITECTURE & RUNTIME READINESS AUDIT

- Formal audit of everything Phase 5 introduced (ADR-012,
  `docs/PHASE_5_5_AUDIT.md`): dimensional layering, bootstrap/registry,
  dimension-type codec, worldgen data chains, anomaly/kinetic/travel
  lifecycles, research authority, multiplayer isolation, save/reload,
  client/server boundary (exhaustive import trace), networking, all 148 JSONs
- 4 genuine low-risk defects fixed with regression tests (10 new tests):
  stale kinetic derates when a node was removed mid-anomaly; transient
  per-player cooldown maps never evicted on logout (new pure
  `PlayerCooldowns`); proximity discovery scan could force chunk generation
  (`hasChunkAt` guard); 3 missing lang keys (Phase 2 debt)
- No Phase 1–4 behavior changed; no content added; no tests weakened
- Gate: **READY WITH CONDITIONS** (native Forge runtime validation still
  pending on real hardware; Phase 6 must follow the audit's architectural
  constraints)

### Known limitations (standing)

- **POST-PHASE-1 RUNTIME VALIDATION**: no real Forge client/server has been
  executed in this environment; registration, GUIs, sync and the kinetic loop
  still need in-game homologation.
- Locked-recipe *visibility* is not hidden (vanilla has no per-player recipe
  filter); locked crafts void the result with an explanation instead.
