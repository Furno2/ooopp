package test.units.actions
// Helpers to generate ValidatedAction instances for tests
import project.*
import project.actions.*
import test.units.*

/** Melee or ranged attack action */
fun attackAction(
    source: Actor,
    target: Entity,
    grid: IGrid,
    weapon: Weapon? = null,
    damage: Int = 1,
    attackType: AttackType = Melee,
    possible: Boolean = true,
): Action {
    val resolvedDamage = weapon?.damage ?: damage
    val ctx = AttackContext(
        source = source,
        target = target,
        grid = grid,
        item = weapon,
        damage = resolvedDamage,
        attackType = attackType
    )
    return if (possible) Action.Possible(ctx) else Action.Impossible(ctx, TestActionFailure)
}

/** Healing action using an item */
fun healAction(
    source: Actor,
    target: Actor = source,
    item: Item,
    amount: Int = 10,
    possible: Boolean = true
): Action {
    val ctx = HealContext(
        source = source,
        target = target,
        item = item,
        amount = amount
    )
    return if (possible) Action.Possible(ctx) else Action.Impossible(ctx, TestActionFailure)
}

/** Movement action to a target position */
fun movementAction(
    source: Actor,
    grid: IGrid,
    targetPosition: Position,
    possible: Boolean = true
): Action {
    val ctx = MovementContext(
        source = source,
        grid = grid,
        targetPosition = targetPosition
    )
    return if (possible) Action.Possible(ctx) else Action.Impossible(ctx, TestActionFailure)
}

/** Reload action (using a weapon or ammo item) */
fun reloadAction(
    source: Actor,
    target: Actor = source,
    item: Item,
    ammoType: Item,
    amount: Int = 1,
    possible: Boolean = true
): Action {
    val ctx = ReloadContext(
        source = source,
        target = target,
        item = item,
        ammoType = ammoType,
        amount = amount
    )
    return if (possible) Action.Possible(ctx) else Action.Impossible(ctx, TestActionFailure)
}

/** Generic UseItem action wrapping a concrete itemContext */
fun useItemAction(
    source: Actor,
    target: Actor = source,
    sourceItem: Item,
    itemContext: ActionContext,
    requiredSlot: project.EquipmentSlot? = null,
    possible: Boolean = true
): Action {
    val ctx = UseItemContext(
        source = source,
        target = target,
        item = sourceItem,
        itemContext = itemContext,
        requiredSlot = requiredSlot
    )
    return if (possible) Action.Possible(ctx) else Action.Impossible(ctx, TestActionFailure)
}
