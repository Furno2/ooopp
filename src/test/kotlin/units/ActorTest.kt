package test.units

import project.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import project.actions.Action

// --- Tests ---
class EntityActorUnitTest {

    // ---------- ENTITY TESTS ----------
    @Test
    fun `getPosition returns correct position`() {
        val pos = Position(2, 3)
        val entity = TestEntity(position = pos)
        assertEquals(pos, entity.getPosition())
    }

    @Test
    fun `toString returns char as string`() {
        val entity = TestEntity(char = 'E')
        assertEquals("E", entity.toString())
    }

    @Test
    fun `modes getter returns all interaction modes`() {
        val def1 = InteractionDefinition(TestMode, hook = {null}, validator = { null })
        val def2 = InteractionDefinition(OtherMode, hook = {null}, validator = { null })
        val entity = TestEntity(interactionDefinitions = listOf(def1, def2))
        assertTrue(entity.modes.contains(TestMode))
        assertTrue(entity.modes.contains(OtherMode))
        assertEquals(2, entity.modes.size)
    }

    @Test
    fun `mode getter doesn't return duplicated`() {
        val def1 = InteractionDefinition(TestMode, hook = {null}, validator = { null })
        val def2 = InteractionDefinition(TestMode, hook = {null}, validator = { null })
        val entity = TestEntity(interactionDefinitions = listOf(def1, def2))
        assertTrue(entity.modes.contains(TestMode))
        assertEquals(1, entity.modes.size)
    }

    @Test
    fun `invokeHook executes hook and returns its value`() {
        var hookCalled = false
        val expectedReturn = TestHookReturn("result")

        val def = InteractionDefinition(
            mode = TestMode,
            hook = { hookCalled = true; expectedReturn },
            validator = { null }
        )
        val entity = TestEntity(interactionDefinitions = listOf(def))

        val ctx = TestActionContext(TestMode, TestActor(), entity, null)
        val action = Action.Possible(ctx)
        val result = entity.invokeHook(TestMode, action)

        assertTrue(hookCalled)
        assertEquals(expectedReturn, result)
    }

    @Test
    fun `validateInside executes validator and returns failure`() {
        val failure = TestActionFailure
        val def = InteractionDefinition(
            mode = TestMode,
            hook = {null},
            validator = { failure }
        )
        val entity = TestEntity(interactionDefinitions = listOf(def))

        val ctx = TestActionContext(TestMode, TestActor(), entity, null)

        val result = entity.validateInside(ctx)
        assertSame(failure, result)
    }

    @Test
    fun `invokeHook returns null if no definition exists`() {
        val entity = TestEntity(interactionDefinitions = listOf())
        val ctx = TestActionContext(TestMode, TestActor(), entity, null)
        val action = Action.Possible(ctx)
        val result = entity.invokeHook(TestMode, action)
        assertNull(result)
    }

    @Test
    fun `validateInside returns null if no definition exists`() {
        val entity = TestEntity(interactionDefinitions = listOf())
        val ctx = TestActionContext(TestMode, TestActor(), entity, null)
        val result = entity.validateInside(ctx)
        assertNull(result)
    }

    @Test
    fun `validateInside returns null if specified`() {
        val def = InteractionDefinition(
            mode = TestMode,
            hook = {null},
            validator = {null}
        )
        // pass the def into the TestEntity so it is used
        val entity = TestEntity(interactionDefinitions = listOf(def))
        val ctx = TestActionContext(TestMode, TestActor(), entity, null)
        val result = entity.validateInside(ctx)
        assertNull(result)
    }

    // ---------- ACTOR TESTS ----------
    @Test
    fun `capabilities getter returns capabilitiesInternal copy`() {
        val cap1 = TestCapability(mode = TestMode)

        val actor =  TestActor(
            capabilitiesInternal = listOf(cap1)
        )

        val caps = actor.capabilities
        assertEquals(1, caps.size)
        assertTrue(caps.contains(cap1))
    }

    @Test
    fun `capabilities can hold duplicated`() {
        val cap1 = TestCapability(mode = TestMode)

        val actor =  TestActor(
            capabilitiesInternal = listOf(cap1,cap1)
        )

        val caps = actor.capabilities
        assertEquals(2, caps.size)
        assertTrue(caps.contains(cap1))
    }

    @Test
    fun `doesContainCapability returns true for contained capability`() {
        val cap1 = TestCapability(mode = TestMode)

        val actor = TestActor(
            capabilitiesInternal = listOf(cap1)
        )

        assertTrue(actor.doesContainCapability(cap1))
        val cap2 = TestCapability(mode = OtherMode)
        assertFalse(actor.doesContainCapability(cap2))
    }

    @Test
    fun `capability validate pipeline returns Possible or Impossible correctly`() {
        val failure = TestActionFailure

        val cap1 = TestCapability(mode = TestMode, validateOutsideImpl = { failure })
        val cap2 = TestCapability(mode = TestMode)

        val actor = TestActor(
            capabilitiesInternal= listOf(cap1,cap2)
        )

        val ctx = TestActionContext(TestMode, actor, TestEntity(), null)

        val action1 = cap1.validate(ctx)
        assertTrue(action1 is Action.Impossible)
        assertSame(failure, (action1 as Action.Impossible).reason)
        assertSame(ctx, action1.context)

        val action2 = cap2.validate(ctx)
        assertTrue(action2 is Action.Possible)
        assertSame(ctx, (action2 as Action.Possible).context)
    }

    @Test
    fun `capability validate returns null if mode does not match`() {
        val cap = TestCapability(mode = TestMode)

        val actor = TestActor(
            capabilitiesInternal= listOf(cap)
        )

        val ctx = TestActionContext(OtherMode, actor, TestEntity(), null)

        val action = cap.validate(ctx)
        assertNull(action)
    }
}
