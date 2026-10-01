# Phase 5.5 — Dimensional Architecture & Runtime Readiness Audit

**Date:** 2026-10-01
**Repository:** `LucaswBohrer/nexus-echoes`, branch `main`
**Baseline audited:** `05898be` (Phase 5 — The Hollow)
**Minecraft / Forge:** 1.20.1 / 47.2.0 · Mod ID `nexus_echoes`
**Test baseline at audit start:** 215/215 passing
**Test result after audit fixes:** 225/225 passing (215 + 10 new regression tests)

This is a formal architectural and integration audit, not a content phase.
No dimensions, machines, entities, research, anomalies, progression, lore,
or gameplay mechanics were added.

---

## Executive Summary

The dimensional architecture introduced by Phase 5 is **sound and genuinely
layered**: pure domain rules (travel, anomalies, discovery) are separated from
Minecraft/Forge adapters, persistence is correctly placed, research remains the
single progression authority, and the client/server boundary is clean (verified
by exhaustive import tracing — zero accidental client dependencies reachable
from a dedicated server).

The audit found **zero blockers**. It found and fixed **four low-risk defects**,
all local, all with regression tests:

1. **Stale kinetic derates** — an expired static-field anomaly could leave
   permanent derate entries for positions whose machine was removed
   mid-anomaly (`AnomalyManager.clearAnomalyDerates` skipped positions with
   no provider). Fixed; derates are now cleared for every uncovered position.
2. **Transient UUID map leak** — the static `AnomalyManager` singleton kept
   per-player cooldown entries forever, with no eviction on logout. Fixed via
   a new pure `PlayerCooldowns` class + `PlayerLoggedOutEvent` cleanup.
3. **Chunk-generation side effect** — the 200-tick proximity discovery scan
   called `getBlockState` on 35,937 positions with no loaded-chunk guard,
   which can force chunk generation at the edge of the loaded area. Fixed with
   a `hasChunkAt` guard.
4. **Missing lang keys** — `clutch_engaged`, `clutch_disengaged`,
   `gearbox_ratio` (pre-existing Phase 2 debt) displayed as raw keys in chat.
   Added to `en_us` + `pt_br`.

One layering smell was documented but intentionally left unchanged
(`dimension/api/HollowDimensions` couples the `api` package to MC types —
works correctly, acceptable as-is).

**Phase 6 gate: READY WITH CONDITIONS** (see gate section). The single standing
condition is environmental, not architectural: native Forge runtime validation
remains impossible in this sandbox and must be executed on real hardware.

---

## Repository State

- `git status` clean at `05898be` before the audit; all changes below are the
  audit's own fixes + this report.
- `src/main/java`: 131 files after audit (+1: `PlayerCooldowns`).
- `src/test/java`: 25 files after audit (+2 regression test classes).
- 148 datapack/asset JSONs — all parse; cross-references verified (see
  Resources/Data).
- Docs inspected: `ARCHITECTURE.md`, `DECISIONS.md` (ADR-001…ADR-011),
  `ROADMAP.md`, `README.md`, `CHANGELOG.md`, plus `build.gradle`,
  `settings.gradle`, `gradle.properties`, `gradle/`.

---

## Phase 1–5 Compatibility

No Phase 1–4 behavior was changed. The four fixes touch only:

- `dimension/mc/AnomalyManager` (derate clearing, cooldown storage),
- `dimension/mc/HollowForgeEvents` (+logout handler, +chunk guard),
- `dimension/mc/PlayerCooldowns` (new, pure),
- `assets/nexus_echoes/lang/{en_us,pt_br}.json` (+3 keys each).

The kinetic simulator, research service/graph, networking, registries, and all
Phase 1–4 content are untouched. Full suite: 225/225, no existing test
modified.

---

## Dimensional Architecture

Every dimensional class traced and classified:

### PURE DOMAIN (no Minecraft dependencies)

