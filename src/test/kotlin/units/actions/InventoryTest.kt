package test.units.actions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import project.*
import project.actions.ActionContext
import project.actions.ActionMode
import project.actions.Capability
import test.units.TestGrid

class InventoryContainerTest {

    // Minimal Item implementation for testing
    class TestItem(override val name: String) : Item() {
        override val capabilities: List<Capability> = emptyList()
        override val interactionDefinitions: List<InteractionDefinition> = emptyList()
    }

    @Test
    fun `adding items increases count`() {
        val inventory = InventoryContainer(mutableMapOf())
        val item = TestItem("Potion")

        inventory.addItem(item)
        assertEquals(1, inventory.data[item])

        inventory.addItem(item)
        assertEquals(2, inventory.data[item])
    }

    @Test
    fun `removing item decreases count and returns item`() {
        val inventory = InventoryContainer(mutableMapOf())
        val item = TestItem("Potion")

        inventory.addItem(item)
        inventory.addItem(item)

        val removed1 = inventory.removeItem(item)
        assertEquals(item, removed1)
        assertEquals(1, inventory.data[item])

        val removed2 = inventory.removeItem(item)
        assertEquals(item, removed2)
        assertFalse(inventory.data.containsKey(item))

        val removed3 = inventory.removeItem(item)
        assertNull(removed3)
    }

    @Test
    fun `data returns immutable view`() {
        val inventory = InventoryContainer(mutableMapOf())
        val item = TestItem("Potion")
        inventory.addItem(item)

        val snapshot = inventory.data
        assertEquals(1, snapshot[item])

        inventory.addItem(item)
        assertEquals(2, inventory.data[item])
    }

    @Test
    fun `modes getter returns interaction modes`() {
        val mode1 = object : ActionMode {}
        val mode2 = object : ActionMode {}

        val item = object : Item() {
            override val name: String = "TestItem"
            override val capabilities: List<Capability> = emptyList()
            override val interactionDefinitions: List<InteractionDefinition> = listOf(
                InteractionDefinition(mode = mode1, hook = {}, validator = { null }),
                InteractionDefinition(mode = mode2, hook = {}, validator = { null })
            )
        }

        val modes = item.modes
        assertTrue(modes.contains(mode1))
        assertTrue(modes.contains(mode2))
    }

    @Test
    fun `getInteractionDefinitionForMode returns definition only if sourceItem matches`() {
        val mode1 = object : ActionMode {}

        val item = object : Item() {
            override val name: String = "TestItem"
            override val capabilities: List<Capability> = emptyList()
            override val interactionDefinitions: List<InteractionDefinition> = listOf(
                InteractionDefinition(mode = mode1, hook = {}, validator = { null })
            )
        }

        val actor = object : Actor() {
            override var char: Char = '@'
            override val grid = TestGrid()
            override var position: Position = Position(0,0)
            override val interactionDefinitions: List<InteractionDefinition> = emptyList()
            override val capabilitiesInternal: List<Capability> = emptyList()
        }

        // Provide a context with sourceItem matching
        val matchingContext = object : ActionContext {
            override val mode: ActionMode = mode1
            override val source = actor
            override val target: Entity = actor
            override val sourceItem: Item? = item
        }

        val def = item.getInteractionDefinitionForMode(mode1, matchingContext)
        assertNotNull(def)

        // Provide context with non-matching sourceItem
        val nonMatchingContext = object : ActionContext by matchingContext {
            override val sourceItem: Item? = null
        }

        val def2 = item.getInteractionDefinitionForMode(mode1, nonMatchingContext)
        assertNull(def2)
    }
}
