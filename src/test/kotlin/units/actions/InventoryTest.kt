package test.units.actions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import project.*
import test.units.*

class InventoryContainerTest {

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
        val item = TestItem(name = "TestItem", interactionDefinitions = listOf(
            InteractionDefinition(mode = TestMode, hook = {null}, validator = { null }),
            InteractionDefinition(mode = OtherMode, hook = {null}, validator = { null })
        ))

        val modes = item.modes
        assertTrue(modes.contains(TestMode))
        assertTrue(modes.contains(OtherMode))
    }

    @Test
    fun `getInteractionDefinitionForMode returns definition only if sourceItem matches`() {
        val item = TestItem(name = "TestItem", interactionDefinitions = listOf(
            InteractionDefinition(mode = TestMode, hook = {null}, validator = { null })
        ))

        val actor = TestActor(grid = TestGrid(), position = Position(0,0))

        // Provide a context with sourceItem matching
        val matchingContext = TestActionContext(TestMode, actor, actor, item)

        val def = item.getInteractionDefinitionForMode(TestMode, matchingContext)
        assertNotNull(def)

        // Provide context with non-matching sourceItem
        val nonMatchingContext = TestActionContext(TestMode, actor, actor, null)
        val def2 = item.getInteractionDefinitionForMode(TestMode, nonMatchingContext)
        assertNull(def2)
    }
}
