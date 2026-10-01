# NEXUS: Echoes of Reality — Architecture

> Living document. Phase 1 scope is normative; later phases are directional.
> When an architectural decision changes, record it in `DECISIONS.md` with the reason.

## 1. Module map

```
com.nexus.echoes
├── NexusEchoes.java            # @Mod entrypoint, wires everything, nothing else
├── core/                       # mod id, creative tabs, config holder
├── registry/
│   └── NexusRegistries.java    # ALL DeferredRegisters live here (single choke point)
├── energy/
│   ├── api/
│   │   ├── EnergyType.java     # RESONANT (p1), KINETIC (p2), VOID/ETHER/CORE (later)
│   │   └── INexusEnergy.java   # THE energy contract: receive/extract/simulate
│   └── NexusEnergyStorage.java # reference implementation (int-based, NBT-serializable)
├── machines/
│   ├── AbstractNexusMachineBlock.java
│   ├── AbstractNexusMachineBlockEntity.java  # energy + progress + dirty-flag sync
│   ├── ResonatorBlock.java
│   ├── ResonatorBlockEntity.java
│   ├── ResonatorMenu.java
│   └── recipe/
│       ├── ResonatorRecipe.java          # data-driven recipe (JSON)
│       └── ResonatorRecipeSerializer.java
├── network/
│   ├── NexusNetwork.java                 # SimpleChannel, versioned protocol
│   └── packet/
│       └── MachineSyncPacket.java        # S2C: pos + energy + progress
├── content/                    # (phase 1: items/blocks registered centrally)
├── integration/
│   └── IntegrationManager.java # ModList.isLoaded("x") gates; never hard deps
├── client/
│   ├── ClientSetup.java        # screen/menu registration (client-only)
│   └── screen/
│       └── ResonatorScreen.java
├── research/                   # Phase 4: pure domain + mc adapter + client
│   ├── ResearchDefinition.java # namespaced id, title, desc, category, cost>0, prereqs, unlocks
│   ├── ResearchGraph.java      # validation: duplicates/unknown/self/cycles (DFS)
│   ├── PlayerResearchState.java# points, completed, claimed sources; NBT round-trip
│   ├── ResearchService.java    # single authority: sources/eligibility/completion/unlocks
│   ├── ResearchSource(s).java  # event-driven point sources (id, points, oncePerPlayer)
│   ├── ResearchGating.java     # pure craft/place/use rules per technology
│   ├── ResearchTechnologies.java # nexus_echoes:crusher/:processor/:nexus_component/:hollow_access
│   ├── codex/                  # CodexEntry + CodexVisibility (pure)
│   ├── mc/                     # ResearchSavedData (UUID-keyed), ResearchManager,
│   │                           #   definition/codex reload loaders, events, /nexus research,
│   │                           #   ResearchGatekeeper (server-side enforcement)
│   ├── network/                # ResearchSyncPacket (S2C), OpenResearchPacket (C2S),
│   │                           #   BuyResearchPacket (C2S, server revalidates)
│   └── client/                 # ResearchClientState (read-only snapshot), keybind R,
│                               #   ResearchScreen + CodexScreen (client-side, no menu)
└── (future) dimensions/ worldgen/ entities/ ai/ rendering/
```

**Rule:** content packages depend on `energy/api`, `machines` (abstracts), `network`,
`registry`, `core` — never the other way around. The abstract machine layer must not know
about the Resonator.

## 2. Energy model

Energy is **typed** from day one. Phase 1 ships one type; the type system is what matters.

```java
public interface INexusEnergy {
    EnergyType getEnergyType();
    int receiveEnergy(int maxReceive, boolean simulate);
    int extractEnergy(int maxExtract, boolean simulate);
    int getEnergyStored();
    int getMaxEnergyStored();
    boolean canReceive();
    boolean canExtract();
}
```

- `EnergyType.RESONANT` — Phase 1 generic stored energy (proves the pipeline:
  producer → transmission → consumer → GUI).
