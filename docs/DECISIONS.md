# NEXUS: Echoes of Reality — Decision Records

Architectural Decision Records. Newest first. Format: context → decision → consequences.

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
