package test.units.actions

import project.Actor
import project.Entity
import project.Item
import project.actions.ActionContext
import project.actions.ActionMode
import project.actions.PotentialActions
import project.actions.UseItemMode
import project.actions.UseItemContext
import project.actions.Action
import test.units.TestActor
import test.units.TestItem
import test.units.TestMode
import kotlin.test.Test
import kotlin.test.assertTrue

class ActionSystemTest {

    // Dummy context to test indexing
    private class DummyContext(override val mode: ActionMode, override val source: Actor, override val target: Entity, override val item: Item?) :
        ActionContext

    @Test
    fun `PotentialActions indexes UseItem under gate itemMode`() {
        val actions = PotentialActions()

        val actor = TestActor()
        val item = TestItem()

        val ctx = UseItemContext(actor, actor, item, object : ActionContext {
            override val mode = TestMode
            override val source = actor
            override val target = actor
            override val item = item
        })
        val action = Action.Possible(ctx)

        actions.addAction(action)

        val byGate = actions.getActionsForMode(UseItemMode)
        val byItemMode = actions.getActionsForMode(TestMode)

        assertTrue(byGate.contains(action), "UseItem should be indexed under gate mode")
    }

    @Test
    fun `PotentialActions allows duplicate entries when same action added twice`() {
        val actions = PotentialActions()
        val actor = TestActor()
        val ctx = DummyContext(TestMode, actor, actor, null)
        val action = Action.Possible(ctx)

        actions.addAction(action)
        actions.addAction(action)

        val list = actions.getActionsForMode(TestMode)
        // current design allows duplicates
        assertTrue(list.size > 1)
    }

    @Test
    fun `PotentialActions clear removes all entries`() {
        val actions = PotentialActions()
        val actor = TestActor()
        val ctx = DummyContext(TestMode, actor, actor, null)
        val action = Action.Possible(ctx)

        actions.addAction(action)
        assertTrue(actions.getAllActions().isNotEmpty())
        actions.clear()
        assertTrue(actions.getAllActions().isEmpty())
    }
}