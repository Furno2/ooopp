package project.actions

import project.*

object ReloadMode : ActionMode

object ItemCannotBeReloaded : ActionFailure
object ItemNotEquipped : ActionFailure
object InvalidItemMode : ActionFailure
object NoAmmoInInventory : ActionFailure

data class ReloadContext(
    override val source: Actor,
    override val target: Entity,
    override val item: Item,
    val ammoType: Item,
    val amount: Int
) : ActionContext {
    override val mode: ActionMode = ReloadMode
    override var capability: Capability? = null
}

data class ReloadCapability(
    override val sourceItem: Item?,
    val interactionHandler: InteractionHandler? = null,
) : Capability {
    override val targetType = TargetType.SELF_ONLY
    override val mode: ActionMode = ReloadMode

    override fun validateOutside(context: ActionContext): ActionFailure? {
        return if(context.item != sourceItem) {
            return ItemMismatchBetweenContextAndCapability
        }
        else{
            null
        }
    }
}
