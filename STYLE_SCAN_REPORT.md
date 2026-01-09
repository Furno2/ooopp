# Style Scan Report

Purpose
- This report lists locations in the project that don't follow the rules in `STYLE_GUIDE.md` and gives suggested fixes and priorities.

Scan plan (what I did)
- Ran targeted searches across `src/**/*.kt` for public `var`, exposed mutable collections, getters returning backing mutable fields, missing KDoc, boolean naming, direct construction of collaborators, throw usage in validation, listener subscribe/unsubscribe, and large validator/hook lambdas.
- Inspected the matched files to triage true positives and gather line ranges and snippets.

Checklist
- [x] Identify public mutable fields / constructor `var`
- [x] Identify exposed mutable collection types
- [x] Detect getters that return mutable backing fields without defensive copy
- [x] Find public APIs missing KDoc
- [x] Check boolean getter naming
- [x] Locate direct constructions of collaborators inside logic
- [x] Find exception usage for domain validation
- [x] Flag large side-effecting lambdas in validators/hooks
- [x] Produce prioritized actionable items

Report format
- Each finding includes: file path, approximate line range, code snippet (short), violated rule, why it's a problem, suggested fix, and severity.

---

## High-priority (must-fix)

1) InventoryContainer.removeBulk: incorrect subtraction (functional bug + style)
- File: `src/main/kotlin/project/Item.kt`
- Lines: ~70-87 (method `removeBulk` in `InventoryContainer`)
- Snippet: `_inventory[item] = _inventory[item] as Int - count`
- Rule violated: Inventory rule in `STYLE_GUIDE.md` — `removeBulk` must remove `min(available, count)` and return removed amount.
- Why: The else branch subtracts the current `count` from itself instead of subtracting the requested `countToRemove`, likely setting the entry to zero incorrectly and returning the wrong removed amount. This is a correctness bug and deviates from the documented behavior.
- Suggested fix: Replace the logic with a clear implementation using `val removed = min(count, countToRemove)` then set `_inventory[item] = count - removed` (or remove the entry when resulting count is 0) and return `Pair(item, removed)`.
- Severity: Critical (functional correctness). Add a unit test for `removeBulk` covering both partial and full removals.

---

## High / Medium priority (style & safety)

2) Public mutable `var` for entity position and display char
- Files: `src/main/kotlin/project/Entity.kt` (class `Human` and other `Entity` subclasses: `ItemEntity`, `Chest`, `Corpse`, `Trap`)
- Lines: constructor parameter overrides like `override var position: Position` and `override var char: Char`
- Snippet: `class Human(... override var position: Position, ... override var char: Char = '@')`
- Rule violated: "Avoid public `var` fields for important state. Prefer controlled mutation methods." (Visibility & mutability rules)
- Why: `position` is core domain state; exposing it as a public mutable var defeats encapsulation and makes it easy for callers to break invariants (grid consistency, events, listeners). The style guide suggests `fun moveTo(newPos: Position)` that calls `grid.moveEntity` and emits events.
- Suggested fix: Make the setter non-public (e.g., `private set` or `protected set`) and add a `moveTo(newPos: Position)` method on `Entity`/`Actor` that performs the `grid.moveEntity(this, oldPos, newPos)` and updates `position`. Update callers/tests accordingly. Document threading assumptions in KDoc.
- Severity: Medium-high (design/correctness & testability). This is a breaking API change for external callers, so do it in a focused PR with tests.

3) Public constructor `var` on `SimpleWeapon.ammoCount`
- File: `src/main/kotlin/project/Items.kt`
- Lines: class `SimpleWeapon` primary constructor
- Snippet: `var ammoCount: Int = if (ammoType == null) Int.MAX_VALUE else 0,`
- Rule violated: "Avoid public `var` fields for important state" and "Keep mutable internals private"
- Why: ammoCount should be controlled by methods like `consumeAmmo`, `acceptReload`, etc. Public `var` lets callers arbitrarily change ammo and breaks invariants.
- Suggested fix: Make ammo count internal/private (`private var _ammoCount`) and expose a read-only `val ammoCount: Int get() = _ammoCount` and update internal methods to mutate `_ammoCount`. Update tests/usages as needed.
- Severity: Medium (API/encapsulation).

