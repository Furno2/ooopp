package test.units

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import project.*
import project.actions.*

class EntityInteractionUnitTest {



    @Test
    fun `no fallback when target is not self`() {
        val item = TestItem(name = "UseHeal", interactionDefinitions = listOf(
            InteractionDefinition(mode = TestMode, hook = { TestHookReturn("ok") }, validator = { null })
        ))

        val human = TestActor()
        val other = TestEntity()
        val ctx = TestActionContext(TestMode, human, other, item)

        val def = human.getInteractionDefinitionForMode(TestMode, ctx)
        assertNull(def)
    }

    @Test
    fun `item selection by equality when multiple items`() {
        val item1 = TestItem(name = "i1", interactionDefinitions = listOf(
            InteractionDefinition(mode = TestMode, hook = { TestHookReturn(1) }, validator = { null })
        ))
        val item2 = TestItem(name = "i2", interactionDefinitions = listOf(
            InteractionDefinition(mode = TestMode, hook = { TestHookReturn(2) }, validator = { null })
        ))

        val inv = InventoryContainer(mutableMapOf(item1 to 1, item2 to 1))
        val human = Human(position = Position(0,0), grid = TestGrid(), initialInventory = inv.data.map { it.key to it.value })

        val ctx = UseItemContext(human, human, item2, object : ActionContext {
            override val mode = TestMode
            override val source = human
            override val target = human
            override val item = item2
        })
        val def = human.getInteractionDefinitionForMode(UseItemMode, ctx)
        assertNotNull(def)
        val ret = def!!.invokeHook(Action.Possible(ctx))
        assertTrue(ret is TestHookReturn)
        assertEquals(2, (ret as TestHookReturn).data)
    }
}
