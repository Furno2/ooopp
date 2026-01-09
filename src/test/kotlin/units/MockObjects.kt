package test.units

import project.*
import project.actions.*


object TestMode : ActionMode
object OtherMode : ActionMode

object TestActionFailure : ActionFailure

open class TestGrid(
    private val setEntityImpl: ((Entity?, Position) -> Unit)? = null,
    private val getEntityImpl: ((Position) -> Entity?)? = null,
    private val moveEntityImpl: ((Entity, Position, Position) -> Unit)? = null,
    private val getTileTypeImpl: ((Position) -> Tile) = {Tile.Floor},
    private val canEnterImpl: ((Position) -> Boolean) = { true },
    initialEntities: Map<Position, Entity?> = emptyMap()
) : IGrid {
    val entities: MutableMap<Position, Entity?> = initialEntities.toMutableMap()

    override fun setEntity(entity: Entity?, position: Position) {
        if (setEntityImpl != null) {
            setEntityImpl(entity, position)
            return
        }
        entities[position] = entity
    }
    override fun getEntity(position: Position): Entity? {
        if (getEntityImpl != null) return getEntityImpl(position)
        return entities[position]
    }
    override fun moveEntity(entity: Entity, oldPosition: Position, newPosition: Position) {
        if (moveEntityImpl != null) { moveEntityImpl(entity, oldPosition, newPosition); return }
        val current = entities[oldPosition]
        if (current !== entity) throw IllegalStateException("entity not at oldPosition")
        entities.remove(oldPosition)
        entities[newPosition] = entity
    }
    override fun getTileType(position: Position) = getTileTypeImpl(position)
    override fun canEnter(position: Position) = canEnterImpl(position)
}


// Simple dummy AI for Actor


// Shared test entity used by multiple tests
open class TestEntity(
    override val grid: IGrid = TestGrid(),
    override var position: Position = Position(0, 0),
    override var char: Char = 'E',
    override val interactionDefinitions: List<InteractionDefinition> = emptyList(),
    private val aliveFlag: Boolean = true
) : Entity() {
    override fun isAlive(): Boolean {
        return aliveFlag
    }
}

// Shared TestActor used by many tests
class TestActor(
    override val grid: IGrid = TestGrid(),
    override var position: Position = Position(0, 0),
    override var char: Char = '@',
    override val interactionDefinitions: List<InteractionDefinition> = emptyList(),
    override val capabilitiesInternal: List<Capability> = listOf(),
    val isAliveFlag: Boolean = true
) : Actor()
{
    override val controller: Controller = NoopController

    override fun isAlive(): Boolean {
        return isAliveFlag
    }
}

// Shared minimal weapon used in tests
open class TestWeapon(
    var ammoAvailable: Boolean = true,
    val canAttackFunc: (AttackType) -> Boolean = { true },
    override val name: String = "TestSword",
    override val capabilities: List<Capability> = listOf(),
    override val interactionDefinitions: List<InteractionDefinition> = listOf(),
    val damageValue: Int = 4,
    override val maxAmmo: Int = 10
) : Weapon() {
    override fun canAttack(attackType: AttackType) = canAttackFunc(attackType)
    override val damage: Int get() = damageValue
    override fun hasAmmo(): Boolean = ammoAvailable
    override fun consumeAmmo(): Boolean { val prev = ammoAvailable; ammoAvailable = false; return prev }
}

// Shared minimal item used in tests
open class TestItem(override val name: String = "TestItem",
    override val capabilities: List<Capability> = emptyList(),
    override val interactionDefinitions: List<InteractionDefinition> = emptyList(),
) : Item()

// Test helper: a simple Capability implementation for tests
class TestCapability(
    override val mode: ActionMode,
    override val sourceItem: Item? = null,
    override val targetType: TargetType = TargetType.OTHER,
    override val targetSelector: TargetSelector = SelfTargetSelector(),
    private val validateOutsideImpl: (ActionContext) -> ActionFailure? = { null }
) : Capability {
    override fun validateOutside(context: ActionContext): ActionFailure? = validateOutsideImpl(context)
}

// Simple test ActionContext implementation for tests
class TestActionContext(
    override val mode: ActionMode,
    override val source: Actor,
    override val target: Entity,
    override val item: Item? = null
) : ActionContext

// Simple HookReturnValue for tests
data class TestHookReturn(val data: Any?) : HookReturnValue