| Class | Notes |
|---|---|
| `dimension/travel/HollowTravelService` | travel permission = research query; safe-spawn = spiral over injected `HeightQuery`. Fully unit-tested. |
| `dimension/anomaly/AnomalyService` | spawn selection under cap 8, expiry partition, instance creation. Fully unit-tested. |
| `dimension/anomaly/AnomalyEffect` | enum, zero imports. |
| `dimension/anomaly/AnomalyInstance` | record; MC `ResourceLocation` used as a value type only. |
| `dimension/anomaly/AnomalyDefinition` | record; `ResourceLocation` as value type. |
| `dimension/discovery/DiscoveryIds` | `ResourceLocation` constants. |
| `SpireBlockEntity.nextCharge` | pure static charge rule inside the adapter — good pattern, unit-tested. |

The use of `ResourceLocation` inside pure domain classes is pragmatic, not a
violation: it is a stable value class with no world/registry access.

### MINECRAFT ADAPTERS (`dimension/mc/`)

`HollowTravel`, `AnomalyManager`, `AnomalySavedData`, `HollowDiscoveryData`,
`PlayerTravelData`, `DiscoverySync`, `HollowForgeEvents`, `HollowCommand` —
all correctly shaped as thin adapters over the pure domain. `HollowTravel`
delegates permission to `HollowTravelService.canTravel` and safe-spawn to
`findSafeSpawn`; `AnomalyManager` delegates spawn/expiry to `AnomalyService`.

### LAYER SMELL (documented, unchanged)

- `dimension/api/HollowDimensions` lives in `api/` but imports
  `net.minecraft.world.level.Level` and `Registries` to pre-build
  `ResourceKey`s. It works correctly and is heavily referenced; rebuilding it
  as pure string constants would ripple through adapters and tests for no
  behavioral gain. **RISK-5, left unchanged.** For Phase 6, prefer plain
  identifiers in any new `api` package and let adapters build keys.

### CLIENT / DATA / INFRASTRUCTURE

- Client: `dimension/client/` (`DiscoveryClientState`, `HollowClientSetup`,
  `render/*`) — all behind `Dist.CLIENT`, verified (see Client/Server
  Boundaries).
- Data/content: `dimension/block/*`, `item/*`, `entity/*`, `feature/*` —
  normal Forge content classes.
- Network: `dimension/network/DiscoverySyncPacket` — S2C only.

**Verdict:** the pure/adapter split is real and reusable. Future dimensions
can reuse `AnomalyService`-style pure rules and the SavedData-per-UUID
patterns without copying Hollow internals.

---

## Dimension Bootstrap

- `ENTITY_TYPES`, `FEATURES`, `BLOCKS`, `ITEMS`, `BLOCK_ENTITIES` via
  `DeferredRegister` on the correct Forge registries, registered to the mod
  bus from `NexusEchoes` construction. Correct registry, correct timing.
- No static-initialization hazards: registry objects are `RegistryObject`
  suppliers; nothing dereferences them at class-load time.
- Datapack paths use the namespaces Forge 47.2.0 recognizes:
  `data/nexus_echoes/dimension/`, `dimension_type/`, `worldgen/*`,
  `data/nexus_echoes/forge/biome_modifier/nexus_ore.json` (correct namespaced
  path, valid `forge:add_features` content).
- `dimension/hollow.json` → `dimension_type/hollow.json` resolves;
  `effects: "nexus_echoes:the_hollow"` matches the client-registered
  `DimensionSpecialEffects`.
- `configured_feature` JSONs reference `"type": "nexus_echoes:<name>"`,
  which resolves through the registered `Feature<?>` entries — the correct
  1.20.1 pattern.
- All 7 placed features → configured features → biome feature lists resolve;
  the multi-noise `biome_source` lists exactly the 4 Hollow biomes.

**No bootstrap defects found.**

---

## Dimension Type

`dimension_type/hollow.json` audited field-by-field against the 1.20.1
`DimensionType` codec:

- `fixed_time: 18000`, `has_skylight: true`, `has_ceiling: false`,
  `ultrawarm: false`, `natural: true`, `coordinate_scale: 1.0`,
  `bed_works: true`, `respawn_anchor_works: false`, `min_y: -64`,
  `height: 384`, `logical_height: 256`,
  `infiniburn: "#minecraft:infiniburn_overworld"`,
  `effects: "nexus_echoes:the_hollow"`, `ambient_light: 0.35` — all valid.
