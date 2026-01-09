package project.actions

import project.Actor
import project.Entity
import project.InteractionHandler
import project.Item

object HealMode : ActionMode

// Heal-specific failures
object ActorMaxHp : ActionFailure
object HealOnNonHuman : ActionFailure

data class HealContext(
    override val source: Actor,
    override val target: Entity,
    override val item: Item,
    val amount: Int
) : ActionContext {
    override val mode: ActionMode = HealMode
    override var capability: Capability? = null
}

data class HealCapability(
    override val sourceItem: Item?,
    override val interactionHandler: InteractionHandler? = null,
) : Capability {
    override val mode: ActionMode = HealMode
    override val targetType: TargetType = TargetType.SELF_ONLY

    override fun validateOutside(context: ActionContext): ActionFailure? {
        return if(context.item != sourceItem) {
            ItemMismatchBetweenContextAndCapability
        }
        else{
            null
        }
    }
}
