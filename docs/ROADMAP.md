# NEXUS: Echoes of Reality — Technical Roadmap

> A feature is **done** only when: code + gameplay + integration + multiplayer +
> performance + UX + tests + documentation are acceptable for its phase.
> "It compiled" is not validation.

## Phase 1 — CORE ✅ (technically complete 2026-10-01)

**Goal:** a functional, extensible nucleus.

- Forge 1.20.1 project, modular packages, single-point registries
- Config (`ForgeConfigSpec`), versioned networking, server-authoritative sync
- Energy architecture: `INexusEnergy` + `EnergyType` + `NexusEnergyStorage`
- First resource: Nexus Shard (ore drop) → Resonant Crystal (processed)
- First block: Nexus Ore (overworld)
- First machine: Resonator (energy → progress → output, GUI with energy bar)
- Creative energy cell (test producer; real generators are Phase 2)
- Data-driven resonator recipes (JSON)
- Unit tests for energy semantics

**Standing item — POST-PHASE-1 RUNTIME VALIDATION:** native Forge client/server
runtime was not homologated in the sandbox; a real client + dedicated server load test
is still required. This does not block Phase 2.

## Phase 2 — KINETIC ENERGY ✅ (implemented 2026-10-01)

**Goal:** prove the energy system with a real physics-feeling network.

**Implemented 2026-10-01 (this repo, no Create dependency, no `EnergyType.KINETIC` —
kinetic is a separate simulation per ADR-007, not a variant of `INexusEnergy`):**

- Pure-Java kinetic core: `Rpm`, `Torque`, `Power` domain types, `ω = RPM·2π/60`, `P = τ·ω`
- `SimNetwork`: demand referred to source, proportional brownout, deterministic source conflicts
- `KineticManager` per `ServerLevel`: dirty-topology cache, snapshots, 100-tick safety refresh, server-authoritative
- Kinetic Generator (120 RPM / 50 Nm, redstone-disabled, display-only GUI)
- Transmission: Shaft, Gear (12t, flips direction), Gearbox (0.5/1/2/4, right-click cycles), Clutch (engage/disengage)
- Resonator migrated to kinetic consumer (120 RPM / 30 Nm required)
- Vanilla BE update packets + `ContainerData` sync; NBT persistence; `/nexus kinetic` diagnostics
- 50/50 tests green; Forge sources compile green (71 classes); JAR rebuilt

**Not yet done (carried forward):** hand crank / windmill / waterwheel sources, belts,
stress-break behavior, rotating-part visualization beyond GUI readouts, datagen
providers, 20-shaft TPS benchmark, real client/server homologation
(POST-PHASE-1 RUNTIME VALIDATION still applies).

## Phase 3 — INDUSTRIAL PROCESSING ✅ (implemented 2026-10-01)

**Goal:** prove the loop `RESOURCE → PROCESSING → COMPONENT → TECHNOLOGY`
on the kinetic network, with no third energy infrastructure.

**Implemented 2026-10-01:**

- `AbstractKineticMachineBlockEntity`: shared kinetic-machine base (registration,
  snapshots, NBT, inventory, recipe cache, integer + fractional progress,
  server-side processing, proportional brownout, 6-index `ContainerData`:
  rpm / torque mN·m / node status / progress / maxProgress / machine status)
- `ProcessingGovernor` (pure): brownout speed factor =
  `min(deliveredRPM/requiredRPM, deliveredTorque/requiredTorque)`; machine
  states `RUNNING / IDLE / NO_POWER / BROWNOUT / BLOCKED`
- Crusher (120 RPM / 20 N·m, `crushing` recipes) and Processor
  (240 RPM / 15 N·m, `processing` recipes) — numbers chosen so the Processor
  genuinely needs a 2:1 gearbox off the 120 RPM / 50 N·m generator
- Resonator refactored onto the new base (legacy `resonating` type + 5-index
  GUI data untouched); its Phase 2 configured energy storage restored
  verbatim — Phase 1 energy API contract unchanged
- `ProcessingRecipe` API: input/output/processingTime/optional byproduct+chance;
  fail-fast validation (`processingTime > 0`, chance in `[0,1]`); separate
  `crushing`/`processing` types + serializers