- `monster_settings` correctly nested (not at root): `piglin_safe: false`,
  `has_raids: false`,
  `monster_spawn_light_level: {type: "minecraft:uniform", value: {min_inclusive: 0, max_inclusive: 7}}`,
  `monster_spawn_block_light_limit: 0` — matches the codec exactly.

**Correct. Not modified.**

---

## Worldgen

- 4 biomes, all data-driven; multi-noise parameters place them; the Hollow
  reuses `minecraft:overworld` noise settings (ADR-011 — custom noise
  explicitly deferred).
- 7 configured + 7 placed features; placement predicates are vanilla
  (`rarity_filter`, `in_square`, `heightmap`, `biome`) — no custom placement
  code, no hardcoded coordinates, no global state.
- Landmark rarities: obelisk 1/60, ruin/vault/fracture_spire per-biome.
- Ore features (`hollow_ore`, `nexus_ore`) use vanilla ore placement.

**Reusability:** the worldgen is almost entirely data-driven JSON + four
small `Feature<NoneFeatureConfiguration>` classes. A second dimension needs
new JSON + new feature classes; nothing Hollow-specific leaks into shared
worldgen code. **REUSABLE AS-IS** (conventions), no extraction needed.

---

## Landmarks

`ObeliskFeature`, `RuinFeature`, `VaultFeature`, `FractureSpireFeature`
audited:

- Deterministic shape from the placement `RandomSource` (seed-derived).
- Only replace air/liquid (obelisk core placed unconditionally at the shaft
  top — overwrites at most one block; acceptable for a landmark).
- No Jigsaw/NBT, no structure pieces — the `Feature<NoneFeatureConfiguration>`
  approach is proportionate to the roadmap.
- Vault loot via loot table (`vault` chest) — data-driven.
- Dimension isolation: features are only listed in Hollow biome JSONs.

**Verdict:** the current approach is acceptable; Jigsaw/NBT stays **DEFERRED**
per ADR-011. No defect found.

---

## Entities

Delegated audit (all four entities + registration + spawn + renderers):

- **Registration:** `DeferredRegister` on `ForgeRegistries.ENTITY_TYPES`,
  correct timing; builder settings sane (categories, sizes, tracking range 10).
- **Attributes:** all four via `EntityAttributeCreationEvent`; no hostile
  missing damage/follow-range.
- **Spawn rules:** `SpawnPlacementRegisterEvent` with `Operation.REPLACE`;
  correct predicates per mob type (`Mob`/`Monster` rules, wisp flying).
  Rift Phantom intentionally has no placement — spawned only by anomalies in
  the Hollow (`AnomalyManager.maybeSpawnPhantom` uses the Hollow level).
- **Dimension restriction:** natural spawns exist only in the 4 Hollow biome
  `spawners` lists; no biome modifiers inject them elsewhere; Rift Phantom
  overrides `canChangeDimensions()` → false.
- **AI:** standard goal kits; wisp blink server-guarded and bounded.
- **Client boundary:** entity classes contain zero `net.minecraft.client`
  imports; renderers/models referenced only from Dist.CLIENT setup.
- **Persistence/despawn:** vanilla semantics verified against
  `forge-official.jar` bytecode; phantom has NBT age + hard lifetime cap +
  discard-on-rift-expiry — no leak path.

**9 SAFE, 0 RISK, 0 BLOCKER, 1 DEFERRED** (entity textures not shipped —
visual only, no crash; no placeholders created per audit policy).

---

## Anomalies

Ownership and lifecycle (all ten audit questions answered):

1. **Owner:** `AnomalySavedData`, per Hollow `ServerLevel` DataStorage.
2. **Created by:** `AnomalyManager.trySpawnNear` (20-tick sweep, 6% roll,
   near a random player actually in the Hollow, cap 8 via `AnomalyService`).
3. **Removed by:** sweep expiry (`partition` → `remove` + derate clearing +
   phantom discard).
4. **Player logout:** anomaly state is world state — unaffected. Per-player
   cooldowns are now cleaned on logout (RISK-2 fix).
5. **Chunk unload:** anomalies are position data, not entities — unaffected.
6. **Dimension unload:** SavedData persists to disk; `gameTime`-based expiry
   stays consistent across restarts (level time is persisted).
