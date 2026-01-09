package test.units.actions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import project.*
import project.actions.*
import test.units.*

class MovementCapabilityTest {

    @Test
    fun `movement possible when grid allows entering`() {
        val grid = TestGrid(canEnterImpl = {true})
        val moveCap = MovementCapability(TargetType.SELF_ONLY, null)
        val actor = TestActor(grid, Position(0,0), capabilitiesInternal = listOf(moveCap))

        val context = MovementContext(actor, grid, Position(1,1))
        val result = moveCap.validate(context)

        assertTrue(result is Action.Possible)
    }

    @Test
    fun `movement impossible when grid blocks entering`() {
        val grid = TestGrid(canEnterImpl = {false})
        val moveCap = MovementCapability(TargetType.SELF_ONLY, null)
        val actor = TestActor(grid, Position(0,0), capabilitiesInternal = listOf(moveCap))

        val context = MovementContext(actor, grid, Position(1,1))
        val result = moveCap.validate(context)

        assertTrue(result is Action.Impossible)
        assertEquals(MovementFailure, (result as Action.Impossible).reason)
    }

    @Test
    fun `movement always targets self`() {
        val grid = TestGrid()
        val moveCap = MovementCapability(TargetType.SELF_ONLY, null)
        val actor = TestActor(grid, Position(0,0), capabilitiesInternal = listOf(moveCap))
        val interfactionDefinition = InteractionDefinition(
            mode = MovementMode,
            hook = { null },
            validator = { null }
        )
        val targetEntity = TestEntity(grid = grid, interactionDefinitions = listOf())

        val context = MovementContext(actor, grid, Position(1,1))
        assertEquals(actor, context.target)
        val result = moveCap.validate(context)
        assertTrue(result is Action.Possible)
    }
}
