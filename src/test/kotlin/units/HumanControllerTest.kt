// file: src/test/kotlin/project/ai/HumanControllerTest.kt
package test.units

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import project.*
import project.actions.*
import project.ai.HumanController
import test.units.actions.*

/**
 * Full test suite for HumanController using your mocks + action system.
 *
 * It tests:
 * - Combat priorities: Flee > Melee > Ranged > Heal > Reload > Chase > Base
 * - Non-combat priorities: Heal > Reload > Pickup > PickupPath > Wander
 * - Movement decisions (flee/chase)
 * - Reload when attack is impossible due to OutOfAmmo
 * - FSM transitions between combat and non-combat
 */
class HumanControllerTest {

    /** Helper to create a ValidatedAction.Impossible attack due to OutOfAmmo (kept explicit) */
    private fun impossibleAttackOutOfAmmo(source: Actor, target: Entity, grid: IGrid): Action.Impossible =
        Action.Impossible(AttackContext(source, target, grid, item = null, damage = 1, attackType = Melee), OutOfAmmo)

    /**
     * Test combat priorities: melee > ranged
     */
    @Test
    fun `combat chooses melee over ranged`() {
        val grid = TestGrid()
        val human = Human(grid, Position(5, 5))
        val controller = HumanController(human, grid)

        // replaced DummyEntity with shared TestEntity (pass grid)
        val target = TestEntity(grid, Position(4, 5))
        val actions = PotentialActions()
        // use shared attackAction helper (source = human)
        actions.addAction(attackAction(human, target, grid, attackType = Ranged))
        actions.addAction(attackAction(human, target, grid, attackType = Melee))

        controller.decide(human,actions)
        controller.decide(human,actions)
        controller.decide(human,actions)
        controller.decide(human,actions) //call multiple times to ensure proper attack entering
        val chosen = controller.decide(human, actions)
        assertNotNull(chosen)
        val ctx = chosen!!.context as AttackContext
        assertTrue(ctx.attackType is Melee, "Melee should be prioritized over ranged")
    }

    /**
     * Flee is chosen when HP is low and movement available
     */
    @Test
    fun `flee chosen when low HP`() {
        val grid = TestGrid()
        val human = Human(grid, Position(5, 5), maxHp = 10)
        val controller = HumanController(human, grid)

        // Enemy present -- use TestEntity
        val enemy = TestEntity(grid, Position(6, 5))
        controller.lastKnownTargetPosition = enemy.getPosition()

        // Allow movement to flee position (TestGrid.canEnter defaults to true)
        val fleePos = Position(4, 5)

        val actions = PotentialActions()
        // movementAction helper (source = human)
        actions.addAction(movementAction(human, grid, fleePos))
        actions.addAction(attackAction(human, enemy, grid, attackType = Melee))

        val chosen = controller.decide(human, actions)
        assertNotNull(chosen)
        assertTrue(chosen!!.context is MovementContext)
        assertEquals(fleePos, (chosen.context as MovementContext).targetPosition)
    }

    /**
     * Reload is chosen when attack is impossible due to OutOfAmmo
     */
    @Test
    fun `reload chosen when out of ammo`() {
        val grid = TestGrid()
        val human = Human(grid, Position(0, 0))
        val controller = HumanController(human, grid)
        // target as TestEntity
        val target = TestEntity(grid, Position(1, 0))

        val actions = PotentialActions()
        actions.addAction(impossibleAttackOutOfAmmo(human, target, grid))
        // create minimal items for reload helper
        val weaponItem = TestItem("TestWeapon")
        val ammoItem = TestItem("TestAmmo")
        actions.addAction(reloadAction(human, human, weaponItem, ammoItem))

        val chosen = controller.decide(human, actions)
        assertNotNull(chosen)
        assertTrue(chosen!!.context is ReloadContext)
    }

    /**
     * Chase movement occurs if last known enemy position is known
     */
    @Test
    fun `chase chosen when lastKnownPosition exists`() { //not testable without a proper grid
        val grid = TestGrid()
        val human = Human(grid, Position(0, 0))
        val controller = HumanController(human, grid)


        controller.lastKnownTargetPosition = Position(1, 0)

        val actions = PotentialActions()
        actions.addAction(attackAction(human, TestEntity(),grid, null,1, attackType = Ranged)) //change to combat mode
        actions.addAction(movementAction(human, grid, Position(1, 0)))

        val chosen = controller.decide(human, actions)
        assertNotNull(chosen)
        assertTrue(chosen!!.context is MovementContext)
        assertEquals(Position(1, 0), (chosen.context as MovementContext).targetPosition)
    }

    /**
     * Heal chosen in combat when HP low and heal action possible
     */
    @Test
    fun `heal in combat when hp low`() {
        val grid = TestGrid()
        val human = Human(grid, Position(0, 0), maxHp = 10)
        val controller = HumanController(human, grid)

        val enemy = TestEntity(grid, Position(1, 0))
        controller.lastKnownTargetPosition = enemy.getPosition()

        val actions = PotentialActions()
        actions.addAction(attackAction(human, enemy, grid, attackType = Melee))
        val medkit = TestItem("Medkit")
        actions.addAction(healAction(human, human, medkit))

        val chosen = controller.decide(human, actions)
        assertNotNull(chosen)
        assertTrue(chosen!!.context is HealContext)
    }

    /**
     * Non-combat priorities: heal > reload > pickup > pickup path > wander
     */
    @Test
    fun `non combat behavior prioritization`() {
        val grid = TestGrid()
        val human = Human(grid, Position(0, 0), maxHp = 50)
        val controller = HumanController(human, grid)

        val actions = PotentialActions()
        val itemEntity = TestEntity(grid, Position(1, 0))
        val med = TestItem("Medkit")
        actions.addAction(healAction(human, human, med)) // should pick this first
        val weaponItem = TestItem("Weapon")
        val ammoItem = TestItem("Ammo")
        actions.addAction(reloadAction(human, human, weaponItem, ammoItem))
        // create a proper PickUpContext: source=human, target=itemEntity, grid, targetInventory
        val targetInv = InventoryContainer(mutableMapOf())
        actions.addAction(Action.Possible(PickUpContext(human, itemEntity, grid, targetInv)))
        actions.addAction(movementAction(human, grid, Position(1,0))) // pickup path

        val chosen = controller.decide(human, actions)
        assertNotNull(chosen)
        assertTrue(chosen!!.context is HealContext)
    }

    /**
     * Base action chosen if no other behaviors available
     */
    @Test
    fun `base behavior fallback`() {
        val grid = TestGrid()
        val human = Human(grid, Position(0, 0))
        val controller = HumanController(human, grid)

        val actions = PotentialActions() // empty
        val chosen = controller.decide(human, actions)
        assertNull(chosen, "No actions available -> decide returns null (base behavior)")
    }
}
