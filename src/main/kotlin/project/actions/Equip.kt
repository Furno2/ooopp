package project.actions

import project.*

object Equip : ActionMode

enum class EquipOperation {
    EQUIP,
    UNEQUIP
}

class EquipContext(
    override val source: Actor,
    override val target: Entity,
    override val sourceItem: Item?,   // null for UNEQUIP
    val slot: EquipmentSlot,
    val operation: EquipOperation
) : ActionContext {
    override val mode: ActionMode = Equip
}

interface EquipFailure : ActionFailure
object MissingItem : EquipFailure
object MissingItemInSlot : EquipFailure
object AlreadyEquipped : EquipFailure
object InvalidSlot : EquipFailure

class EquipmentCapability(
    override val sourceItem: Item? // Item to equip, null if UNEQUIP
) : Capability {
    override val mode: ActionMode = Equip
    override val targetType: TargetType = TargetType.SELF_ONLY

    override fun validateOutside(context: ActionContext): ActionFailure? {
        val ctx = context as EquipContext
        return when (ctx.operation) {
            EquipOperation.EQUIP -> {
                val item = ctx.sourceItem ?: return MissingItem
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
