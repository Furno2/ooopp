package test

import new.InteractionMode
import new.Interaction
import new.Capability
import new.IGrid
import new.InteractionContext
import new.InteractionCommand
import new.Entity
import new.Tile
import new.Position


import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// Simple dummy implementations for testing
object TestMode1 : InteractionMode {
    override fun <C : InteractionMode> handleIncoming(interaction: Interaction.Possible<C>) {}
}

object TestMode2 : InteractionMode {
    override fun <C : InteractionMode> handleIncoming(interaction: Interaction.Possible<C>) {}
}

class TestCommand(): InteractionCommand<TestMode1>{
    override fun execute(context: InteractionContext<TestMode1>) {

    }
}

class TestCapability : Capability<TestMode1> {
    override val mode = TestMode1
    override val source: Any? = null

    override fun createContext(source: Entity, target: Entity, grid: IGrid): InteractionContext<TestMode1> {
        return object : InteractionContext<TestMode1> {
            override val mode = this@TestCapability.mode
            override val source = source
            override val target = target
        }
    }

    override fun validate(context: InteractionContext<TestMode1>): Interaction<TestMode1> {
        return Interaction.Possible(context, {TestCommand()})
    }
}

// Minimal Grid and Position stubs
class Grid : IGrid{
    override fun getEntity(position: new.Position): Entity? {
        TODO("Not yet implemented")
    }

    override fun getTileType(position: new.Position): Tile {
        TODO("Not yet implemented")
    }

    override fun moveEntity(entity: Entity, newPosition: new.Position) {
        TODO("Not yet implemented")
    }
    override fun setEntity(entity: Entity?, position: new.Position): Throwable {
        TODO("Not yet implemented")
    }
}
data class Position(val x: Int, val y: Int)

// Concrete test entity
class TestEntity(
    override val grid: Grid,
    override var position: Position,
    override val interactionModes: List<InteractionMode>,
    override val interactionHooks: Map<InteractionMode, (Interaction.Possible<*>) -> Unit>,
    override var capabilities: List<Capability<out InteractionMode>>
) : Entity(){
    init{
        validateHooks()
    }
}

class EntityTest {

    @Test
    fun `creating a Entity with matching hooks and modes`() {
        // Matching modes and hooks — should pass
        val grid = Grid()
        val entity = TestEntity(
            grid = grid,
            position = Position(0, 0),
            interactionModes = listOf(TestMode1, TestMode2),
            interactionHooks = mapOf(
                TestMode1 to { _ -> },
                TestMode2 to { _ -> }
            ),
            capabilities = emptyList()
        )
        assertNotNull(entity)
    }
    @Test()
    fun `creating a Entity with a missing hook`(){
        // Missing a hook for a mode — should throw
        val grid = Grid()
        assertThrows<IllegalArgumentException> {
            TestEntity(
                grid = grid,
                position = Position(0, 0),
                interactionModes = listOf(TestMode1, TestMode2),
                interactionHooks = mapOf(
                    TestMode1 to { _ -> } // TestMode2 hook missing
                ),
                capabilities = emptyList()
            )
        }
    }
    @Test()
    fun `creating a Entity with a missing mode`(){
        val grid = Grid()
        // Extra hook not in interactionModes — should throw
        assertThrows<IllegalArgumentException> {
            val test = TestEntity(
                grid = grid,
                position = Position(0, 0),
                interactionModes = listOf(TestMode1),
                interactionHooks = mapOf(
                    TestMode1 to { _ -> },
                    TestMode2 to { _ -> } // Extra hook
                ),
                capabilities = emptyList()
            )
        }
    }

    @Test
    fun `getPosition returns correct value`() {
        val grid = Grid()
        val entity = TestEntity(
            grid = grid,
            position = Position(3, 5),
            interactionModes = listOf(TestMode1),
            interactionHooks = mapOf(TestMode1 to { _ -> }),
            capabilities = emptyList()
        )

        val pos = entity.getPosition()
        assertEquals(Position(3, 5), pos)
    }

    @Test
    fun `interactionList starts empty`() {
        val grid = Grid()
        val entity = TestEntity(
            grid = grid,
            position = Position(0, 0),
            interactionModes = listOf(TestMode1),
            interactionHooks = mapOf(TestMode1 to { _ -> }),
            capabilities = emptyList()
        )

        assertTrue(entity.interactionList.actionsByMode.isEmpty())
    }


}