7. **Server restart:** anomalies reload; derates are transient and gone —
   re-applied by the sweep within 20 ticks if the anomaly is still active.
8. **Duplicates:** instances carry random UUIDs; cap enforced at spawn
   selection; no duplicate path found.
9. **Stale references:** instances are value records; the manager holds no
   entity/world references.
10. **Cross-dimension effects:** the sweep runs only on the Hollow level;
    derates target `KineticManager.get(hollow)` (per-level `WeakHashMap`) —
    cannot leak to the Overworld.

---

## Kinetic Integration

`AnomalyManager → KineticManager.setDerate/clearDerate` audited:

- The pure kinetic simulator is untouched: derate is an adapter-layer output
  scaling (`setDerate` clamps to [0,1], triggers rebuild). Confirmed by code
  inspection.
- Derate state is server-authoritative and transient (never persisted).
- Multiple static fields compose: expiry clearing now consults all other
  active static fields (`isUncovered` seam) — overlapping fields keep their
  derates.
- **RISK-1 (fixed):** `clearAnomalyDerates` previously skipped positions with
  no registered provider, so a machine broken mid-anomaly left a permanent
  stale derate entry that a future machine at that position would inherit.
  Now every uncovered position is cleared regardless of provider presence.
  Regression test: `AnomalyDerateLifecycleTest` (6 tests).
- `KineticManager` instances live in a per-level `WeakHashMap` — no static
  leak across dimension unloads; `unregister` on node removal is the BE's
  responsibility (unchanged).

---

## Travel

`HollowTravel` + `HollowTravelService` + `PlayerTravelData` traced end to end:

- **Enter:** research gate (`hollow_access`) → spire charge (240 RPM / 25 N·m,
  60 s, brownout-proportional, pure `nextCharge`) → discharge only on
  successful `travelTo` → origin recorded **before** teleport (logout
  mid-transit keeps the return link) → safe spawn via pure spiral search →
  server-authoritative `changeDimension` with `FixedTeleporter` → first-entry
  discoveries granted idempotently → snapshot sync.
- **Return:** obelisk (Hollow-only block) → stored origin → bounded safe
  spot near the spire (16-block ring search) → deterministic fallback to the
  target dimension's shared spawn → `changeDimension`.
- **Death in the Hollow:** no special handling — vanilla respawn applies;
  the recorded origin survives (SavedData), so the player can return and
  re-enter. Deterministic, documented.
- **Logout/reconnect:** position persists via vanilla player data; origin
  persists via SavedData; discovery snapshot re-synced on login.
- **Edge cases:** origin in unloaded chunks (safe-spot search guards with
  `hasChunkAt`, falls back to shared spawn); destroyed spire (same fallback);
  invalid dimension key (malformed entries dropped on load); `travelTo` from
  inside the Hollow records a Hollow origin — degenerate but harmless
  (overwritten by the next real trip).

**No travel defects found.**

---

## Research Integration

Phase 4 remains the single progression authority — verified:

- Chain: `dimensional_resonance` (250) → unlocks `hollow_access` →
  `hollow_exploration` (200) → `anomaly_studies` (250).
- Every Hollow gate (`DimensionalSpireBlock.use` ×2,
  `HollowTravel.canAccess`, `HollowTravelService.canTravel`) resolves through
  `ResearchService.isUnlocked` — no alternate authority.
- Discoveries grant **research points** via once-only `ResearchSource`s
  (`enter_hollow`, `discover_obelisk`, `discover_ruin`, `discover_anomaly`)
  and a repeatable one (`analyze_memory_fragment`) — they never unlock
  technologies directly.
- Codex `requiredDiscovery` is display-gating only (5 entries, all ids match
  `DiscoveryIds`).
- No `if player has X advancement/item/visited Y` replacement paths found.

---

## Discovery Persistence

`HollowDiscoveryData` (server-wide SavedData on the overworld's storage,
UUID-keyed):

- Per-player, server-authoritative, persistent, idempotent (`grant` returns
  true only on first grant — once-only rewards trivially enforceable).
- Malformed/unknown entries dropped on load; unknown discovery ids are inert
  strings (no crash, no phantom unlocks).
