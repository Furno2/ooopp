package project.actions

import project.Actor
import project.Entity
import project.IGrid
import project.InteractionHandler
import project.Item
import project.Position

object MovementMode: ActionMode

object MovementFailure: ActionFailure

data class MovementContext(
    override val source: Actor,
    val grid: IGrid,
    val targetPosition: Position
) : ActionContext
{
    override val mode = MovementMode
    override val target: Entity = source
    override val item = null
    override var capability: Capability? = null
}

data class MovementCapability(
    override val targetType: TargetType,
    override val interactionHandler: InteractionHandler? = null,
) : Capability
{
    override val mode = MovementMode
    override val sourceItem = null
    override val targetSelector: TargetSelector = SelfTargetSelector()
    override fun validateOutside(context: ActionContext): ActionFailure? {
        val castedContext = context as MovementContext
        return if(castedContext.grid.canEnter(castedContext.targetPosition)){
            null
        }
        else{
            MovementFailure
        }
    }
}
