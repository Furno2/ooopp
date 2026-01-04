package project.actions

import project.Actor
import project.Entity
import project.IGrid
import project.Item
import project.Position

object MovementFailure: ActionFailure

object Movement: ActionMode

class MovementCapability(
    override val sourceItem: Item?,
    override val targetType: TargetType
) : Capability
{
    private val _mode: ActionMode = Movement
    override val mode: ActionMode get() = _mode
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

class MovementContext(
    override val source: Actor,
    val grid: IGrid,
    val targetPosition: Position
) : ActionContext
{
    override val mode = Movement
    override val target: Entity = source
    override val sourceItem: Item?
        get() =null
}