- Survives death (player data untouched), clone (UUID-keyed, not
  instance-keyed), logout/login (re-synced), server restart (SavedData),
  dimension changes (server-wide storage).
- 100× repeated grant: `Set.add` → no-ops after the first; no duplicate
  rewards, no state corruption. Covered by `HollowDiscoveryDataTest`.

---

## Multiplayer

Conceptual two-player audit (A in Hollow / B in Overworld, then reversed):

- Research, discoveries, Codex visibility, travel origins: all UUID-keyed,
  server-wide — A's progression cannot unlock B's. Covered by
  `MultiplayerIsolationTest` (research) and `HollowDiscoveryDataTest`
  (per-player isolation).
- Anomaly world state (positions, ward registrations) is correctly shared in
  the Hollow level; effects apply per-player by AABB containment in that
  level only.
- All mutation happens on the server thread (tick/events/commands); no
  concurrent-modification path found (`AnomalySavedData.active()` iteration
  vs. structural changes sequenced within the sweep).
- Sync is per-player (`PacketDistributor.PLAYER`); snapshots are full-state
  (no delta bugs).

**No multiplayer isolation defects found.** (Live multiplayer execution
remains Category C — see Runtime Validation Boundary.)

---

## Save/Reload

Traced server start → join → progression → travel → discovery → logout →
shutdown → restart → login:

- `PlayerTravelData` / `HollowDiscoveryData` / research data: overworld
  `DataStorage` — always loaded, never attached to a temporary level.
- `AnomalySavedData`: Hollow level storage — correct scope for world state.
- `KineticManager`: transient per-level, rebuilt from BE registration.
- `SpireBlockEntity` charge: NBT-persisted, clamped on load.
- Ward positions: persisted **and** re-registered by BE `onLoad`;
  unregistered on `setRemoved`/`onChunkUnloaded` (idempotent set ops).
- All three SavedData classes drop malformed entries on load and mark dirty
  only on real mutation.

**No save/reload defects found.** One negligible deferred note: a ward block
removed while the server is offline leaves a stale ward position (D-5).

---

## Client/Server Boundaries

Delegated exhaustive audit (every `net.minecraft.client.*` import traced to
its load path):

- 13 files import client classes — **all** inside `client/` packages behind
  verified `Dist.CLIENT` event subscribers. **SAFE.**
- S2C packets (`DiscoverySyncPacket`, `ResearchSyncPacket`) reference
  `@OnlyIn(Dist.CLIENT)` state holders, but both holders contain **zero**
  client imports (pure `Set<ResourceLocation>`), and both packets are
  direction-locked to `PLAY_TO_CLIENT` — decode/handle never run on a
  dedicated server. **SAFE**, with a maintenance caution: keep those two
  holder classes dependency-free (D-6).
- `NetworkHooks.openScreen` in common blocks is the server-safe API
  (false positive). Anomaly particles/sounds use `ServerLevel` broadcast
  APIs. Entity classes reference no renderers. `HollowForgeEvents` (common)
  handles only common/server events with correct side guards.

**6 SAFE, 0 RISK, 0 BLOCKER.** Nothing here blocks Phase 6.

---

## Networking

- `DiscoverySyncPacket`: S2C (`PLAY_TO_CLIENT`), sent on login, dimension
  change, and after every grant. Tiny, infrequent, no per-tick traffic.
- C2S packets (`OpenResearchPacket`, `BuyResearchPacket`): requests only;
  server revalidates before mutating progression.
- Registration order in `NexusNetwork` preserved; Phase 5 added exactly one
  packet without disturbing Phase 4's.
- No client packet mutates authoritative progression.

**No networking defects found.** (Decode has no count bound, but the packet
is S2C from the trusted server — not a finding.)

---

## Resources/Data

Delegated audit (19 blocks, 28 items, 4 entities, 4 features, 148 JSONs):

- **No resource-loading breakers.** All blockstates/models/item-models exist;
  every model-referenced texture exists on disk; all placed→configured→biome
  references resolve; loot tables, recipes, tags, research/codex JSONs valid;
  all 5 `requiredDiscovery` values match `DiscoveryIds`.
