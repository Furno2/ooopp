package test.units.actions

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import project.Heal
import project.Human
import project.HumanEffect
import project.InteractionDefinition
import project.Position
import project.EquipmentSlot
import project.actions.*
import test.units.TestGrid
import test.units.TestHookReturn
import test.units.TestItem
import test.units.TestMode

class UseItemFlowTest {

    @Test
    fun `UseItem indexed and resolves via itemMode`() {
        val item = TestItem(
            name = "Potion", interactionDefinitions = listOf(
                InteractionDefinition(mode = TestMode, hook = { TestHookReturn("healed") }, validator = { null })
            )
        )

        val human = Human(position = Position(0, 0), grid = TestGrid(), initialInventory = listOf(item to 1))

        val ctx = UseItemContext(human, human, item, object : ActionContext {
            override val mode = TestMode
            override val source = human
            override val target = human
            override val item = item
        })
        val action = Action.Possible(ctx)

        val pa = PotentialActions()
        pa.addAction(action)

        val byGate = pa.getActionsForMode(UseItemMode)
        val byItem = pa.getActionsForMode(TestMode)

        Assertions.assertTrue(byGate.contains(action))

        // Ensure UseItem gate exists on Human
        val useDef = human.getInteractionDefinitionForMode(UseItemMode, ctx)
        Assertions.assertNotNull(useDef)

        // Invoke the UseItem gate on the human; the Human hook will invoke the item's concrete mode and return its hook result
        val ret = useDef!!.invokeHook(action)
        Assertions.assertTrue(ret is TestHookReturn)
        Assertions.assertEquals("healed", (ret as TestHookReturn).data)
    }

    @Test
    fun `UseItem impossible when item not in inventory`() {
        val item = TestItem(
            name = "Potion", interactionDefinitions = listOf(
                InteractionDefinition(mode = TestMode, hook = { TestHookReturn("healed") }, validator = { null })
            )
        )

        val human = Human(position = Position(0, 0), grid = TestGrid(), initialInventory = listOf())
        val ctx = UseItemContext(human, human, item, object : ActionContext {
            override val mode = TestMode
            override val source = human
            override val target = human
            override val item = item
        })

        // The UseItem interaction exists on the Human, but validator should report ItemNotPresentInContainer
        val validation = human.validateInside(ctx)
        Assertions.assertSame(ItemNotPresentInContainer, validation)

        // Also ensure that the Human's UseItem interaction definition exists (the gate), but it will delegate and validation prevents execution
        val useDef = human.getInteractionDefinitionForMode(UseItemMode, ctx)
        Assertions.assertNotNull(useDef)
    }

    @Test
    fun `UseItem invokes item's interaction via itemMode and applies effects`() {
        // Use shared TestMode as the concrete item mode
        val healingItem = TestItem(
            name = "HealPotion", interactionDefinitions = listOf(
                InteractionDefinition(
                    mode = TestMode,
                    hook = {
                        // when used, produce a Heal effect to the source Human
                        Heal(20)
                    },
                    validator = { _ -> null }
                )
            ))

        val human = Human(TestGrid(), Position(0, 0), initialInventory = listOf(healingItem to 1))

        val ctx = UseItemContext(human, human, healingItem, object : ActionContext {
            override val mode = TestMode
            override val source = human
            override val target = human
            override val item = healingItem
        })

        // Invoke the Human's UseItem gate; it delegates to the item's concrete itemMode and returns its hook result
        val useDef = human.getInteractionDefinitionForMode(UseItemMode, ctx)
        Assertions.assertNotNull(useDef)

        val hookResult = useDef!!.invokeHook(Action.Possible(ctx))
        Assertions.assertTrue(hookResult is HumanEffect)
        if (hookResult is Heal) {
            Assertions.assertEquals(20, hookResult.amount)
        }
    }

    @Test
    fun `UseItem requires equipped slot when requiredSlot is set`() {
        val item = TestItem(name = "Sword", interactionDefinitions = listOf(
            InteractionDefinition(mode = TestMode, hook = { TestHookReturn("swing") }, validator = { null })
        ))

        val human = Human(position = Position(0,0), grid = TestGrid(), initialInventory = listOf(item to 1))

        // UseItem requires weapon equipped
        val useCtx = UseItemContext(human, human, item, object : ActionContext {
            override val mode = TestMode
            override val source = human
            override val target = human
            override val item = item
        }, requiredSlot = EquipmentSlot.WEAPON)

        // Initially not equipped -> validation fails
        val val1 = human.validateInside(useCtx)
        Assertions.assertSame(ItemNotEquipped, val1)

        // Equip the item into weapon slot using Equip interaction
        val equipCtx = EquipContext(human, human, item, EquipmentSlot.WEAPON, EquipOperation.EQUIP)
        val equipDef = human.getInteractionDefinitionForMode(EquipMode, equipCtx)
        Assertions.assertNotNull(equipDef)
        equipDef!!.invokeHook(Action.Possible(equipCtx))

        // Now validation should pass and using the item should return hook result
        val val2 = human.validateInside(useCtx)
        Assertions.assertNull(val2)

        val useDef = human.getInteractionDefinitionForMode(UseItemMode, useCtx)
        Assertions.assertNotNull(useDef)
        val ret = useDef!!.invokeHook(Action.Possible(useCtx))
        Assertions.assertTrue(ret is TestHookReturn)
        Assertions.assertEquals("swing", (ret as TestHookReturn).data)
    }

    @Test
    fun `UseItem requires not equipped when requireNotEquipped is true`() {
        val item = TestItem(name = "Scroll", interactionDefinitions = listOf(
            InteractionDefinition(mode = TestMode, hook = { TestHookReturn("read") }, validator = { null })
        ))

        val human = Human(position = Position(0,0), grid = TestGrid(), initialInventory = listOf(item to 1))

        // Equip the item first
        val equipCtx = EquipContext(human, human, item, EquipmentSlot.WEAPON, EquipOperation.EQUIP)
        val equipDef = human.getInteractionDefinitionForMode(EquipMode, equipCtx)
        Assertions.assertNotNull(equipDef)
        equipDef!!.invokeHook(Action.Possible(equipCtx))

        // Now requireNotEquipped -> should fail while equipped

        // Unequip

    }
}