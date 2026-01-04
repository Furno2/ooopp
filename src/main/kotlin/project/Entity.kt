package project

import project.actions.*
import kotlin.contracts.Effect

internal interface InteractionDefinitionProvider {
    fun getInteractionDefinitionForMode(mode: ActionMode, contextIfNeeded: ActionContext? = null): InteractionDefinition?
}

data class InteractionDefinition(
    val mode: ActionMode,
    private val hook: (ValidatedAction.Possible) -> HookReturnValue,
    private val validator: (ActionContext) -> ActionFailure?,
)
{
    fun invokeHook(action: ValidatedAction.Possible): HookReturnValue {
        return hook.invoke(action)
    }
    fun validateInside(context: ActionContext): ActionFailure? {
        return validator.invoke(context)
    }
}

interface AiController {
    fun computeActions(actor: Actor): List<ValidatedAction>
}

abstract class Entity : InteractionDefinitionProvider {
    protected abstract val grid: IGrid
    protected abstract var position: Position
    protected abstract var char : Char

    @JvmName("getPositionPublic")
    fun getPosition() = position
    override fun toString() = char.toString()
    abstract fun isAlive(): Boolean

    // Protected internals
    protected abstract val interactionDefinitions: List<InteractionDefinition>

    // Public, read-only views / APIs
    val modes: List<ActionMode>
        get() = interactionDefinitions.map{it.mode}.toList().distinct()

    override fun getInteractionDefinitionForMode(mode: ActionMode, contextIfNeeded: ActionContext?): InteractionDefinition? {
        return interactionDefinitions.firstOrNull { it.mode === mode }
    }

    // Base Entity only consults its own hooks; subclasses may fall back to other sources (e.g., items)
    fun invokeHook(mode: ActionMode, action: ValidatedAction.Possible): Any? {
        return getInteractionDefinitionForMode(mode)?.invokeHook(action)
    }

    // Validation is kept behind a function
    fun validateInside(context: ActionContext): ActionFailure? {
        return getInteractionDefinitionForMode(context.mode)?.validateInside(context)
    }
}

abstract class Actor: Entity(){
    protected abstract val capabilitiesInternal: List<Capability>

    open val capabilities: List<Capability>
        get() = capabilitiesInternal.toList()

    open fun doesContainCapability(cap: Capability): Boolean {
        return capabilities.any {
            cap.equalsCapability(it)
        }
    }

    private val interactionListInternal: PotentialActions = PotentialActions()

    fun addInteraction(action: ValidatedAction) {
        interactionListInternal.addAction(action)
    }

    fun getInteractionsForMode(mode: ActionMode): List<ValidatedAction> = interactionListInternal.getActionsForMode(mode)

    fun getAllInteractions(): Map<ActionMode, List<ValidatedAction>> = interactionListInternal.getAllActions()

    fun clearInteractions() = interactionListInternal.clear()

}