- **Fixed (RISK-4):** 3 message keys missing from both languages
  (`clutch_engaged`, `clutch_disengaged`, `gearbox_ratio` — pre-existing
  Phase 2 debt) showed raw keys in chat. Added to `en_us` + `pt_br`.
- **Zero orphans.** No `beacon` residue anywhere.
- **Deferred (D-4):** `pt_br` lacks ~20 research GUI/command keys (falls back
  to English; all Phase 5 keys present in both languages).

---

## Performance

Recurring paths, measured by code inspection:

| System | Frequency | Scope | Assessment |
|---|---|---|---|
| Anomaly sweep | every 20 ticks, Hollow only | ≤8 anomalies; per-anomaly one AABB player query + bounded box iteration | fine |
| Static-field derate apply | per sweep, per static field | box volume iteration, but only map lookups (`providerAt`) per cell | fine |
| Derate clear on expiry | on expiry only | same bounded box | fine |
| Proximity discovery | every 200 ticks per player in Hollow | 33³ cube | **fixed**: `hasChunkAt` guard added (was able to force chunk gen) |
| Deep Hollow exposure | every 200 ticks per player in Hollow | single biome lookup | fine |
| Obelisk scanner | on use, 5 s cooldown | ≤4.2k chunk-guarded columns | fine |
| Ward check | per anomaly effect application | iterates registered wards only (sparse set) | fine |
| Discovery/research sync | on login / grant / dimension change | one small packet per player | fine |

No `O(players × all anomalies)` or global scans. The one pathological-adjacent
pattern (unguarded `getBlockState` cube) was fixed.

---

## Testing

Baseline 215/215 → after audit 225/225 (10 new regression tests).

Coverage map:

| Area | Status |
|---|---|
| Pure travel rules / safe spawn | COVERED |
| Pure anomaly rules (spawn cap, expiry, partition) | COVERED |
| Spire charge rule (incl. brownout) | COVERED |
| Anomaly ward suppression + SavedData NBT | COVERED |
| Discovery grant/idempotency/UUID isolation | COVERED |
| Research authority, gating, graph, persistence | COVERED |
| Codex discovery gates | COVERED |
| Worldgen JSON integrity | COVERED |
| **Derate-clear decision (expired vs. covered)** | **COVERED (new)** |
| **Transient cooldown lifecycle + logout cleanup** | **COVERED (new)** |
| Packet encode/decode round-trip | UNCOVERED — suitable for a pure unit test (FriendlyByteBuf is constructible); minor |
| Logout event wiring (`HollowForgeEvents`) | UNCOVERED — requires Forge event bus; Category C |
| Live dimension bootstrap / worldgen visuals | UNCOVERED — Category C (native runtime) |
| Live multiplayer behavior | UNCOVERED — Category C (native runtime) |

Two pre-existing tests with incorrect assertions were corrected during Phase 5
(documented in the Phase 5 report); no test was weakened in this audit.

---

## Runtime Validation Boundary

Per the standing constraint, validation is separated:

- **A — Pure automated validation:** 225/225 tests, manual `javac` against
  `forge-official.jar` (exit 0), JSON cross-reference checks, import-level
  client/server tracing. **Done.**
- **B — Manual artifact validation:** JAR rebuilt with audit fixes;
  `mods.toml` placeholders expanded; resources inspected. **Done.**
- **C — Native runtime validation:** actual Forge client/server launch,
  Hollow entry in-game, worldgen visuals, entity rendering/AI live, anomaly
  behavior live, multiplayer session. **Not possible in this sandbox**
  (ForgeGradle daemon handshake fails); **remains pending** on real hardware.

Category C items: entity spawn behavior on real terrain, renderer/model
visuals, dimension-effects registration at runtime, packet sync in a live
multiplayer session, worldgen landmark placement density "feel", deep-hollow
exposure pacing, spire charge UX timing.

---

## Reusability Analysis

For Phase 6 (The Ether), per candidate:

