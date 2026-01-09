package test.units.actions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import project.actions.*
import test.units.TestActor
import test.units.TestGrid
import test.units.TestMode

class TargetSelectorUnitTest {

    @Test
    fun `SelfTargetSelector returns source in seed`() {
        val actor = TestActor()
        val seed = ActionSeed(mode = TestMode, source = actor, sourceItem = null, grid = TestGrid())
        val sel = SelfTargetSelector()
        val set = sel.select(seed)
        assertTrue(set.contains(actor))
    }
}
