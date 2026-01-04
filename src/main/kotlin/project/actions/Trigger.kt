package project.actions

import project.Actor
import project.Entity
import project.IGrid
import project.Item

object Trigger : ActionMode

class TriggerContext(
    override val source: Actor,
    override val target: Entity,
    val grid: IGrid
) : ActionContext {
    override val mode = Trigger
    override val sourceItem: Item? = null
}

object TriggerFailure : ActionFailure

class TriggerCapability(
    override val sourceItem: Item? = null
) : Capability {

    override val mode: ActionMode = Trigger
    override val targetType = TargetType.OTHERS_ONLY

    override fun validateOutside(context: ActionContext): ActionFailure? {
        val ctx = context as TriggerContext
        // example condition: must be on same tile
        return if (ctx.source.getPosition() == ctx.target.getPosition()) null
        else TriggerFailure
    }
}
