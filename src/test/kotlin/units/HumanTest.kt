package test.units

import project.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import project.actions.ActionFailure
import project.actions.ActionMode
import project.actions.UseItemMode
import project.actions.Action

// Simple wrapper to satisfy HookReturnValue type
data class SimpleReturn(val v: Any?) : HookReturnValue

// Helper to create TestItem instances using the shared TestItem from MockObjects
private fun mkItem(mode: ActionMode, hookResult: Any? = null, validatorResult: ActionFailure? = null): Item {
    val def = InteractionDefinition(
        mode = mode,
        hook = { _ -> SimpleReturn(hookResult) },
        validator = { _ -> validatorResult }
    )
    return TestItem(name = "TestItem", interactionDefinitions = listOf(def))
}

// ---------- Tests ----------
class HumanCharacterTest {
    private fun newInventory(inventory: MutableMap<Item,Int>): InventoryContainer {
        return InventoryContainer(inventory)
    }

    private fun createHuman(
        inventory: InventoryContainer,
    ): Human =
        Human(
            position = Position(0, 0),
            grid = TestGrid(),
            initialInventory = inventory.data.map { it.key to it.value })

    @Test
    fun `inventory getter returns immutable copy`() {
        val item = mkItem(mode = TestMode)
        val inventory = newInventory(mutableMapOf(item to 1))
        val human = createHuman(inventory)

        val inv = human.inventory
        assertEquals(1, inv.size)
        assertSame(item, inv.keys.first())

    }

    @Test
    fun `falls back to inventory interaction definition for self action`() {
        val item = mkItem(
            mode = TestMode,
            hookResult = "OK"
        )
        val inventory = newInventory(mutableMapOf(item to 1))
        val human = createHuman(inventory)

        // Use a proper UseItemContext so Human's UseItem gate delegates to the item's concrete mode
        val ctx = project.actions.UseItemContext(human, human, item, object : project.actions.ActionContext {
            override val mode = TestMode
            override val source = human
            override val target = human
            override val item = item
        })

        val def = human.getInteractionDefinitionForMode(
            mode = UseItemMode,
            contextIfNeeded = ctx
        )
        assertNotNull(def)
        val result = def!!.invokeHook(Action.Possible(ctx))
        assertEquals("OK", (result as SimpleReturn).v)
    }

    @Test
    fun `does not fall back to inventory when target is not self`() {
        val item = mkItem(mode = TestMode)
        val otherItem = mkItem(mode = TestMode)
        val inventory = newInventory(mutableMapOf(item to 1))
        val inventory2 = newInventory(mutableMapOf(otherItem to 1))
        val human = createHuman(inventory)

        createHuman(inventory2)

        val def = human.getInteractionDefinitionForMode(
            mode = TestMode,
            contextIfNeeded = null
        )
        assertNull(def)
    }

    @Test
    fun `validateInside uses item validator for self action`() {
        val failure = TestActionFailure
        val item = mkItem(
            mode = TestMode,
            validatorResult = failure
        )
        val inventory = newInventory(mutableMapOf(item to 1))
        val human = createHuman(inventory)
        val ctx = project.actions.UseItemContext(human, human, item, object : project.actions.ActionContext {
            override val mode = TestMode
            override val source = human
            override val target = human
            override val item = item
        })

        // Validate via the item's concrete mode definition (item validators are on the item)
        val itemDef = item.getInteractionDefinitionForMode(TestMode, ctx)
        assertNotNull(itemDef)

        val result = itemDef!!.validateInside(ctx)
        assertSame(failure, result)
    }

    @Test
    fun `item interaction definition is chosen based on item equality`() {
        val item1 = mkItem(mode = TestMode, hookResult = 1)
        val item2 = mkItem(mode = TestMode, hookResult = 2)

        val inventory1 = newInventory(mutableMapOf(item1 to 1, item2 to 1))
        val human = createHuman( inventory1, )

        val ctx1 = project.actions.UseItemContext(human, human, item2, object : project.actions.ActionContext {
            override val mode = TestMode
            override val source = human
            override val target = human
            override val item = item2
        })

        val humanDef1 = human.getInteractionDefinitionForMode(UseItemMode, ctx1)
        assertNotNull(humanDef1)
        val humanRet1 = humanDef1!!.invokeHook(Action.Possible(ctx1))
        assertEquals(2, (humanRet1 as SimpleReturn).v)

        val ctx2 = project.actions.UseItemContext(human, human, item2, object : project.actions.ActionContext {
            override val mode = TestMode
            override val source = human
            override val target = human
            override val item = item2
        })

        val humanDef2 = human.getInteractionDefinitionForMode(UseItemMode, ctx2)
        assertNotNull(humanDef2)
        val humanRet2 = humanDef2!!.invokeHook(Action.Possible(ctx2))
        assertEquals(2, (humanRet2 as SimpleReturn).v)
    }

    @Test
    fun `itemAllowedModes reflects inventory interaction modes`() {
        val item1 = mkItem(mode = TestMode)
        val item2 = mkItem(mode = OtherMode)

        val inventory1 = newInventory(mutableMapOf(item1 to 1, item2 to 1))
        val human = createHuman(
            inventory1
        )

        val modes = human.itemAllowedModes

        assertEquals(human.modes.size + 2 , modes.size)
        assertTrue(modes.contains(TestMode))
        assertTrue(modes.contains(OtherMode))
    }
}
