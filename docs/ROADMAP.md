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

## Phase 3 — TECHNOLOGY

**Goal:** ~7 functional machines demonstrating the machine framework.

- Processing, storage, energy, automation, real recipes, GUIs, progression gating
- JEI/REI integration, first automation (hoppers/pipes interact correctly)
- Machine upgrade modules (speed/efficiency)

**Exit criteria:** a fully automated line (ore → processed → stored) runs unattended;
JEI shows all recipes.

## Phase 4 — THE HOLLOW

**Goal:** prove NEXUS is simultaneously a tech mod and an exploration mod.

- Dimension: worldgen, 4+ biomes, resources, structures/ruins, mobs
- Hollow-native energy type + portal (technological: discovery → research → resources →
  machine → energy → stabilization → portal)
- Anomalies v1 (unstable regions, machine interference, rare events)
- Codex v1 (progressive discovery log: techs, resources, dimensions, lore fragments)
- First lore thread: *"someone was here before"*

**Exit criteria:** a player can progress Overworld → Hollow using only in-game
discoveries; the dimension feels like a different reality, not a reskin.

## Phase 5 — THE ETHER (directional)

Second dimension: energetic/unstable reality, advanced resources, new physics phenomena,
evidence that Hollow tech is structural to reality itself.

## Phase 6 — ROBOTICS & AI (directional)

Modular robots (tasks, inventory, energy, navigation, machine interaction);
AI architecture (states, goals, priorities, perception) — some entities feel
almost conscious. Feeds the lore.

## Phase 7 — THE CORE / THE NEXUS (directional)

Final layer: extreme technology, final energy, control systems, large-scale anomalies,
answers about the Nexus, preparation for the final confrontation.

## Phase 8 — ENDGAME: THE ARCHITECT (directional)

Boss as culmination: arena, phases, environment manipulation, Nexus systems as
mechanics, narrative integration. Not "a mob with lots of HP".

---

## Cross-cutting (every phase)

- Multiplayer correctness review before phase sign-off
- Performance profiling of new ticking systems
- Config coverage for new tunables
- Docs update (architecture deltas → DECISIONS.md)