- Chain content: `nexus_ore → Crusher → 2 nexus_dust (+30% cobblestone) →
  Processor → refined_nexus → (crafting) nexus_plate → nexus_component`;
  crafting recipes for both machines (Crusher needs no processed Nexus —
  no circular dependency)
- Data-driven worldgen: configured + placed features + biome modifier
  (`forge/biome_modifier/`, vein 7, count 7, -32..48, overworld)
- Per-face sprites (`orientable`: front/side/top) + furnace-style `facing`
  via shared `AbstractOrientedMachineBlock`; GUIs show input/output/progress/
  RPM/torque/power/status; `/nexus kinetic` shows consumer demand
  (`req=<rpm>rpm/<torque>Nm`)
- Automation: input accepts insertion, output/byproduct are extract-only
  (menu + capability), extraction allowed from all slots
- 78/78 tests green (governor, kinetic chain incl.
  Generator → Shaft → Gearbox → Crusher → Processor, NBT round-trips, recipe
  validation); `javac` clean (93 classes); JAR reassembled

**Not yet done (carried forward):** JEI/REI, datagen providers, crate, fluids,
`active` blockstate (GUI shows status instead), recipe-ID persistence across
reload, recipe-matching GameTests, real client/server homologation
(POST-PHASE-1 RUNTIME VALIDATION still applies).

## Phase 4 — RESEARCH & TECHNOLOGICAL PROGRESSION ✅ (implemented 2026-10-01)

**Goal:** turn research into the player's path to technology:
`PLAYER → RESEARCH → TECHNOLOGY → UNLOCK` — without dimensions, portals,
new energy, JEI/REI or a quest system.

**Implemented 2026-10-01 (ADR-010):**

- Pure domain: `ResearchDefinition` (namespaced id, title, description,
  category, cost > 0, prerequisites, unlocks); `ResearchGraph` validates
  duplicates, unknown prerequisites, self-dependencies and cycles
  (DFS), with deterministic topological order
- `PlayerResearchState`: integer points, completed set, once-only source
  claims; NBT round-trip (missing fields default, negative points clamp,
  malformed IDs skipped)
- `ResearchService`: the single authority — source grants, `canComplete`,
  atomic `completeResearch` (failed validation spends nothing), duplicate
  prevention, `isUnlocked`, `available()`
- Sources: `discover_nexus_ore` (+10 once), `mine_nexus_ore` (+2 repeatable),
  `craft_gear` (+10 once)
- Technologies: `nexus_echoes:crusher`, `:processor`, `:nexus_component`,
  `:hollow_access` (future hook only — no dimension/portal/worldgen code)
- Persistence: `ResearchSavedData` (`nexus_echoes_research`) in the
  Overworld `DimensionDataStorage`, UUID-keyed — survives
  death/clone/dimension/log-out/restart; `setDirty()` on mutation
- Networking: `ResearchSyncPacket` (S2C snapshot on login + every
  mutation), `OpenResearchPacket` (C2S snapshot request),
  `BuyResearchPacket` (C2S, fully revalidated server-side); client keeps a
  read-only snapshot (`ResearchClientState`) — no client authority
- GUI: research screen on keybind **R** (points, research list with
  COMPLETE / RESEARCH / NEED PTS / LOCKED, tooltips, buy button,
  codex button), codex screen (only visible entries, back button)
- Gating (all server-side, `ResearchGating` pure rules, machine BEs
  untouched): locked machine craft voids the stack with an explanation
  (`ItemCraftedEvent` fires post-hoc from `ResultSlot`); locked placement
  cancelled (`BlockEvent.EntityPlaceEvent`); `use()` on locked Crusher /
  Processor returns `InteractionResult.FAIL`
- `/nexus research` command: status, list, add / complete / reset
  (permission level 2)
- Data: 4 research JSONs + 6 codex JSONs under `data/nexus_echoes/`,
  fail-fast `SimpleJsonResourceReloadListener` validation; lang entries