4) Inline construction of controller fallback in `Human` constructor
- File: `src/main/kotlin/project/Entity.kt`
- Line: Human constructor initialization of `controller`
- Snippet: `override val controller: Controller = controller ?: project.ai.HumanController(this, grid)`
- Rule violated: Dependency injection & testability (prefer injected collaborators)
- Why: Constructing `HumanController` inline reduces testability and violates the DI guidance. It's acceptable as a test convenience, but the style guide favors injection or factories/providers at composition root.
- Suggested fix: Require controller injection (pass a default from the app wiring), or use a provider/factory object passed into the application bootstrap. Alternatively, keep the fallback but document it in KDoc and ensure tests always pass mocks explicitly.
- Severity: Low-to-medium (testability). Not strictly urgent; consider as part of a refactor.

---

## Medium / Low priority (style suggestions, docs, and refactors)

5) Missing KDoc on public APIs
- Files: many public classes in `src/main/kotlin` (examples: `GridPathfinder.kt`, `Grid2.kt` types `Grid`, `IGrid`, `Position`, `Entity`, `Item`, `Items`, etc.)
- Rule violated: KDoc & documentation requirement for public APIs
- Why: The STYLE_GUIDE requires KDoc for public interfaces/classes and to document threading/invariant assumptions.
- Suggested fix: Add concise KDoc blocks to public classes/interfaces and public methods (one-line summary + threading note and side-effects). Start with core domain types (`Entity`, `Actor`, `IGrid`, `Grid`, `InventoryContainer`, `Item`).
- Severity: Low (documentation), but important for maintainability.

6) Large hook lambdas (side-effects) inside `InteractionDefinition` declarations
- File: `src/main/kotlin/project/Entity.kt`, `src/main/kotlin/project/Items.kt`
- Snippet: multi-line `hook = { action -> ... }` bodies that mutate state (`hp` changes, equipment mutation, inventory changes, grid.moveEntity`, applyEffects etc.)
- Rule violated: Hooks should be explicit and centralized; validators must be pure. Large lambdas are harder to read and test.
- Why: While hooks are allowed to perform side-effects, large anonymous lambdas can hide complex logic and make testing harder.
- Suggested fix: Move large hook bodies into named functions with KDoc (e.g., `private fun handleAttackHook(action: ValidatedAction.Possible): HookReturnValue?`) so the behavior is easier to read and test. Keep validators pure.
- Severity: Low-to-medium (readability/testability).

7) Redundant defensive copy: `inventoryInternal.data.toMap()`
- File: `src/main/kotlin/project/Entity.kt`
- Snippet: `val inventory: Map<Item, Int> get() = inventoryInternal.data.toMap()` (note `data` already returns a `toMap()`)
- Rule: Defensive copies are good, but double-copy costs allocation.
- Suggested fix: Remove the extra `.toMap()` and depend on `inventoryInternal.data` (already a copy), or keep it if explicit immutability required.
- Severity: Low (performance/clarity).

---

## Noted acceptable patterns (no action required)
- `InventoryContainer.data` returns `_inventory.toMap()` — adheres to defensive copy guidance.
- `require(...)` usage in `Grid.setEntity` / `moveEntity` — acceptable for invariants (exceptions for programmer errors).
- Boolean method names are generally compliant (`isAlive`, `hasAmmo`, `canAttack`, `isAdjacent`).
- Private mutable collections used internally (e.g., `GridPathfinder` A* implementation) — acceptable.

---

## Suggested next steps (pick one)
- Option A (recommended): Fix the `removeBulk` bug now and add unit tests. This change is small, low-risk, and fixes functional correctness. I can patch `src/main/kotlin/project/Item.kt` and run the test suite.
- Option B: Produce a tidy Markdown/CSV report (this file) and open one PR per change: (1) `removeBulk` bugfix + tests, (2) make `ammoCount` private + tests, (3) encapsulate `position` with `moveTo()` (bigger refactor), (4) add KDoc stubs.
- Option C: I can auto-generate KDoc stubs for the most important public types in separate commits (non-invasive) so reviewers can fill details.

If you want me to apply changes, tell me which option to proceed with. My recommendation: apply the `removeBulk` fix and run tests first.

---

## Requirements coverage
- Checked rules: public var, constructor var, exposed mutable collections, defensive getters, KDoc presence, boolean-naming, DI/direct construction, exceptions in domain validation, hook complexity.
- Status: issues discovered as listed above. I did not find event listener subscribe/unsubscribe patterns in production sources.

---

Generated by style-scan; saved as `STYLE_SCAN_REPORT.md` at the project root.

