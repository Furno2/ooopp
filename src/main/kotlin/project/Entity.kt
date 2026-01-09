package project

import project.actions.*


data class InteractionHandler(
    val hook: (Action.Possible) -> Unit,
    val validator: (ActionContext) -> ActionFailure?,
){
    fun invokeHook(action: Action.Possible): Unit {
        return hook(action)
    }
    fun validateInside(context: ActionContext): ActionFailure? {
        return validator(context)
    }
}

data class InteractionDefinition(
    val mode: ActionMode,
    private val handler: InteractionHandler,
) {
    // Backwards-compatible constructor so existing call sites (mode=..., hook=..., validator=...)
    // continue to work: it wraps hook/validator into InteractionHandler.
    constructor(
        mode: ActionMode,
        hook: (Action.Possible) -> Unit,
        validator: (ActionContext) -> ActionFailure?
    ) : this(mode, InteractionHandler(hook, validator))

    fun invokeHook(action: Action.Possible): Unit {
        return handler.invokeHook(action)
    }
    fun validateInside(context: ActionContext): ActionFailure? {
        return handler.validateInside(context)
    }
}



interface Controller {
    fun decide(actor: Actor, actions: PotentialActions): Action?
}

// Default no-op controller that never selects an action.
object NoopController : Controller {
    override fun decide(actor: Actor, actions: PotentialActions): Action? = null
}

abstract class Entity{
    protected abstract val grid: IGrid
    protected abstract var position: Position
    protected abstract var char : Char

    protected abstract val interactionDefinitions: List<InteractionDefinition>

    @JvmName("getPositionPublic")
    fun getPosition() = position
    override fun toString() = char.toString()
    abstract fun isAlive(): Boolean

    val modes: List<ActionMode>
        get() = interactionDefinitions.map{it.mode}.distinct().toList()

    fun getInteractionDefinitionForMode(mode: ActionMode): InteractionDefinition? {
        return interactionDefinitions.firstOrNull { it.mode === mode }
    }

    fun invokeHook(mode: ActionMode, action: Action.Possible){
        getInteractionDefinitionForMode(mode)?.invokeHook(action)
    }

    fun validateInside(context: ActionContext): ActionFailure? {
        return getInteractionDefinitionForMode(context.mode)?.validateInside(context)
    }
}

abstract class Actor: Entity() {
    protected abstract val capabilitiesInternal: List<Capability>

    open val capabilities: List<Capability>
        get() = capabilitiesInternal.toList()

    open fun doesContainCapability(cap: Capability): Boolean {
        return capabilitiesInternal.any {
            it == cap
        }
    }

    protected val interactionListInternal: PotentialActions = PotentialActions()

    abstract val controller: Controller
}

