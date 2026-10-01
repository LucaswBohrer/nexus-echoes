# NEXUS: Echoes of Reality — Decision Records

Architectural Decision Records. Newest first. Format: context → decision → consequences.

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
