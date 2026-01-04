package test.units.actions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import project.*
import project.actions.*

class MovementCapabilityTest {

    // Minimal test grid
    class TestGrid(var canEnterResult: Boolean = true) : IGrid {
        override fun setEntity(entity: Entity?, position: Position) {}
        override fun getEntity(position: Position): Entity? = null
        override fun moveEntity(entity: Entity, newPosition: Position) {}
        override fun getTileType(position: Position) = Tile.Floor
        override fun canEnter(position: Position) = canEnterResult
    }

    // Minimal actor
    class TestActor(override val grid: IGrid, override var position: Position, caps: List<Capability>) : Actor() {
        override var char: Char = '@'
        override val interactionDefinitions: List<InteractionDefinition> = emptyList()
        override val capabilitiesInternal: List<Capability> = caps
    }

    @Test
    fun `movement possible when grid allows entering`() {
        val grid = TestGrid(canEnterResult = true)
        val moveCap = MovementCapability(null, TargetType.SELF_ONLY)
        val actor = TestActor(grid, Position(0,0), listOf(moveCap))

        val context = MovementContext(actor, grid, Position(1,1))
        val result = moveCap.validate(context)

        assertTrue(result is ValidatedAction.Possible)
    }

    @Test
    fun `movement impossible when grid blocks entering`() {
        val grid = TestGrid(canEnterResult = false)
        val moveCap = MovementCapability(null, TargetType.SELF_ONLY)
        val actor = TestActor(grid, Position(0,0), listOf(moveCap))

        val context = MovementContext(actor, grid, Position(1,1))
        val result = moveCap.validate(context)

        assertTrue(result is ValidatedAction.Impossible)
        assertEquals(MovementFailure, (result as ValidatedAction.Impossible).reason)
    }

    @Test
    fun `movement always targets self`() {
        val grid = TestGrid()
        val moveCap = MovementCapability(null, TargetType.SELF_ONLY)
        val actor = TestActor(grid, Position(0,0), listOf(moveCap))
        val targetEntity = object : Entity() {
            override val grid: IGrid = grid
            override var position: Position = Position(5,5)
            override var char: Char = 'T'
            override val interactionDefinitions: List<InteractionDefinition> = emptyList()
        }

        val context = MovementContext(actor, grid, Position(1,1))
        assertEquals(actor, context.target)
        val result = moveCap.validate(context)
        assertTrue(result is ValidatedAction.Possible)
    }
}
