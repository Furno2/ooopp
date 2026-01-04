package test.units

import project.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import project.actions.ActionContext
import project.actions.ActionFailure
import project.actions.ActionMode
import project.actions.Capability
import project.actions.TargetType
import project.actions.ValidatedAction

// --- Test doubles ---

// --- Actor Test double ---
class TestActor  (
    override val grid: IGrid = TestGrid(),
    override var position: Position = Position(0, 0),
    override var char: Char = 'A',
    override val interactionDefinitions: List<InteractionDefinition> = listOf(),
    override val capabilitiesInternal: List<Capability> = listOf(),
): Actor()

// --- Tests ---
class EntityActorUnitTest {

    // ---------- ENTITY TESTS ----------
    @Test
    fun `getPosition returns correct position`() {
        val pos = Position(2, 3)
        val entity = EmptyEntity(position = pos)
        assertEquals(pos, entity.getPosition())
    }

    @Test
    fun `toString returns char as string`() {
        val entity = EmptyEntity(char = 'E')
        assertEquals("E", entity.toString())
    }

    @Test
    fun `modes getter returns all interaction modes`() {
        val def1 = InteractionDefinition(TestMode, hook = {}, validator = { null })
        val def2 = InteractionDefinition(OtherMode, hook = {}, validator = { null })
        val entity = EmptyEntity(defs = listOf(def1, def2))
        assertTrue(entity.modes.contains(TestMode))
        assertTrue(entity.modes.contains(OtherMode))
        assertEquals(2, entity.modes.size)
    }

    @Test
    fun `mode getter doesn't return duplicated`() {
        val def1 = InteractionDefinition(TestMode, hook = {}, validator = { null })
        val def2 = InteractionDefinition(TestMode, hook = {}, validator = { null })
        val entity = EmptyEntity(defs = listOf(def1, def2))
        assertTrue(entity.modes.contains(TestMode))
        assertEquals(1, entity.modes.size)
    }

    @Test
    fun `invokeHook executes hook and returns its value`() {
        var hookCalled = false
        val expectedReturn = "hooked"

        val def = InteractionDefinition(
            mode = TestMode,
            hook = { hookCalled = true; expectedReturn },
            validator = { null }
        )
        val entity = EmptyEntity(defs = listOf(def))

        val ctx = object : ActionContext {
            override val mode = TestMode
            override val source: Actor = TestActor()
            override val target: Entity = entity
            override val sourceItem: Item? = null
        }
        val action = ValidatedAction.Possible(ctx)
        val result = entity.invokeHook(TestMode, action)

        assertTrue(hookCalled)
        assertEquals(expectedReturn, result)
    }

    @Test
    fun `validateInside executes validator and returns failure`() {
        val failure = object : ActionFailure {}
        val def = InteractionDefinition(
            mode = TestMode,
            hook = {},
            validator = { failure }
        )
        val entity = EmptyEntity(defs = listOf(def))

        val ctx = object : ActionContext {
            override val mode = TestMode
            override val source: Actor = TestActor()
            override val target: Entity = entity
            override val sourceItem: Item? = null
        }

        val result = entity.validateInside(ctx)
        assertSame(failure, result)
    }

    @Test
    fun `invokeHook returns null if no definition exists`() {
        val entity = EmptyEntity(defs = listOf())
        val ctx = object : ActionContext {
            override val mode = TestMode
            override val source: Actor = TestActor()
            override val target: Entity = entity
            override val sourceItem: Item? = null
        }
        val action = ValidatedAction.Possible(ctx)
        val result = entity.invokeHook(TestMode, action)
        assertNull(result)
    }

    @Test
    fun `validateInside returns null if no definition exists`() {
        val entity = EmptyEntity(defs = listOf())
        val ctx = object : ActionContext {
            override val mode = TestMode
            override val source = TestActor()
            override val target = entity
            override val sourceItem: Item? = null
        }
        val result = entity.validateInside(ctx)
        assertNull(result)
    }

