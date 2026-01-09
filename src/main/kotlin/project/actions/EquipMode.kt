package project.actions

import project.*

object EquipMode : ActionMode

interface EquipFailure : ActionFailure
object MissingItem : EquipFailure
object MissingItemInSlot : EquipFailure
object AlreadyEquipped : EquipFailure
object InvalidSlot : EquipFailure

data class EquipContext(
    override val source: Actor,
    override val target: Entity,
    override val item: Item?,   // null for UNEQUIP
    val slot: EquipmentSlot,
    val operation: EquipOperation
) : ActionContext {
    override val mode = EquipMode
    override var capability: Capability? = null
}

enum class EquipOperation {
    EQUIP,
    UNEQUIP
}

data class EquipmentCapability(
    override val sourceItem: Item?, // Item to equip, null if UNEQUIP
    override val interactionHandler: InteractionHandler? = null,
) : Capability {
    override val mode: ActionMode = EquipMode
    override val targetType: TargetType = TargetType.SELF_ONLY

    override fun validateOutside(context: ActionContext): EquipFailure? {
        val ctx = context as EquipContext
        return when (ctx.operation) {
            EquipOperation.EQUIP -> {
                val item = ctx.item ?: return MissingItem
                val valid = when (ctx.slot) {
                    EquipmentSlot.WEAPON -> item is Weapon
                    EquipmentSlot.ARMOR -> item is Armor
                    EquipmentSlot.ARTEFACT -> item is Artefact
                }
                if (!valid) InvalidSlot else null
            }
            EquipOperation.UNEQUIP -> {
               null
            }
        }
    }
}