class iHuman(
    override var position: Position,
    override val grid: IGrid,
    initialInventory: List<Pair<Item, Int>> = emptyList()
) : Actor() {

    override var char: Char = '@'
    private val maxHp = 100
    var hp: Int = maxHp
        private set

    override fun isAlive(): Boolean = hp > 0

    // Internal inventory
    private val inventoryInternal: InventoryContainer =
        InventoryContainer(initialInventory.associate { it.first to it.second }.toMutableMap())

    val inventory: Map<Item, Int> get() = inventoryInternal.data.toMap()

    // Equipment slots
    private val equipmentSlots: MutableMap<EquipmentSlot, Item?> = mutableMapOf(
        EquipmentSlot.WEAPON to null,
        EquipmentSlot.ARMOR to null,
        EquipmentSlot.ARTEFACT to null
    )

    // InteractionDefinitions
    override val interactionDefinitions: List<InteractionDefinition> = listOf(
        // Attack: reduce target HP by damage
        InteractionDefinition(
            mode = Attack,
            hook = { action ->
                val ctx = action.context as AttackContext
                val damage = ctx.damage
                    hp -= damage
                NoReturnValue
            },
            validator = { _ -> null } // no validation
        ),

        // Equip/Unequip
        InteractionDefinition(
            mode = Equip,
            hook = { action ->
                val ctx = action.context as EquipContext
                if (ctx.operation == EquipOperation.EQUIP) {
                    equipmentSlots[ctx.slot] = ctx.sourceItem
                    inventoryInternal.removeItem(equipmentSlots[ctx.slot] as Item)
                } else {
                    inventoryInternal.addItem(equipmentSlots[ctx.slot] as Item)
                    equipmentSlots[ctx.slot] = null
                }
            },
            validator = { context ->
                val ctx = context as EquipContext
                when (ctx.operation) {
                    EquipOperation.EQUIP -> if (equipmentSlots[ctx.slot] != null) AlreadyEquipped else null
                    EquipOperation.UNEQUIP -> if (equipmentSlots[ctx.slot] == null) MissingItemInSlot else null
                }
            }
        ),

        // Movement
        InteractionDefinition(
            mode = Movement,
            hook = { action ->
                val ctx = action.context as MovementContext
                val oldPosition = position
                grid.moveEntity(this, oldPosition,ctx.targetPosition)
                position = ctx.targetPosition
                NoReturnValue
            },
            validator = { _ -> null } // no validation
        ),

        // UseItem
        InteractionDefinition(
            mode = UseItem,
            hook = { action ->
                val ctx = action.context as UseItemContext
                ctx.sourceItem.getInteractionDefinitionForMode(ctx.mode, ctx)?.invokeHook(
                    action
                )
                NoReturnValue
            },
            validator = { context ->
                val ctx = context as UseItemContext
                if (inventoryInternal.data.containsKey(ctx.sourceItem)) null
                else ItemNotPresentInContainer
            }
        ),

        // Inventory
        InteractionDefinition(
            mode = Inventory,
            hook = { action ->
                val ctx = action.context as InventoryContext
                ctx.action.operation(inventoryInternal, ctx.sourceItem)
            },
            validator = { context ->
                val ctx = context as InventoryContext
                if (ctx.source !== this) TooFar else null
            }
        )
    )

    private fun applyEffects(effects: List<HumanEffect>) {
        effects.forEach { effect ->
            when (effect) {
                is Heal -> hp += effect.amount

                // is Eat -> food += effect.nutrition

                is Reload -> {
                    inventoryInternal.removeBulk(effect.ammoType, effect.amount)
                }
            }
        }
    }

    // Capabilities include default actions plus inventory items
    override val capabilitiesInternal: List<Capability>
        get() = listOf(
            MovementCapability(null, TargetType.SELF_ONLY),
            AttackCapability(null, Melee),
            InventoryCapability(TargetType.SELF_ONLY)
        ) + inventory.keys.flatMap { it.capabilities }

    val itemAllowedModes: List<ActionMode>
        get() = inventory.keys.flatMap { it.modes }.distinct()

    // Accessors for equipment (optional)
    fun getEquipped(slot: EquipmentSlot): Item? = equipmentSlots[slot]
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
            mode = PickUp,
            validator = { context ->
                // Fail if the item is no longer alive
                if (!alive) EntityDoesNotExist else null
            },
            hook = { action ->
                val ctx = action.context as PickUpContext
                // Transfer the item to the target inventory
                ctx.targetInventory.addItem(item)
                // Mark this entity as no longer alive
                alive = false
                NoReturnValue
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
            mode = Inventory,
            validator = { null },
            hook = { action ->
                val ctx = action.context as InventoryContext
                // Execute the operation requested by context
                ctx.action.operation(inventory, ctx.sourceItem)
                NoReturnValue
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
        sourceItem = null,
        attackType = Other
    )

    override val capabilitiesInternal: List<Capability>
        get() = listOf(attackCapability)
}