| Candidate | Verdict |
|---|---|
| Dimension registration pattern (`DeferredRegister` + datapack JSON) | **REUSABLE AS-IS** — conventions, not code |
| Pure travel rules (`HollowTravelService` shape) | **REUSABLE WITH SMALL EXTRACTION** — permission-via-research + safe-spawn search generalize; keep per-dimension policy classes, don't build a framework yet |
| Player travel state (`PlayerTravelData` shape) | **REUSABLE WITH SMALL EXTRACTION** — UUID→origin map per dimension; a second dimension would duplicate ~97 lines — acceptable once, extract if a third appears |
| Discovery framework (`HollowDiscoveryData` + `DiscoveryIds` + S2C sync) | **REUSABLE WITH SMALL EXTRACTION** — the per-UUID grant/sync pattern is the template; keep per-dimension id sets |
| Anomaly framework (`AnomalyService` + `AnomalyManager` shape) | **REUSABLE WITH SMALL EXTRACTION** — pure lifecycle rules are dimension-agnostic; the MC driver is Hollow-flavored (particles, wards, kinetic) — copy the shape, not the file |
| Dimension SavedData patterns | **REUSABLE AS-IS** |
| `HollowDimensions` identifiers | **KEEP HOLLOW-SPECIFIC** — and for Ether, prefer plain constants in `api` (RISK-5) |
| Worldgen conventions (data-driven + tiny features) | **REUSABLE AS-IS** |
| Landmark features | **KEEP HOLLOW-SPECIFIC** |
| `PlayerCooldowns` | **REUSABLE AS-IS** — generic transient per-player cooldowns |

**Deliberately NOT built:** no generic dimension framework, no base classes,
no registries-of-dimensions. One more dimension will tell us what the real
abstractions are; premature generalization is the bigger risk.

---

## Architecture Smells

Searched explicitly per the audit checklist:

- **Static mutable global state:** `HollowForgeEvents.ANOMALIES` (static
  singleton) — justified (one server, one Hollow), but its UUID maps leaked
  → **fixed** (RISK-2). `KineticManager.INSTANCES` is a `WeakHashMap`
  keyed by level — no leak. No other static mutable state found.
- **UUID maps without lifecycle cleanup:** the anomaly cooldown maps →
  **fixed**. `PlayerTravelData`/`HollowDiscoveryData` maps are persistent
  SavedData (correct scope), bounded by player count.
- **Client state used by server logic / vice versa:** none found
  (exhaustive import trace).
- **Duplicate progression authorities:** none — research is the single
  authority; discoveries grant points only.
- **Hardcoded dimension IDs:** `HollowDimensions` centralizes them; no
  string literals scattered.
- **Hardcoded Hollow assumptions in shared code:** none — `KineticManager`
  knows nothing of dimensions; derate is caller-owned.
- **Direct MC dependencies in pure domain:** `ResourceLocation` as value
  type only — acceptable.
- **Hidden tick loops:** none — all recurring work is on explicit cadences
  (20/100/200 ticks) with bounded scopes.
- **Unsafe event ordering assumptions:** none found; spawn placements use
  `Operation.REPLACE` explicitly.

---

## Findings

### SAFE (34)

Bootstrap/registry timing; `DeferredRegister` usage; datapack paths;
`dimension/hollow.json` → type resolution; dimension-type codec fields
(incl. nested `monster_settings`); configured/placed feature wiring;
biome feature lists; multi-noise biome source; ore placement; landmark
determinism; landmark dimension isolation; entity registration; entity
attributes; spawn predicates; entity dimension restriction; entity AI kits;
entity client boundary; entity despawn/persistence semantics; phantom
lifecycle; vanilla entity sync; anomaly ownership/lifecycle (10 questions);
research single authority; discovery idempotency/persistence; multiplayer
isolation (conceptual); SavedData placement/scope; NBT malformed-data
tolerance; S2C/C2S packet directions; client import boundary (6 subsumed);
server-safe particle/sound APIs; travel lifecycle incl. death/logout edge
cases; spire charge gating; obelisk return fallback chain.

### RISK (5)

- **RISK-1 — Stale kinetic derates after anomaly expiry.** FIXED.
  `dimension/mc/AnomalyManager.java` (`clearAnomalyDerates` + `isUncovered`
  seam) + `AnomalyDerateLifecycleTest` (6 tests).
- **RISK-2 — Transient per-player cooldown entries never evicted.**
  FIXED. New `dimension/mc/PlayerCooldowns.java` (pure) +
  `AnomalyManager.onPlayerLogout` + `PlayerLoggedOutEvent` wiring in
  `HollowForgeEvents` + `PlayerCooldownsTest` (4 tests).
