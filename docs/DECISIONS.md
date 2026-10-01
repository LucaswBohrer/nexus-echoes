# NEXUS: Echoes of Reality — Decision Records

Architectural Decision Records. Newest first. Format: context → decision → consequences.

---

## ADR-012 — Phase 5.5: dimensional architecture audit (2026-10-01)

**Context:** After Phase 5 introduced a full dimensional layer (dimension,
travel, anomalies, discovery, entities, worldgen), the architecture needed a
formal audit before The Ether multiplies it. Constraints: no new content,
fix only genuine defects with the smallest safe change, do not weaken tests,
no generic dimension framework yet.

**Decision:** Audited every dimensional class (domain vs. adapter vs. client),
bootstrap/registry ordering, the 1.20.1 dimension-type codec, worldgen data
chains, anomaly/kinetic/travel lifecycles, research authority, multiplayer
isolation, save/reload paths, client/server imports (exhaustive), networking
directions, and all 148 JSONs. Fixed 4 genuine low-risk defects with
regression tests: (1) stale kinetic derates when a node was removed
mid-anomaly — derate clearing now covers every uncovered position;
(2) static per-player cooldown maps in `AnomalyManager` never evicted — new
pure `PlayerCooldowns` + logout cleanup; (3) proximity discovery scan could
force chunk generation — added `hasChunkAt` guard; (4) 3 missing lang keys
(Phase 2 debt) — added to both languages. Documented but unchanged:
`dimension/api/HollowDimensions` couples the `api` package to MC types
(works, acceptable); Jigsaw/NBT, custom noise, and entity textures stay
deferred. Zero blockers; gate **READY WITH CONDITIONS** (conditions:
native runtime validation still pending on real hardware; Phase 6 must follow
the audit's architectural constraints — no premature dimension framework).

**Consequences:** +1 production class (`PlayerCooldowns`), +2 test classes
(10 tests), 225/225 green. Full report: `docs/PHASE_5_5_AUDIT.md`.

---

## ADR-011 — Phase 5: The Hollow dimension architecture (2026-10-01)

**Context:** Phase 5 adds the first real dimension. The audit (spec §1–2) found
no architectural blocker: Forge 1.20.1 dimensions, dimension types, biomes,
features are data-driven datapack JSON; teleportation, persistence and
spawning all have server-side APIs; research already owns progression. The
risk is not capability but shape — a "HollowManager" god-object or a one-off
dimension hack would have to be rewritten for The Ether and The Core.
Hard constraints from the phase brief: research stays the only progression
authority; `hollow_access` gates per-player; no second kinetic system and no
global physics changes; no per-tick global scans; no runtime structure
generation; machines need structural models.

**Decision:** a `dimension/` module mirroring the established
pure-domain → thin-MC-adapter split:

- **Domain (no Minecraft classes):** `dimension/api` (dimension/biome keys),
  `dimension/travel` (`HollowTravelService`: permission rule + safe-spawn
  search over an injected height function), `dimension/anomaly`
  (`AnomalyDefinition`, `AnomalyInstance`, `AnomalyService`: spawn caps,
  expiry, effect selection — all pure), `dimension/discovery`
  (`DiscoveryIds`: `enter_hollow`, `obelisk`, `ruin_found`, `anomaly_seen`,
  `deep_hollow`, `memory_fragment`).
- **The dimensional spire is the entry mechanism — not a portal, not a new
  energy system.** The roadmap's early sketch mentioned a "Hollow-native
  energy type"; the phase constraints forbid a second kinetic system, so the
  spire is instead a *kinetic consumer* (240 RPM / 25 N·m, genuinely needs a
  2:1 gearbox off the standard generator). It accumulates charge for 60 s at
  full power (brownout slows charging instead of stalling it); right-click at
  full charge crosses into the Hollow and **drains the charge to zero** —
  every crossing must be earned. Crafting/placing/using the spire is gated on
  `hollow_access`. The recipe uses overworld materials only
  (resonant crystals, iron, nexus component) — no chicken-and-egg with
  Hollow resources.
- **The obelisk is the only return mechanic.** `PlayerTravelData`
  (Overworld `DimensionDataStorage`, per-player UUID → origin
  dimension+pos) records where each traveler came from; right-clicking an
  `obelisk_core` returns them there. A redundant `HollowBeaconBlock` was
  prototyped mid-phase and **removed**: two entry/return devices would split
  the mechanic without adding gameplay. Death/respawn keeps vanilla
  semantics, documented: `bed_works=true`, `respawn_anchor_works=false`.
- **Worldgen is data-driven except where code is honest.**
  `dimension/`, `dimension_type/`, `worldgen/biome/` ×4 are JSON. The
  dimension reuses the vanilla **overworld noise settings**
  (`minecraft:overworld`) — no custom `noise_settings/hollow` was authored;
  identity comes from blocks, biomes, fog, features and content, not from
  exotic density math that cannot be visually verified in this sandbox.
  Landmarks are deterministic custom `Feature<NoneFeatureConfiguration>`
  classes (`obelisk`, `ruin`, `vault`, `fracture_spire`) placed via
  configured/placed-feature JSON referenced directly from the biome
  generation arrays — **explicitly not** `Structure`/`StructureSet`
  (no Jigsaw pipeline exists in this repo to author or verify `.nbt`
  templates; calling them "structures" in player-facing text is fine,
  calling them structures in technical docs is not). Obelisk rarity was
  tuned 250 → 60 so the 128-block scanner reliably finds one.
- **Travel is server-authoritative.** `HollowTravelService.canTravel`
  reuses `ResearchService.isUnlocked(hollow_access)` — no second progression
  system. The MC adapter (`HollowTravel`) teleports via
  `ServerPlayer.changeDimension` with a custom `ITeleporter` running the pure
  safe-spawn search (spiral fallback, never inside blocks).
- **Anomalies are server-owned, sparse, and client-light.** `AnomalySavedData`
  lives in the Hollow level's `DimensionDataStorage`; `AnomalyManager` sweeps
  every 20 ticks, caps active anomalies at 8, spawns sparsely near players —
  no global scans. The client only sees vanilla particles
  (`ServerLevel.sendParticles`); no custom anomaly packets. Kinetic
  interference (`static_field`) is applied at the adapter layer only: a
  transient derate map in `KineticManager` (`setDerate`, consulted when
  building `SimNode`s, rebuild on change). The pure simulator is untouched —
  anomalies can never corrupt network state. `AnomalyWardBlock` (24-block
  radius, registered in `AnomalySavedData`) suppresses all effects; wards
  need no power.
- **Discovery-gated Codex is additive.** `CodexEntry` gains an optional
  `requiredDiscovery`; `HollowDiscoveryData` (per-player set, Overworld
  storage) syncs via `DiscoverySyncPacket` (S2C). Entries without
  `requiredDiscovery` behave exactly as in Phase 4. Five Hollow entries
  (`the_hollow`, `obelisks`, `anomalies`, `deep_hollow`,
  `memory_fragments`) unlock progressively through play.
- **Research branch, not a new system.** `dimensional_resonance` (250) →
  `hollow_exploration` (200, unlocks `anomaly_ward`) → `anomaly_studies`
  (250, unlocks `resonance_scanner`). Research rewards: first entry
  (+25), obelisk (+15), ruin (+15), anomaly sighting (+10), memory-fragment
  analysis (+20). Per-UUID isolation like Phase 4 — shared dimension,
  private progression.
- **Registries stay single-file** (ADR-002): new `ENTITY_TYPES` and
  `FEATURES` `DeferredRegister`s join `NexusRegistries`. Sounds use vanilla
  events only — no custom sound registry in Phase 5.

**Consequences:** The Ether/Core reuse the same module shape
(domain → adapter, data-driven worldgen, server-owned travel/state). What is
honestly deferred: visual verification of terrain/fog/structures/entities
(`POST-PHASE-1 RUNTIME VALIDATION`), Jigsaw structures, custom
`noise_settings`, custom sounds. Tests target the pure domain (travel rules,
safe-spawn, anomaly lifecycle, discovery gating, multiplayer isolation) plus
JSON/data validation; the 174-test baseline must stay green.

---

## ADR-010 — Phase 4: research & technological progression (2026-10-01)

**Context:** Phase 4 must add the first real technology-progression layer:
`EXPLORE → DISCOVER → RESEARCH → UNLOCK → BUILD → ADVANCE`. Research is a
*progression* system, not another energy system. It must be per-player,
server-authoritative, persisted, data-driven, multiplayer-safe, and able to
gate Phase 3 content — without touching the kinetic/machine simulation, and
without implementing any dimension (Hollow/Ether/Core are explicitly out of
scope; only an unlock *hook* for them may exist).

**Decision:**

- **Research is its own clean domain.** Pure core under
  `com.nexus.echoes.research` (definition, dependency graph, player state,
  service, sources, codex) with no Minecraft imports except NBT for
  serialization (NBT already serializes headlessly in tests). Minecraft
  adaptation lives in `research.mc` (SavedData, reload listeners, events,
  commands, packets, menus). The core never knows about players, levels or
  packets; the adapter never owns progression rules.
- **State home: server-side `SavedData`, keyed by player UUID.**
  `ResearchSavedData` lives in the overworld's `DimensionDataStorage` and
  maps `UUID → PlayerResearchState` (points int, completed research set,
  claimed once-only sources). UUID-keying means death, respawn/clone,
  dimension changes and logout/login need no special handling — the row
  survives all of them. `setDirty()` on every mutation; disk persistence
  covers server restarts. No player persistent-NBT, no capabilities, no
  global static state, no client authority.
- **Research points are integers, deterministic, server-authoritative.**
  Sources are event-driven through a small `ResearchSource` interface
  (`id`, `points`, `oncePerPlayer`). Initial set: mining `nexus_ore`
  (+2, repeatable), first `nexus_ore` mined (+10, once), first `gear`
  crafted (+10, once). Negative gains are rejected; spending requires
  balance; duplicate completion is impossible (completed set).
- **Definitions are data-driven JSON** under
  `data/nexus_echoes/research/*.json`, loaded by a
  `SimpleJsonResourceReloadListener` on both sides (server authoritative
  for logic, client copy for GUI display). Validation is fail-fast at load:
  malformed IDs, `cost <= 0`, unknown prerequisites, duplicate IDs and
  dependency cycles are rejected with clear errors. Initial tree:
  `industrial_foundations` (50) → `kinetic_transmission` (100) →
  `advanced_processing` (150) → `dimensional_resonance` (250). Costs live in
  the JSON, not in `NexusConfig` (config is for operator policy, not
  progression values).
- **Unlocks are a centralized query, not scattered conditionals.**
  Definitions declare `unlocks: [<technology-id>]`;
  `ResearchService.isUnlocked(state, technologyId)` is the single choke
  point. Initial technologies: `nexus_echoes:crusher`,
  `nexus_echoes:processor`, `nexus_echoes:nexus_component`,
  `nexus_echoes:hollow_access` (future hook — no dimension, portal, blocks
  or worldgen are registered; Phase 5+ consumes the unlock).
- **Gating model (no machine-BE changes, no recipe duplication).**
  Forge has no clean per-player recipe hook (`ItemCraftedEvent` fires
  post-hoc from `ResultSlot`), so gating is enforced where it is provably
  correct, all server-side, all decided by the pure `ResearchGating` rule:
  (1) `BlockEvent.EntityPlaceEvent` cancels placement of locked machine
  blocks; (2) `CrusherBlock`/`ProcessorBlock.use()` refuses to open the GUI
  without the unlock; (3) `ItemCraftedEvent` voids the crafted stack
  (`getCrafting().setCount(0)` — verified to fire from `ResultSlot` on the
  taken stack) and explains the missing research. Locked technology cannot
  be used through the client; the machine BEs never learn about research.
- **Instant unlocks, no active-research timers.** `completeResearch`
  validates prerequisites + cost atomically and records completion. No
  in-progress state is introduced because the design doesn't need it.
- **Networking: three packets on the existing channel.**
  C2S `OpenResearchPacket` (keybind pressed → client asks for a snapshot),
  C2S `BuyResearchPacket(researchId)` (server validates via
  `ResearchService`, never trusts the client),
  S2C `ResearchSyncPacket` (points + completed set; sent on login and on
  every mutation). The research UI is a client-side `Screen` opened directly
  by the keybind (no `ResearchMenu`/container is registered — the UI has no
  slots, so server-opened menus would add plumbing for nothing); the client
  keeps a read-only snapshot (`ResearchClientState`) and every mutation is
  server-authoritative via C2S+S2C round-trip. The Codex screen reuses the
  synced completed-set plus the client's local codex entries.
- **Codex is data-driven documentation + lore, not a recipe book.**
  Entries under `data/nexus_echoes/codex/*.json`
  (title/category/content/optional `requiredResearch`); visibility is a pure
  function of the completed set. First layer only: introduction, kinetic
  systems, industrial processing, research methods, and two subtle lore
  fragments that ask "what is the Nexus?" without answering it.

**Consequences:** New tests cover points, prerequisites, cycles, unlocks,
NBT persistence, clone simulation, multiplayer isolation, definition
validation, gating rules, codex visibility and a full progression
integration — **174/174 green** (78 previous + 96 new, 2026-10-01). Honest deltas: recipe
*visibility* is not hidden (vanilla has no per-player recipe filter —
attempting locked crafts voids the result with an explanation instead);
machine BEs are research-agnostic by design; codex has no page-turning art;
`POST-PHASE-1 RUNTIME VALIDATION` still pending (no real Forge client/server
has run this).

---

## ADR-009 — Phase 3: industrial processing on the kinetic network (2026-10-01)

**Context:** Phase 3 must prove `RESOURCE → PROCESSING → COMPONENT → TECHNOLOGY`
(Raw Resource → Crusher → Processor → Refined Material → Component → Machines)
using the Phase 2 kinetic API, without a third energy infrastructure, without
copy-pasting machine logic, and with data-driven recipes/worldgen.

**Decision:**

- **One shared kinetic-machine base.** `AbstractKineticMachineBlockEntity`
  centralizes kinetic registration/snapshots/NBT, inventory, recipe cache,
  integer progress + fractional accumulator, server-side processing,
  proportional brownout and the 6-index `ContainerData`
  (rpm, torque mN·m, node status, progress, maxProgress, machine status).
  Concrete machines only declare required RPM/torque and their recipe type.
  The Resonator was refactored onto this base (keeps its legacy `resonating`
  type and 5-index menu data so its GUI is untouched).
- **Phase 1 energy contract preserved.** The base takes explicit
  `energyCapacity`/`energyMaxReceive`: the Resonator restores its Phase 2
  configured storage (`NexusConfig.RESONATOR_CAPACITY/MAX_RECEIVE`) verbatim;
  Crusher/Processor pass `0/0` — they hold no energy buffer at all. No second
  energy system, no parallel buffer.
- **Numbers are progression, not decoration.** Crusher: 120 RPM / 20 N·m;
  Processor: 240 RPM / 15 N·m. A 120 RPM / 50 N·m generator through a 2:1
  gearbox delivers 240 RPM / ~25 N·m — the Processor *requires* real
  transmission, and a third consumer on one source causes overload.
- **Brownout is proportional to the scarcest mechanical resource:**
  `speedFactor = min(deliveredRPM/requiredRPM, deliveredTorque/requiredTorque)`
  via the pure `ProcessingGovernor` (unit-tested). Fractional progress
  accumulates so slow machines still advance smoothly.
- **Machine states:** `RUNNING, IDLE, NO_POWER, BROWNOUT, BLOCKED` —
  `BLOCKED` = output/byproduct has no room. Byproduct space is required even
  though the drop is probabilistic: the alternative is voiding items, and the
  machine must never destroy resources silently.
- **Recipes are data-driven and validated.** `ProcessingRecipe`
  (input/output/processingTime/optional byproduct+chance); separate
  `crushing`/`processing` types + serializers. Constructor rejects
  `processingTime <= 0`, chance outside `[0,1]`/NaN, empty byproduct stacks —
  datapack errors fail fast instead of corrupting saves.
- **Worldgen is data-driven and minimal.** One ore (`nexus_ore`, vein 7,
  count 7, -32..48, overworld, `underground_ores`); configured + placed
  features + biome modifier under `data/nexus_echoes/forge/biome_modifier/`
  (singular — the plural path silently never loads on Forge 47.2.0).
- **Automation rules are explicit and minimal.** Menu `OutputSlot`s block
  manual insertion; `allowsExternalInsert` blocks hopper/pipe insertion into
  output/byproduct (input accepts insertion; every slot allows extraction so
  automation can pull wrong items or unload). Internal crafting uses
  `setStackInSlot` and bypasses the rule.
- **Per-face sprites + player facing.** Models use
  `minecraft:block/orientable` (`front`/`side`/`top`); `facing` comes from a
  shared `AbstractOrientedMachineBlock` (furnace-style, one place).
  Resonator/Creative Cell stay orientation-agnostic — untouched.

**Consequences:** The full chain
`Generator → Shaft → Gearbox → Crusher → Processor` is covered by integration
tests (brownout, overload, clutch, persistence round-trips); 78/78 unit tests
green; `javac` clean (93 classes); JAR reassembled. Honest deltas: no JEI, no
datagen, no crate, no fluids, no `active` blockstate (GUI shows status),
recipe re-evaluated from input after reload (no recipe-ID persistence), recipe
matching still needs a real Forge runtime (headless Bootstrap can't load
client language assets). `POST-PHASE-1 RUNTIME VALIDATION` still pending.

---

## ADR-008 — Phase 2 shipped as implemented; scope deltas documented (2026-10-01)

**Context:** The Phase 2 spec asked for belts, stress-break, hand crank/windmill
sources, rotating-part visualization, datagen and `EnergyType.KINETIC`. Building all
of that at once would have risked the architecture the spec also demands
("prioritize a solid implementation" over implementing everything simultaneously).

**Decision:** Phase 2 ships the solid core — pure-Java sim, manager, generator,
shaft/gear/gearbox/clutch, migrated Resonator, GUIs, sync, NBT, `/nexus kinetic`,
50/50 tests — and documents the deltas honestly in ROADMAP instead of pretending
they exist:

- No belts, no hand crank/windmill/waterwheel, no stress-break, no datagen yet.
- Kinetic is NOT an `EnergyType` variant (see ADR-007); it is a separate simulation.
  `INexusEnergy` remains for resonant storage / future machines.
- Every transmission component currently hosts a light `KineticBlockEntity`
  (needed for NBT state + vanilla update-packet sync in Phase 2 scope); the
  BE-vs-stateless-component split is a known review item for Phase 3.
- `KineticManager` connects any adjacent kinetic blocks (axis-agnostic); axis-aware
  connections are future work.

**Consequences:** The required flow
`Generator → Shaft → Gearbox → Resonator → Crystal` exists end-to-end with real
RPM/torque/derived power, demand, load, sync, GUI, persistence, tests and
diagnostics. Deferred items are tracked, not silently dropped.

---

## ADR-007 — Kinetic energy as a separate pure-Java simulation (2026-10-01)

**Context:** Phase 2 needs RPM, torque, derived power, gear ratios, direction,
branches, overload and multiplayer sync. The Phase 1 `INexusEnergy` contract is an
integer *storage* abstraction — RPM/torque cannot honestly be squeezed into it
without lying about the physics (a shaft is not a battery).

**Decision:** Kinetic gets its own stack, independent of the resonant storage API:

- Domain value types `Rpm`, `Torque`, `Power` (reject negative/NaN/infinite);
  power is **derived only** (`P = τ × ω`), never an independent input.
- A **pure-Java network simulator** (`kinetic/sim`) with no Minecraft classes:
  `SimNetwork` + `SimNode` + `KineticSnapshot`. All math, overload and conflict
  semantics are unit-testable without the game (39 tests green in Phase 2).
- Propagation is SOURCE → TRANSMISSION → CONSUMER. Each transmission node applies
  its transform **on entry** (gearbox ratio, gear teeth mesh with direction flip,
  clutch engage/disengage). Consumers are sinks.
- Demand is referred to the source through the torque gain of the path
  (`demandAtSource = requiredTorque / gain`); per-source overload = Σ demands >
  rated torque. Brownout: every consumer's delivered torque scales by
  `supply / demand`, capped by what the transmission can physically carry:
  `delivered = min(required × scale, rated × gain)`.
- Two sources touching one node = **conflict**: first source (lowest id) wins,
  snapshot flags `conflict = true` for the diagnostic UI. No silent merging.
- The Minecraft layer (`kinetic/mc`) builds `SimNode`s from block entities and
  holds one `KineticManager` per `ServerLevel` with **dirty-topology caching**:
  the network is re-simulated only when blocks change or every N ticks, never
  from scratch each tick. The client never simulates; it receives snapshots.

**Consequences:** Two energy systems coexist honestly (resonant storage +
kinetic flow). The simulator is the single source of truth for physics and is
fully tested; the Minecraft layer is a thin adapter (topology → nodes, snapshot
→ packets/NBT/GUI). Future energy types reuse the pattern: pure sim + thin adapter.

## ADR-006 — Phase 1 energy producer is a creative-only cell (2026-10-01)

**Context:** Phase 1 must prove producer → transmission → consumer, but real generators
belong to Phase 2 (kinetic). Shipping a survival-craftable generator now would preempt
Phase 2's design.

**Decision:** Add `creative_energy_cell`: infinite resonant producer, creative-tab only,
pushes energy to adjacent `INexusEnergy` consumers. No recipe, no survival obtainability.

**Consequences:** Phase 1 is fully testable in survival-like conditions for the machine
half of the loop; the generation half is explicitly stubbed and replaced in Phase 2.

## ADR-005 — Push-model energy transfer, neighbor-only (2026-10-01)

**Context:** A full energy network graph is needed for kinetic (Phase 2) but overkill for
resonant.

**Decision:** Phase 1 transfer = producers push to the 6 neighbors every 10 ticks.
No graph, no pathfinding.

**Consequences:** Simple, TPS-safe. The graph simulator arrives with kinetic networks;
the `INexusEnergy` interface is already shaped to accommodate it.

## ADR-004 — Typed energy from day one (2026-10-01)

**Context:** The mod's identity is multi-energy progression (kinetic → ether → core).
A single FE-like system would bake in wrong assumptions.

**Decision:** `EnergyType` enum + `INexusEnergy` contract. Phase 1 ships only `RESONANT`.

**Consequences:** New energies add types/storage/transport without touching the contract.
Slight upfront abstraction cost, paid once.

## ADR-003 — Data-driven recipes in Phase 1 (2026-10-01)

**Context:** "Data-driven whenever possible" is a project principle; hardcoding the first
recipe would set the wrong precedent.

**Decision:** Custom `RecipeType` + `RecipeSerializer` for the Resonator from the start;
recipes live in `data/nexus_echoes/recipes/`.

**Consequences:** ~3 extra classes in Phase 1; every future machine follows the pattern
for free.

## ADR-002 — Single-file registries (2026-10-01)

**Context:** Scattered `DeferredRegister`s across content classes cause mod-id typos and
make audits hard.

**Decision:** `registry.NexusRegistries` owns ALL registers. Content classes expose
`RegistryObject`s declared there.

**Consequences:** One choke point; slightly less "colocated" code, much easier review.

## ADR-001 — Forge 1.20.1, official (Mojang) mappings (2026-10-01)

**Context:** Target platform per project brief.

**Decision:** Forge 47.2.0, Mojang official mappings (`official 1.20.1`).

**Consequences:** Stable, documented, largest 1.20.1 addon ecosystem for future
integrations. Sticking with 1.20.1 until a deliberate, recorded migration decision.
