package test.units

import project.*
import project.actions.ActionFailure
import project.actions.ActionMode
import project.actions.ValidatedAction


class EmptyEntity(
    override var position: Position = Position(0, 0),
    override val grid: IGrid = TestGrid(),
    override var char: Char = '?',
    val defs: List<InteractionDefinition> = listOf()
) : Entity() {
    override val interactionDefinitions: List<InteractionDefinition> = defs
}

object TestMode : ActionMode
object OtherMode : ActionMode

object TestActionFailure : ActionFailure

class TestGrid : IGrid {
    override fun setEntity(entity: Entity?, position: Position){}
    override fun getEntity(position: Position) = null
    override fun moveEntity(entity: Entity, newPosition: Position) {}
    override fun getTileType(position: Position) = Tile.Floor
    override fun canEnter(position: Position) = true
}

// Simple dummy AI for Actor
class DummyAI(private val actionToReturn: ValidatedAction) : AiController {
    override fun computeActions(actor: Actor): List<ValidatedAction> = listOf(actionToReturn)
}
