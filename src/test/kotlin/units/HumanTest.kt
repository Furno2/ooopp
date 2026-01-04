package test.units

import project.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import project.actions.ActionContext
import project.actions.ActionFailure
import project.actions.ActionMode
import project.actions.Capability
import project.actions.ValidatedAction

// ---------- Test Item ----------
class TestItem(
    override val name: String = "TestItem",
    mode: ActionMode,
    hookResult: Any? = null,
    validatorResult: ActionFailure? = null
) : Item() {

    override val capabilities: List<Capability> = emptyList()

    override val interactionDefinitions: List<InteractionDefinition> =
        listOf(
            InteractionDefinition(
                mode = mode,
                hook = { hookResult },
                validator = { validatorResult }
            )
        )
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
            inventoryInternal = inventory,)

    @Test
    fun `inventory getter returns immutable copy`() {
        val item = TestItem(mode = TestMode)
        val inventory = newInventory(mutableMapOf(item to 1))
        val human = createHuman(inventory)

        val inv = human.inventory
        assertEquals(1, inv.size)
        assertSame(item, inv.keys.first())

    }

    @Test
    fun `falls back to inventory interaction definition for self action`() {
        val item = TestItem(
            mode = TestMode,
            hookResult = "OK"
        )
        val inventory = newInventory(mutableMapOf(item to 1))
        val human = createHuman(inventory)

        val ctx = object : ActionContext {
            override val mode = TestMode
            override val source: Actor = human
            override val target: Entity = human
            override val sourceItem = item
        }

        val def = human.getInteractionDefinitionForMode(
            mode = TestMode,
            contextIfNeeded = ctx
        )

        assertNotNull(def)

        val result = def!!.invokeHook(
            ValidatedAction.Possible(ctx)
        )

        assertEquals("OK", result)
    }

    @Test
    fun `does not fall back to inventory when target is not self`() {
        val item = TestItem(mode = TestMode)
        val otherItem = TestItem(mode = TestMode)
        val inventory = newInventory(mutableMapOf(item to 1))
        val inventory2 = newInventory(mutableMapOf(otherItem to 1))
        val human = createHuman(inventory)

        val other = createHuman(inventory2)

        val def = human.getInteractionDefinitionForMode(
            mode = TestMode,
            contextIfNeeded = null
        )
        assertNull(def)
    }

    @Test
    fun `validateInside uses item validator for self action`() {
        val failure = object : ActionFailure {}
        val item = TestItem(
            mode = TestMode,
            validatorResult = failure
        )
        val inventory = newInventory(mutableMapOf(item to 1))
        val human = createHuman(inventory)

        val ctx = object : ActionContext {
            override val mode = TestMode
            override val source: Actor = human
            override val target: Entity = human
            override val sourceItem = item
        }

        val def = human.getInteractionDefinitionForMode(
            mode = TestMode,
            contextIfNeeded = ctx
        )

        val result = def!!.validateInside(ctx)
        assertSame(failure, result)
    }

    @Test
    fun `item interaction definition is chosen based on item equality`() {
        val item1 = TestItem(mode = TestMode, hookResult = 1)
        val item2 = TestItem(mode = TestMode, hookResult = 2)

        val inventory1 = newInventory(mutableMapOf(item1 to 1, item2 to 1))
        val human = createHuman(
                                inventory1
            )

        val ctx1 = object : ActionContext {
            override val mode = TestMode
            override val source: Actor = human
            override val target: Entity = human
            override val sourceItem = item2
        }

        val def1 = human.getInteractionDefinitionForMode(
            mode = TestMode,
            contextIfNeeded = ctx1
        )

        val result1 = def1!!.invokeHook(
            ValidatedAction.Possible(ctx1)
        )

        assertEquals(2, result1)

        val ctx2 = object : ActionContext {
            override val mode = TestMode
            override val source: Actor = human
            override val target: Entity = human
            override val sourceItem = item2
        }

        val def2 = human.getInteractionDefinitionForMode(
            mode = TestMode,
            contextIfNeeded = ctx2
        )

        val result2 = def2!!.invokeHook(
            ValidatedAction.Possible(ctx2)
        )

        assertEquals(2, result2)
    }

    @Test
    fun `itemAllowedModes reflects inventory interaction modes`() {
        val item1 = TestItem(mode = TestMode)
        val item2 = TestItem(mode = OtherMode)

        val inventory1 = newInventory(mutableMapOf(item1 to 1, item2 to 1))
        val human = createHuman(
            inventory1
        )

        val modes = human.itemAllowedModes

        assertEquals(2, modes.size)
        assertTrue(modes.contains(TestMode))
        assertTrue(modes.contains(OtherMode))
    }
}
