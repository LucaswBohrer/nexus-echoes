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
