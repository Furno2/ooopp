Project Style Guide — Reasonable OOP in Kotlin

Purpose
- A concise, opinionated style guide for writing and reviewing object-oriented Kotlin code in this codebase. Follow these guidelines to keep the model clear, testable, and UI-agnostic.

Quick plan / checklist (use when authoring code)
1. Define a single responsibility for each class. (SRP)
2. Prefer composition (capabilities, services) over subclassing.
3. Keep mutable state private; expose behavior via methods or read-only views.
4. Inject external collaborators (Pathfinder, EventBus) for testability.
5. Add unit tests for any behavior or bugfix.
6. Document public APIs with KDoc and thread-safety assumptions.

Core principles (short)
- Single Responsibility: one reason to change per class.
- Encapsulation: keep internals private; expose controlled APIs.
- Composition: attach behavior via components/capabilities, not inheritance.
- Testability: depend on interfaces and inject collaborators.
- Immutability by default: prefer `val`, immutable types, and defensive copies.

File & naming conventions
- One major type per file. Filename matches the primary class or concept: `InventoryContainer.kt`, `HumanController.kt`.
- Group related small types (mode/context/capability) in a single file: e.g., `AttackMode.kt` contains `AttackMode`, `AttackContext`, `AttackCapability`, and related failures.
- Use clear names: `World`, `Pathfinder`, `GameEvent`, `InventoryContainer`, `InteractionDefinition`.
- Boolean getters: prefix with `is` or `has` (e.g., `isAlive()`, `hasAmmo()`).

Visibility & mutability rules
- Keep mutable internals private. Example:
  - private val _inventory: MutableMap<Item, Int>
  - public val data: Map<Item, Int> get() = _inventory.toMap()
- Avoid public `var` fields for important state. Prefer controlled mutation methods:
  - Use `fun moveTo(newPos: Position)` that calls `grid.moveEntity` and emits an event.
- Use `protected` only when subclasses truly need access.
- For collections, return defensive copies (`toMap()`) or unmodifiable views.

Interfaces vs abstract classes
- Use interfaces for roles and capabilities: `IGrid`, `Controller`, `Capability`, `Pathfinder`.
- Use abstract classes for shared implementation + state: `Entity`, `Actor`, `Item`.
- Prefer composition: items supply capabilities; actors compose capabilities from base and items.

Dependency injection & testability
- Inject collaborators via constructor parameters. Example:
  - `class HumanController(val human: Human, val pathfinder: Pathfinder)`
- Avoid `new`/direct construction in logic when the object is a dependency that should be mocked in tests.
- For singletons/config, prefer provider objects passed in or a simple DI container rather than global mutable state.

Eventing & memory safety
- Use a `World` facade that manages an event bus (sealed `GameEvent`).
- Listener lifecycle: require explicit unsubscribe or use weak references. Always remove listeners when entities are removed.
- Tests should cover that removing an entity unregisters listeners and frees references.

Action / Capability pattern conventions
- Capabilities: stateless descriptors that provide `validateOutside(context)` and `targetSelector`.
- InteractionDefinitions: target-side validators and hooks; `validateInside(context)` must be pure and not change global state.
- Hooks (side-effecting) should be explicit and centralized, not hidden in lambdas that do many things.
- Validation should use domain failure objects (see `ValidatedAction.Impossible`) rather than exceptions.

Pathfinding
- Add an interface: `interface Pathfinder { fun findPath(start: Position, goal: Position): List<Position>? }`.
- Provide an adapter: `class GridPathfinderAdapter(private val grid: Grid) : Pathfinder`.
- Inject into controllers or `World` rather than constructing pathfinders inline.

Inventory & Item rules (repo-specific)
- `InventoryContainer` must:
  - Keep `_inventory` private.
  - `fun removeBulk(item: Item, count: Int): Pair<Item, Int>` must remove `min(available, count)` and return the removed count.
  - `val data: Map<Item, Int> get() = _inventory.toMap()` to avoid exposing the mutable map.
- Avoid exposing `Item` internals; prefer capability-based behavior for using items.

Controllers & AI
- Keep `HumanController` perception updates explicit: add `fun perceive(view: ActionView)` to update `lastKnownTargetPosition` and other sensory state.
- Use injected `Pathfinder` and avoid `is` checks against concrete types.
- FSMs (`gdx-ai`) run where `decide` is called; document threading expectations.

Field-of-View & LOS
- Implement FOV as a utility: `Fov.computeVisible(grid, source, range): Set<Position>`.
- Use FOV results for rendering and for `AttackCapability` LOS checks and `TargetSelector` filtering.
- Maintain a `seen` map on `World` for fog-of-war (tiles seen previously but not currently visible).

Error handling & validation
- Use domain failure objects (e.g., `OutOfAmmo`) for expected invalid states.
- Use exceptions only for programmer errors / invariants (e.g., `require` for preconditions).

Testing expectations
- Add unit tests for any public behavior change or bugfix.
- For AI, test `HumanController` behavior by mocking `PotentialActions` and a `Pathfinder`.
- For eventing, test that events fire and listeners are cleaned up after entity removal.

KDoc & documentation
- Add concise KDoc for public interfaces and classes: purpose, threading, side effects.
- Document invariants on methods that mutate state (e.g., `Grid.setEntity` preconditions).

PR & code review checklist (copy into PR description)
- [ ] Does the code expose mutable internal state? If so, is there a strong justification?
- [ ] Are new dependencies injected or hard-coded? Prefer injection for testability.
- [ ] Are public methods documented with KDoc and thread-safety specified?
- [ ] Are unit tests added for behavioral changes and edge cases?
- [ ] Are listeners/events unregistered when entities are removed?
- [ ] Is composition used (capabilities/services) instead of subclassing?
- [ ] Is there any new global mutable state or singleton? If yes, justify.

Small idiomatic examples
- Immutable inventory view:
  - `val data: Map<Item, Int> get() = _inventory.toMap()`
- Controlled movement API:
  - `fun moveTo(newPos: Position) { val old = position; grid.moveEntity(this, old, newPos); position = newPos; eventBus.emit(EntityMoved(this, old, newPos)); }`
- Capability validation should be pure; hooks perform state changes explicitly.

Acceptance criteria for "reasonably OOP"
- No public mutable internals for core domain objects.
- New features implemented via composition or small services, not class explosion.
- Unit tests cover behavior and lifecycle.
- PR checklist items satisfied before merge.

If you want, I can also:
- Add a short PR template that includes the above checklist.
- Run automated checks to detect obvious rule violations (public mutable fields, exposed MutableMap returns) and produce a report.

Saved to `STYLE_GUIDE.md` in the project root.

