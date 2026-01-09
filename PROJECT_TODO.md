Project TODO — Improvements derived from ARCHITECTURE_ASSESSMENT.txt

Status legend: [ ] = open, [x] = done, (P) = priority (H=High, M=Med, L=Low)

Overview
- This file collects prioritized, actionable tasks to make the project cleaner, safer, and ready for UI (Zircon) + visibility features. Each task includes a short description, priority, estimated effort, dependencies, and acceptance criteria.

0) Completed
- [x] Save architecture assessment to ARCHITECTURE_ASSESSMENT.txt (Done) (P: L) — Acceptance: assessment file present at project root.

Immediate (High value, low risk)
1. Fix InventoryContainer.removeBulk correctness (P: H, Est: 0.5h)
   - Change: Correct logic to remove `min(available, countToRemove)` and return actual removed count.
   - Also change `InventoryContainer.data` to return an immutable copy (`_inventory.toMap()`).
   - Tests: Add unit tests for add/remove/removeBulk (cases: 0, less than available, equal available, more than available).
   - Acceptance: Tests pass and no regressions; inventory internal map cannot be mutated by callers.

2. Add unit tests for Inventory behavior (P: H, Est: 1h)
   - Write tests under `src/test/kotlin/project/InventoryContainerTest.kt` covering addItem, removeItem, removeBulk edge cases.
   - Acceptance: Tests green under `mvn -DskipTests=false test` (or equivalent).

3. Remove/annotate empty placeholder files (Pathfinder.kt, FsmController.kt) (P: H, Est: 0.5h)
   - Change: Either delete or add a clear `// TODO: ...` stub explaining purpose.
   - Acceptance: No empty files remain that confuse maintainers.

Short-term (Medium effort)
4. Introduce a `Pathfinder` interface and adapt `HumanController` to accept/inject a pathfinder (P: M, Est: 2-4h)
   - Create `interface Pathfinder { fun findPath(start: Position, goal: Position): List<Position>? }` and an adapter for `GridPathfinder`.
   - Update `HumanController` to accept a `Pathfinder` (constructor param) and use it in `getNextStepTo`.
   - Tests: unit test HumanController path-related behavior with a mock Pathfinder.
   - Acceptance: HumanController no longer checks `grid is Grid`; pathfinding is injectable and testable.

5. Add World facade + EventBus (P: M, Est: 1-2d)
   - Responsibilities: own Grid, manage entities, provide thread-safe APIs for commands (move, add/remove entity, inventory ops), and broadcast events (EntityMoved, EntityAdded, HpChanged, InventoryChanged, EntityRemoved).
   - Use lightweight sealed `GameEvent` and synchronous listener mechanism (optionally add weak references later).
   - Acceptance: UI layer can subscribe to events and query world state; no direct external mutation of internal maps.

6. Make inventories and other internal collections return read-only views (P: M, Est: 1h)
   - Ensure InventoryContainer.data returns a copy or read-only view.
   - Ensure no public fields expose mutable internal collections without a defensive copy.
   - Acceptance: No public API exposes a raw MutableMap or mutable collections.

Medium-term (Medium→Large effort)
7. Implement Field-of-View (FOV) / visibility system (P: M, Est: 1-2d prototype)
   - Add `VisionComponent` or `World.getVisibleEntities(viewer)`.
   - Implement simple raycast FOV; later replace with shadowcasting for performance.
   - Integrate with AttackCapability LOS checks and TargetSelectors.
   - Acceptance: Renderer shows only visible entities for player; LOS checks use same FOV results.

8. Add Zircon rendering and input adapter (P: M, Est: 1-2d)
   - Create `project.ui.ZirconRenderer` and `InputHandler` that translate UI events to world commands or controller inputs.
   - Acceptance: Player can move and open inventory through Zircon input; renderer receives events and updates screen.

9. Make Controller/AI improvements (P: M, Est: 1d)
   - Make `HumanController` perception explicit (add `perceive(view: ActionView)` to update `lastKnownTargetPosition`).
   - Inject configuration for thresholds/delays or use a config object.
   - Acceptance: Behaviors act predictably in unit tests; `lastKnownTargetPosition` is updated from perceived actions.

Long-term / Nice-to-have
10. Extract cross-cutting services (inventory management, ammo reload) from `Human` into smaller service classes (P: L, Est: multiple days)
    - This improves testability and single responsibility.

11. Replace large lambdas in InteractionDefinition with named functions and add KDoc to public APIs (P: L, Est: 0.5-1d)
    - Acceptance: Improved readability and unit testability.

12. Add profiling/heap-check steps for memory leaks (P: L, Est: 1d)
    - Add a small guide in README for using VisualVM / heap dumps to validate no leaks.

Process & owners
- I can implement items (1) and (2) now (Inventory bug + tests). If you want me to, I will apply the change, add tests, and run the test suite.
- If you prefer a different priority ordering, tell me which task to do first.

How to use this TODO
- Each task includes an acceptance criteria; mark `[x]` when done and commit the change with a clear message.
- Link PRs to tasks in the checklist (e.g., PR #12 implements tasks 1–3).

Notes
- Prioritized quick wins: fixing inventory correctness + read-only views, removing placeholder files, and adding World facade are the highest ROI.

Saved file: PROJECT_TODO.md



