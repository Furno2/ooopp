package test.units.actions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import project.*
import project.actions.*
import test.units.OtherMode
import test.units.TestActionContext
import test.units.TestActionFailure
import test.units.TestActor
import test.units.TestCapability
import test.units.TestEntity
import test.units.TestMode

class CapabilityUnitTest {

    @Test
    fun `mode mismatch returns null`() {
        val cap = TestCapability(mode = TestMode)
        val actor = TestActor(capabilitiesInternal = listOf())
        val ctx = TestActionContext(OtherMode, actor, TestEntity(), null)

        val result = cap.validate(ctx)
        assertNull(result)
    }

    @Test
    fun `self_only enforces target relation`() {
        val cap = TestCapability(mode = TestMode, targetType = TargetType.SELF_ONLY)
        val actor = TestActor()
        val other = TestEntity()
        val ctx = TestActionContext(TestMode, actor, other, null) // source != target

        val result = cap.validate(ctx)
        assertNull(result)
    }

    @Test
    fun `containment check returns null when source doesn't contain capability`() {
        val cap = TestCapability(mode = TestMode)
        val actor = TestActor(capabilitiesInternal = listOf()) // does not contain cap
        val ctx = TestActionContext(TestMode, actor, actor, null)

        val result = cap.validate(ctx)
        assertNull(result)
    }

    @Test
    fun `validateOutside failure becomes Impossible`() {
        val failure = TestActionFailure
        val cap = TestCapability(mode = TestMode, validateOutsideImpl = { failure })
        val actor = TestActor(capabilitiesInternal = listOf(cap))
        val ctx = TestActionContext(TestMode, actor, actor, null)

        val result = cap.validate(ctx)
        assertTrue(result is Action.Impossible)
        val imp = result as Action.Impossible
        assertSame(failure, imp.reason)
        assertSame(ctx, imp.context)
    }

    @Test
    fun `validateInside failure becomes Impossible`() {
        val failure = TestActionFailure
        // target will validateInside to return failure: create entity with InteractionDefinition validator
        val def = InteractionDefinition(mode = TestMode, hook = { null }, validator = { failure })
        val target = TestEntity(interactionDefinitions = listOf(def))

        val cap = TestCapability(mode = TestMode)
        val actor = TestActor(capabilitiesInternal = listOf(cap))
        val ctx = TestActionContext(TestMode, actor, target, null)

        val result = cap.validate(ctx)
        assertTrue(result is Action.Impossible)
        val imp = result as Action.Impossible
        assertSame(failure, imp.reason)
    }

    @Test
    fun `all checks pass returns Possible`() {
        val cap = TestCapability(mode = TestMode)
        val actor = TestActor(capabilitiesInternal = listOf(cap))
        val target = TestEntity()
        val ctx = TestActionContext(TestMode, actor, target, null)

        val result = cap.validate(ctx)
        assertTrue(result is Action.Possible)
        assertSame(ctx, (result as Action.Possible).context)
    }
}