- `EnergyType.KINETIC` — Phase 2 (RPM/torque/power; its own network sim, same interface
  shape where it makes sense, separate physics where it doesn't).
- Later types (`ETHER`, `VOID`, `CORE`) follow the same pattern: new `EnergyType`,
  new storage/transport, **no changes** to `INexusEnergy`.

### Transmission (Phase 1)

Push model, deliberately simple:

- A **producer** (Phase 1: creative energy cell) each server tick (throttled to every
  10 ticks) offers energy to the 6 neighbors that expose `INexusEnergy` of a compatible type.
- Compatibility in Phase 1: exact type match. Converters arrive with new energy types.
- No energy network graph yet — direct neighbor transfer only. The graph is a Phase 2/3
  concern (kinetic networks need it; resonant doesn't).

### Persistence

`NexusEnergyStorage` serializes to NBT (`energy` int). Block entities save/restore it in
`saveAdditional`/`load`. Survives chunk unload, world reload, and (via packets) GUI display.

### Kinetic network (Phase 2, ADR-007)

Separate physics, separate code. Package `kinetic`:

```
kinetic/
├── Rpm.java, Torque.java, Power.java, RotationDirection.java  (domain value types)
├── KineticMath.java            (ω = RPM·2π/60, P = τ·ω, ratio transforms)
├── api/                        (NodeRole, KineticFlow, NodeStatus)
├── sim/                        (SimNetwork, SimNode, NodeState, KineticSnapshot)
└── mc/                         (Minecraft adapter — Phase 2 integration)
```

- **Sources** (kinetic generators) declare rated RPM + rated torque + direction.
- **Transmission** (shaft / gear / gearbox / clutch) transforms flow on entry;
  gears mesh by teeth count (`ratio = teethDriver / teethDriven`) and flip direction.
- **Consumers** declare required RPM + required torque; they run OK at or above
  rated RPM, stall (`UNDERPOWERED`) below it.
- **Overload** is per-source: Σ referred demands > rated torque → brownout scale
  `supply / demand` applied to every consumer on that source's network.
- **Topology cache:** one `KineticManager` per `ServerLevel`; networks rebuild only
  on block change (dirty flag) or periodic refresh — never a full re-sim per tick.
- **Snapshots** (`KineticSnapshot`) are the sync unit: server → client packets,
  GUI display, NBT persistence of the last-known state.

## 3. Machines

```
Block (AbstractNexusMachineBlock)
 ├── AbstractOrientedMachineBlock (furnace-style horizontal facing; Crusher/Processor)
 └── BlockEntity (AbstractNexusMachineBlockEntity)
      ├── INexusEnergy storage (Phase 1 API; capacity per machine)
      ├── progress / maxProgress (recipe processing)
      ├── serverTick(): consume energy → advance progress → complete → output
      ├── dirty-flag sync: packet only when energy/progress changed materially
      └── saveAdditional/load: full state
```

Kinetic machines (Phase 3, ADR-009):

```
AbstractKineticMachineBlockEntity extends AbstractNexusMachineBlockEntity
 ├── required RPM / torque (constructor)
 ├── KineticNodeProvider (CONSUMER): KineticManager registration, NodeState snapshots
 ├── inventory (input/output/byproduct) + recipe cache (re-evaluated on input change)
 ├── progress (int) + progressFrac (double): brownout advances fractional ticks
 ├── ProcessingGovernor.speedFactor(): min(rpmRatio, torqueRatio) — proportional brownout
 ├── MachineStatus: RUNNING / IDLE / NO_POWER / BROWNOUT / BLOCKED
 ├── 6-index ContainerData: rpm, torque (mN·m), node status, progress, maxProgress, machine status
 ├── energyCapacity/energyMaxReceive passthrough: Resonator restores its Phase 2
 │   configured storage; Crusher/Processor pass 0/0 (no energy buffer at all)
 └── allowsExternalInsert(): only INPUT_SLOT (output/byproduct extract-only)
```

- **Server authority:** `serverTick` runs only on `ServerLevel`. Client block entities are
  display-only.
- **Sync:** `MachineSyncPacket` (S2C) carries `BlockPos + energy + maxEnergy + progress +
  maxProgress`, sent to tracking clients when the dirty flag trips (throttled: at most
  every 10 ticks, or on >5% energy delta).
- **GUI:** `AbstractContainerMenu` + `Screen`. No game logic in the screen — it renders
  values from the menu, which mirrors the last synced state.
- **States/sound/animation:** hooks exist (`getActivityState()`); content-light in Phase 1.

## 4. Networking

- One `SimpleChannel`: `NexusNetwork.CHANNEL`, protocol version `"1"`.
- Packet ids assigned in `NexusNetwork.register()` — append-only; never reuse an id.
- All packets are versioned; a version mismatch fails fast with a clear message.
- Phase 1 packets: `MachineSyncPacket` (S2C). C2S packets (e.g. GUI buttons) follow
  the same registration pattern.
- Phase 4 packets: `ResearchSyncPacket` (S2C: points + completed set, on login and
  every mutation), `OpenResearchPacket` (C2S: snapshot request),
  `BuyResearchPacket` (C2S: `researchId`, fully revalidated server-side).

## 5. Registries

`NexusRegistries` owns every `DeferredRegister`. Content classes only declare
`RegistryObject`s. Nothing registers outside this file — one place to audit, one place
where mod-id mistakes surface.

Registered in Phase 1: blocks, items, `BlockEntityType`s, `MenuType`s, `RecipeType` +
`RecipeSerializer`.

## 6. Config

`NexusConfig` (ForgeConfigSpec, `nexus_echoes-common.toml`):

- `resonatorCapacity`, `resonatorMaxReceive`, `resonatorEnergyPerTick`,
  `resonatorProcessTime`, `cellTransferRate`.
- **Rule:** no critical numeric literal lives in machine code; everything tunable is in
  config with a comment explaining the unit.

## 7. Data-driven content

- Resonator recipes are JSON (`data/nexus_echoes/recipes/resonating/*.json`) via a custom
  `RecipeType` + `RecipeSerializer`. Adding content = adding JSON, not Java.
- Loot tables, blockstates, models, lang: hand-written JSON in Phase 1
  (datagen providers are a Phase 2 hygiene task).

## 8. Multiplayer rules (non-negotiable)

1. Simulation runs on the server. The client never advances progress or energy.
2. Every GUI-visible value arrives via packet or `ContainerData` — never read the
   client block entity for logic.
3. Energy transfer happens server-side between block entities.
4. Packets carry positions, not block entity references.
5. Research is server-authoritative per player (UUID-keyed `SavedData`):
   the client holds a read-only snapshot; buys are C2S requests the server
   revalidates from scratch.

## 9. Performance rules

- Machine tick throttled: energy I/O every 10 ticks; progress every tick is fine (cheap int math).
- Sync packets throttled (dirty flag + min interval).
- No world scans, no per-tick allocations in hot paths (`BlockPos` reuse where looping).
- Recipe lookup cached per input until inventory changes.

## 10. Integration

`IntegrationManager` centralizes `ModList.get().isLoaded(...)` checks:

```java
if (IntegrationManager.isCreateLoaded()) { /* optional hooks */ }
```

Phase 1: detection helpers only, no hooks yet. JEI/REI display support is a Phase 3 task.

## 11. Testing strategy

- **Unit** (`src/test`, plain JUnit 5, no Minecraft): `NexusEnergyStorage` semantics —
  receive/extract/simulate clamping, NBT round-trip, type compatibility.
- **In-game validation** (checklist, manual for now): place cell + resonator, verify
  processing, GUI values, relog persistence, dedicated-server smoke test.
- Later: game-test framework for multiblocks/dimensions/portals.
- Phase 4: 96 unit tests for the research domain — graph validation
  (duplicates, unknown prerequisites, self-dependencies, cycles), point
  accounting, service rules (sources, prerequisites, costs, atomicity,
  unlocks), NBT persistence (round-trips, defaults, malformed data,
  clone/respawn simulation), multiplayer isolation, definition/codex
  validation (including the shipped JSONs), gating rules, codex visibility
  and a full zero-to-`dimensional_resonance` integration progression.

## 12. Research & progression (Phase 4)

Research is a separate domain, not an energy type. The pure core
(`ResearchDefinition`, `ResearchGraph`, `PlayerResearchState`,
`ResearchService`, `ResearchGating`, codex) knows nothing about Minecraft;
the `mc` adapter bridges it (`ResearchSavedData`, `ResearchManager`,
reload loaders, events, `/nexus research`); networking is three packets on
the existing channel; the client shows read-only screens (keybind **R**).

- Definitions and codex entries are JSON under `data/nexus_echoes/`,
  validated fail-fast on reload (malformed IDs, `cost <= 0`, unknown
  prerequisites, duplicates, cycles).
- Costs and unlocks live in the JSON, not in `NexusConfig` (config is
  operator policy, not progression values).
- Gating is enforced server-side at three points — craft (voids the taken
  stack with an explanation), placement (cancelled), machine `use()`
  (refuses the GUI) — without changing machine block entities.
- `nexus_echoes:hollow_access` is a technology ID only: Phase 5 consumes the
  unlock; no dimension/portal/worldgen code exists.