    @Test
    fun `validateInside returns null if specified`() {
        val def = InteractionDefinition(
        mode = TestMode,
        hook = {},
        validator = {null}
    )
        val entity = EmptyEntity(defs = listOf())
        val ctx = object : ActionContext {
            override val mode = TestMode
            override val source = TestActor()
            override val target = entity
            override val sourceItem: Item? = null
        }
        val result = entity.validateInside(ctx)
        assertNull(result)
    }

    // ---------- ACTOR TESTS ----------
    @Test
    fun `capabilities getter returns capabilitiesInternal copy`() {
        val cap1 = object : Capability {
            override val mode: ActionMode = TestMode
            override val sourceItem: Item? = null
            override val targetType: TargetType = TargetType.ANY
            override fun validateOutside(context: ActionContext) = null
        }

        val actor =  TestActor(
            capabilitiesInternal = listOf(cap1)
        )

        val caps = actor.capabilities
        assertEquals(1, caps.size)
        assertTrue(caps.contains(cap1))
    }

    @Test
    fun `capabilities can hold duplicated`() {
        val cap1 = object : Capability {
            override val mode: ActionMode = TestMode
            override val sourceItem: Item? = null
            override val targetType: TargetType = TargetType.ANY
            override fun validateOutside(context: ActionContext) = null
        }

        val actor =  TestActor(
            capabilitiesInternal = listOf(cap1,cap1)
        )

        val caps = actor.capabilities
        assertEquals(2, caps.size)
        assertTrue(caps.contains(cap1))
    }

    @Test
    fun `doesContainCapability returns true for contained capability`() {
        val cap1 = object : Capability {
            override val mode: ActionMode = TestMode
            override val sourceItem: Item? = null
            override val targetType: TargetType = TargetType.ANY
            override fun validateOutside(context: ActionContext) = null
        }

        val actor = TestActor(
            capabilitiesInternal = listOf(cap1)
        )

        assertTrue(actor.doesContainCapability(cap1))
        val cap2 = object : Capability {
            override val mode: ActionMode = OtherMode
            override val sourceItem: Item? = null
            override val targetType: TargetType = TargetType.ANY
            override fun validateOutside(context: ActionContext) = null
        }
        assertFalse(actor.doesContainCapability(cap2))
    }

    @Test
    fun `capability validate pipeline returns Possible or Impossible correctly`() {
        val failure = object : ActionFailure {}

        val cap1 = object : Capability {
            override val mode: ActionMode = TestMode
            override val sourceItem: Item? = null
            override val targetType: TargetType = TargetType.ANY
            override fun validateOutside(context: ActionContext): ActionFailure? = failure
        }

        val cap2 = object : Capability {
            override val mode: ActionMode = TestMode
            override val sourceItem: Item? = null
            override val targetType: TargetType = TargetType.ANY
            override fun validateOutside(context: ActionContext): ActionFailure? = null
        }

        val actor = TestActor(
            capabilitiesInternal= listOf(cap1,cap2)
        )

        val ctx = object : ActionContext {
            override val mode: ActionMode = TestMode
            override val source: Actor = actor
            override val target: Entity = EmptyEntity()
            override val sourceItem: Item? = null
        }

        val action1 = cap1.validate(ctx)
        assertTrue(action1 is ValidatedAction.Impossible)
        assertSame(failure, (action1 as ValidatedAction.Impossible).reason)
        assertSame(ctx, action1.context)

        val action2 = cap2.validate(ctx)
        assertTrue(action2 is ValidatedAction.Possible)
        assertSame(ctx, (action2 as ValidatedAction.Possible).context)
    }

    @Test
    fun `capability validate returns null if mode does not match`() {
        val cap = object : Capability {
            override val mode: ActionMode = TestMode
            override val sourceItem: Item? = null
            override val targetType: TargetType = TargetType.ANY
            override fun validateOutside(context: ActionContext) = null
        }

        val actor = TestActor(
            capabilitiesInternal= listOf(cap)
        )

        val ctx = object : ActionContext {
            override val mode: ActionMode = OtherMode
            override val source: Actor = actor
            override val target: Entity = EmptyEntity()
            override val sourceItem: Item? = null
        }

        val action = cap.validate(ctx)
        assertNull(action)
    }
}