- 174/174 tests green (78 previous + 96 new); `javac` clean (122 classes);
  JAR reassembled

**Known limitations:** recipe visibility is not hidden (vanilla has no
per-player recipe filter — locked crafts void with an explanation);
POST-PHASE-1 RUNTIME VALIDATION still applies (no real Forge
client/server executed in this sandbox).

## Phase 5 — THE HOLLOW ✅ (implemented 2026-10-01, ADR-011)

Proved NEXUS is simultaneously a tech mod and an exploration mod:

- Dimension: `the_hollow` with 4 biomes, resources, features-as-landmarks, 4 entities
- Travel is technological, not a portal: dimensional spire (kinetic consumer) as entry,
  obelisk as return — no second energy system was created (deliberate deviation from the
  early "Hollow-native energy" sketch; research stays the only progression authority)
- Anomalies v1 (unstable regions, machine interference via transient kinetic derate)
- Codex v1 with discovery gating (`requiredDiscovery`); first lore thread: *"someone was
  here before"* (ruins, vaults, memory fragments)

**Exit criteria met:** a player can progress Overworld → Hollow using only in-game
discoveries (research `hollow_access` → craft spire → charge with kinetic power → cross →
discover → return via obelisk).

**Honestly deferred:** visual verification of terrain/fog/landmarks/entities,
Jigsaw structures (features are landmarks, not `Structure`/`StructureSet`), custom
`noise_settings`, custom sounds. POST-PHASE-1 RUNTIME VALIDATION still applies (no real
Forge client/server executed in this sandbox).

## Phase 5.5 — DIMENSIONAL ARCHITECTURE AUDIT ✅ (2026-10-01, ADR-012)

Formal read-only-first audit of everything Phase 5 introduced — the
Hollow's dimensional architecture is the foundation The Ether will multiply,
so it had to be proven sound before Phase 6.

- Every dimensional class traced and classified (pure domain vs. MC adapter vs.
  client); bootstrap/registry ordering, dimension-type codec fields (incl.
  nested `monster_settings`), worldgen JSON chains, and all 148 JSONs verified
- Fixed 4 genuine low-risk defects with regression tests: stale kinetic
  derates after anomaly expiry, transient per-player cooldown leak (logout
  cleanup), unguarded chunk access in the proximity discovery scan, 3 missing
  lang keys (Phase 2 debt) — no Phase 1–4 behavior changed
- Confirmed: research is the single progression authority; multiplayer
  isolation holds by UUID-keyed SavedData; client/server boundary clean
  (exhaustive import trace); no generic dimension framework built yet
  (deliberate — the Ether will reveal the real abstractions)
- **Gate: READY WITH CONDITIONS** — native Forge runtime validation still
  pending on real hardware; full report: `docs/PHASE_5_5_AUDIT.md`

## Phase 6 — THE ETHER (directional)

Second dimension: energetic/unstable reality, advanced resources, new physics phenomena,
evidence that Hollow tech is structural to reality itself.

**Phase 6 must respect the Phase 5.5 constraints:** research stays the single
progression authority; pure domain first, thin adapters; no generic dimension
framework yet; per-UUID state in server-wide SavedData; transient effects with
documented owners and clearing paths; loaded-chunk guards on world scans.
(see `docs/PHASE_5_5_AUDIT.md`)

## Phase 7 — ROBOTICS & AI (directional)

Modular robots (tasks, inventory, energy, navigation, machine interaction);
AI architecture (states, goals, priorities, perception) — some entities feel
almost conscious. Feeds the lore.

## Phase 8 — THE CORE / THE NEXUS (directional)

Final layer: extreme technology, final energy, control systems, large-scale anomalies,
answers about the Nexus, preparation for the final confrontation.

## Phase 9 — ENDGAME: THE ARCHITECT (directional)

Boss as culmination: arena, phases, environment manipulation, Nexus systems as
mechanics, narrative integration. Not "a mob with lots of HP".

---

## Cross-cutting (every phase)

- Multiplayer correctness review before phase sign-off
- Performance profiling of new ticking systems
- Config coverage for new tunables
- Docs update (architecture deltas → DECISIONS.md)