class Human(
    override val grid: IGrid,
    override var position: Position,
    override var char: Char = '@',
    val maxHp: Int = 100,
    initialInventory: List<Pair<Item, Int>> = listOf(),
    controller: Controller? = null
) : Actor() {

    private var hpInternal: Int = maxHp

    val hp get() = hpInternal

    override fun isAlive(): Boolean = hp > 0

    override val controller: Controller = controller ?: project.ai.HumanController(this, grid)

    // Internal inventory
    private val inventoryInternal: InventoryContainer =
        InventoryContainer(initialInventory.associate { it }.toMutableMap())

    val inventory: Map<Item, Int> get() = inventoryInternal.data.toMap()

    // Equipment slots
    private val equipmentSlots: MutableMap<EquipmentSlot, Item?> = mutableMapOf(
        EquipmentSlot.WEAPON to null,
        EquipmentSlot.ARMOR to null,
    )

    // --- InteractionDefinition helpers for self-targeting actions ---

    private fun movementHook(action: Action.Possible): Unit {
        val ctx = action.context as MovementContext
        val oldPosition = position
        grid.moveEntity(this, oldPosition, ctx.targetPosition)
        position = ctx.targetPosition
    }
    private fun movementValidator(context: ActionContext): ActionFailure? = null

    private fun equipHook(action: Action.Possible): Unit {
        val ctx = action.context as EquipContext
        if (ctx.operation == EquipOperation.EQUIP) {
            if (ctx.item != null) {
                equipmentSlots[ctx.slot] = ctx.item
                inventoryInternal.removeItem(ctx.item)
            }
        } else {
            val item = equipmentSlots[ctx.slot]
            if (item != null) inventoryInternal.addItem(item)
            equipmentSlots[ctx.slot] = null
        }
    }
    private fun equipValidator(context: ActionContext): ActionFailure? {
        val ctx = context as EquipContext
        return when (ctx.operation) {
            EquipOperation.EQUIP ->
                if (equipmentSlots[ctx.slot] != null) AlreadyEquipped
                else if(ctx.item == null) MissingItem
                else null
            EquipOperation.UNEQUIP -> if (equipmentSlots[ctx.slot] == null) MissingItemInSlot
            else if (ctx.item != null) InvalidSlot
            else null
        }
    }

    private fun attackHook(action: Action.Possible): Unit {
        val ctx = action.context as AttackContext
        val attacker = ctx.source
        val attackerWeapon = (ctx.item as? Weapon) ?: (attacker as? Human)?.getEquippedItem(EquipmentSlot.WEAPON) as? Weapon
        val baseDamage = attackerWeapon?.damage ?: ctx.damage
        val targetArmor = (this as? Human)?.getEquippedItem(EquipmentSlot.ARMOR) as? Armor
        val protection = targetArmor?.protection ?: 0
        val finalDamage = (baseDamage - protection).coerceAtLeast(0)
        if (this is Human) {
            this.hpInternal -= finalDamage
        }
        if (ctx.attackType is Ranged) {
            attackerWeapon?.consumeAmmo()
        }
    }
    private fun attackValidator(context: ActionContext): ActionFailure? {
        val ctx = context as AttackContext
        if (ctx.attackType is Ranged) {
            val attacker = ctx.source
            val attackerWeapon = if (ctx.item == (attacker as? Human)?.getEquippedItem(EquipmentSlot.WEAPON) as? Weapon) ctx.item else null
            if (attackerWeapon != null && !attackerWeapon.hasAmmo()) return OutOfAmmo
        }
        return null
    }

    private fun inventoryHook(action: Action.Possible): Unit {
        val ctx = action.context as InventoryContext
        ctx.action.operation(inventoryInternal, ctx.item)
    }
    private fun inventoryValidator(context: ActionContext): ActionFailure? {
        val ctx = context as InventoryContext
        return if (ctx.source !== this) TooFar else null
    }


    // --- End helpers ---

    // Only self-targeting actions are defined here; item-based actions are delegated to the item
    override val interactionDefinitions: List<InteractionDefinition> = listOf(
        InteractionDefinition(
            mode = AttackMode,
            hook = this::attackHook,
            validator = this::attackValidator
        ),
        InteractionDefinition(
            mode = EquipMode,
            hook = this::equipHook,
            validator = this::equipValidator
        ),
        InteractionDefinition(
            mode = InventoryMode,
            hook = this::inventoryHook,
            validator = this::inventoryValidator
        ),
    )


    // Helpers to inspect equipped items
    fun isItemEquippedInSlot(item: Item, slot: EquipmentSlot): Boolean {
        return equipmentSlots[slot] === item
    }
    fun getEquippedItem(slot: EquipmentSlot): Item? = equipmentSlots[slot]

    // Remove ammo from inventory for reloading
    fun removeAmmoFromInventory(ammoType: Item, requested: Int): Int {
        val pair = inventoryInternal.removeBulk(ammoType, requested)
        return pair.second
    }

    private fun applyEffects(effects: List<HumanEffect>) {
        effects.forEach { effect ->
            when (effect) {
                is Heal -> hpInternal += effect.amount.coerceAtMost(hpInternal+effect.amount)
                is ReloadRequest -> {
                    val removed = removeAmmoFromInventory(effect.ammoType, effect.requested)
                    if (removed > 0) {
                        effect.weapon.acceptReload(removed)
                    }
                }
            }
        }
    }

    // Capabilities: only self-targeting and container actions are defined here; item-based come from items
    override val capabilitiesInternal: List<Capability>
        get() = listOf(
            MovementCapability(TargetType.SELF_ONLY,
                InteractionHandler(
                hook = this::movementHook,
                validator = this::movementValidator)
                ),
            InventoryCapability(),
            PickUpCapability(),
        ) + inventory.keys.flatMap { it.capabilities }
}

class ItemEntity(
    override val grid: IGrid,
    override var position: Position,
    val item: Item
) : Entity() {
    override fun isAlive(): Boolean = alive
    private var alive = true
    // Interaction for picking up
    override val interactionDefinitions: List<InteractionDefinition> = listOf(
        InteractionDefinition(
            mode = PickUpMode,
            validator = { context ->
                if (!alive) EntityDoesNotExist else null
            },
            hook = { action ->
                val ctx = action.context as PickUpContext
                ctx.targetInventory.addItem(item)
                alive = false
                null
            }
        )
    )

    override var char: Char = 'i'
}

class Chest(
    override val grid: IGrid,
    override var position: Position,
    val inventory: InventoryContainer = InventoryContainer(mutableMapOf())
) : Entity() {
    override fun isAlive(): Boolean = true

    override var char: Char = 'C'

    override val interactionDefinitions: List<InteractionDefinition> = listOf(
        InteractionDefinition(
            mode = InventoryMode,
            validator = { null },
            hook = { action ->
                val ctx = action.context as InventoryContext
                // Execute the operation requested by context
                ctx.action.operation(inventory, ctx.item)
                null
            }
        )
    )
}
// Corpse: behaves like a Chest (provides an inventory) but represents a dead body.
// Code intentionally mirrors Chest to keep behavior identical; icon and isAlive differ.
class Corpse(
    override val grid: IGrid,
    override var position: Position,
    val inventory: InventoryContainer = InventoryContainer(mutableMapOf())
) : Entity() {
    override fun isAlive(): Boolean = false

    // Distinct icon for corpse — lowercase 'c'
    override var char: Char = 'c'

    override val interactionDefinitions: List<InteractionDefinition> = listOf(
        InteractionDefinition(
            mode = InventoryMode,
            validator = { null },
            hook = { action ->
                val ctx = action.context as InventoryContext
                // Execute the operation requested by context
                ctx.action.operation(inventory, ctx.item)
                null
            }
        )
    )
}

class Trap(
    override val grid: IGrid,
    override var position: Position,
    private val damage: Int
) : Actor() {
    override fun isAlive(): Boolean = true
    override var char: Char = '^'

    fun getDamage(): Int = damage

    // Trap doesn’t have other interaction definitions
    override val interactionDefinitions: List<InteractionDefinition> = emptyList()

    // Capabilities include a single AttackCapability with Other attack type
    private val attackCapability = AttackCapability(
        weapon = null,
        attackType = Other,
        damage = damage
    )

    override val capabilitiesInternal: List<Capability>
        get() = listOf(attackCapability)

    override val controller: Controller = NoopController
}