- **RISK-3 — Proximity discovery scan could force chunk generation.**
  FIXED. `hasChunkAt` guard in
  `HollowForgeEvents.checkProximityDiscoveries`.
- **RISK-4 — Missing lang keys showed raw keys in chat**
  (`clutch_engaged`, `clutch_disengaged`, `gearbox_ratio`; Phase 2 debt).
  FIXED in `en_us.json` + `pt_br.json`.
- **RISK-5 — `dimension/api/HollowDimensions` couples the `api` package to
  MC types** (`Level`, `Registries`). Works correctly; left unchanged by
  design. Note for Phase 6: prefer plain identifiers in new `api` packages.

### BLOCKER (0)

None.

### DEFERRED (6)

- **D-1:** Entity textures for the 4 Hollow entities not shipped (visual
  only; vanilla binds missing texture, no crash).
- **D-2:** Jigsaw/NBT structures (ADR-011 — the `Feature` approach stands).
- **D-3:** Custom noise settings for the Hollow (reuses overworld).
- **D-4:** `pt_br` incomplete for ~20 research GUI/command keys (falls back
  to English; all Phase 5 keys translated).
- **D-5:** Ward position can go stale if the block is removed while the
  server is offline (negligible impact).
- **D-6:** `DiscoveryClientState` / `ResearchClientState` must stay free of
  `net.minecraft.client` imports (maintenance caution for the S2C pattern).

---

## Required Fixes Before Phase 6

All required fixes were applied in this audit (RISK-1…RISK-4). No code
changes remain outstanding. The only pre-Phase-6 requirement is
environmental:

1. **Native Forge runtime validation** on real hardware (client + dedicated
   server): launch, Hollow entry/return, worldgen inspection, entity
   rendering, anomaly behavior, multiplayer session. Category C by definition.

---

## Recommended Phase 6 Architectural Constraints

1. **Research stays the single progression authority.** No advancement-,
   item-, or visit-based shadow progression.
2. **Pure domain first:** Ether travel/anomaly-equivalent rules as pure
   classes; Forge adapters thin. Reuse the *shape* (`*Service` + `mc/*`),
   not Hollow's files.
3. **No generic dimension framework yet.** A second dimension is the
   experiment that reveals the real abstractions; extract on the third.
4. **Per-UUID state in server-wide SavedData; world state in per-level
   SavedData.** Never static maps without a logout/unload cleanup path
   (lesson of RISK-2).
5. **Transient cross-system effects (derates, cooldowns) must have a
   documented owner and a clearing path for every removal scenario**
   (lesson of RISK-1).
6. **World scans need loaded-chunk guards** (lesson of RISK-3).
7. **Client holders referenced by packets stay dependency-free** (D-6).
8. **New `api` packages use plain identifiers**, not pre-built MC keys
   (RISK-5).
9. **Ether content stays data-driven** (biomes/features/placements as JSON)
   following the Hollow worldgen conventions.

---

## Final Readiness Assessment

**READY WITH CONDITIONS**

The Hollow's dimensional architecture is a sound foundation for Phase 6:
layering is real, persistence is correctly scoped, multiplayer isolation
holds by construction, the client/server boundary is clean, and the four
genuine defects found are fixed with regression tests (225/225).

Conditions:

1. Native Forge runtime validation (Category C) must be executed on real
   hardware before Ether content is considered validated — this is an
   environmental limitation of the sandbox, not an architectural gap.
2. Phase 6 design must respect the architectural constraints above, in
   particular: no premature dimension framework, research as the single
   authority, and lifecycle-complete transient state.

No Phase 6 gameplay was implemented in this audit.

---

## Appendix — Validation categories

- **A (pure automated):** 225/225 tests; `javac` against `forge-official.jar`
  exit 0 (131 main + 25 test files); JSON cross-reference verification;
  exhaustive client-import tracing.
- **B (manual artifact):** JAR rebuilt with audit fixes; `mods.toml`
  placeholders expanded; resources inspected in the artifact.
- **C (native runtime):** pending — requires real Forge execution.
