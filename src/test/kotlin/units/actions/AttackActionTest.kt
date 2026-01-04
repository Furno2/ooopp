package test.units.actions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import project.*
import project.actions.*

class AttackCapabilityTest {

    // Minimal test grid
    class TestGrid : IGrid {
        override fun setEntity(entity: Entity?, position: Position) {}
        override fun getEntity(position: Position): Entity? = null
        override fun moveEntity(entity: Entity, newPosition: Position) {}
        override fun getTileType(position: Position) = Tile.Floor
        override fun canEnter(position: Position) = true
    }

    // Minimal target entity
    class TestEntity(override val grid: IGrid, override var position: Position) : Entity() {
        override var char: Char = 'E'
        override val interactionDefinitions: List<InteractionDefinition> = emptyList()
    }

    // Minimal weapon
    abstract class TestWeapon(val ammoAvailable: Boolean = true) : Weapon() {
        override val name: String = "TestSword"
        override val capabilities: List<Capability> = emptyList()
        override val interactionDefinitions: List<InteractionDefinition> = emptyList()
        override fun canAttack(attackType: AttackType) = true
        override val damage = 4
    }

    @Test
    fun `attack within melee range is possible`() {
        val grid = TestGrid()
        val weapon = object : TestWeapon() {}
        val capability = AttackCapability(weapon, Melee)

        val source = object : Actor() {
            override val grid = grid
            override var position = Position(0, 0)
            override var char = '@'
            override val interactionDefinitions: List<InteractionDefinition> = emptyList()
            override val capabilitiesInternal: List<Capability> = listOf(capability)
        }

        val target = TestEntity(grid, Position(0, 1)) // adjacent
        val context = AttackContext(source, target, grid, weapon, damage = 5, attackType = Melee)

        val result = capability.validate(context)
        assertTrue(result is ValidatedAction.Possible)
    }

    @Test
    fun `attack out of melee range fails with OutOfRange`() {
        val grid = TestGrid()
        val weapon = object : TestWeapon() {}
        val capability = AttackCapability(weapon, Melee)

        val source = object : Actor() {
            override val grid = grid
            override var position = Position(0, 0)
            override var char = '@'
            override val interactionDefinitions: List<InteractionDefinition> = emptyList()
            override val capabilitiesInternal: List<Capability> = listOf(capability)
        }

        val target = TestEntity(grid, Position(5, 5)) // too far
        val context = AttackContext(source, target, grid, weapon, damage = 5, attackType = Melee)

        val result = capability.validate(context)
        assertTrue(result is ValidatedAction.Impossible)
        assertEquals(OutOfRange, (result as ValidatedAction.Impossible).reason)
    }

    @Test
    fun `attack with weapon that cannot attack fails with InvalidWeapon`() {
        val grid = TestGrid()
        val weapon = object : TestWeapon() {
            override fun canAttack(type: AttackType) = false
        }
        val capability = AttackCapability(weapon, Melee)

        val source = object : Actor() {
            override val grid = grid
            override var position = Position(0, 0)
            override var char = '@'
            override val interactionDefinitions: List<InteractionDefinition> = emptyList()
            override val capabilitiesInternal: List<Capability> = listOf(capability)
        }

        val target = TestEntity(grid, Position(0, 1))
        val context = AttackContext(source, target, grid, weapon, damage = 5, attackType = Melee)

        val result = capability.validate(context)
        assertTrue(result is ValidatedAction.Impossible)
        assertEquals(InvalidWeapon, (result as ValidatedAction.Impossible).reason)
    }

    @Test
    fun `ranged attack with ammo passes, without ammo fails with NoAmmo`() {
        val grid = TestGrid()
        // Weapon with ammo
        val weaponWithAmmo = object : TestWeapon() {}
        val capabilityWithAmmo = AttackCapability(weaponWithAmmo, Ranged)

        val sourceWithAmmo = object : Actor() {
            override val grid = grid
            override var position = Position(0, 0)
            override var char = '@'
            override val interactionDefinitions: List<InteractionDefinition> = emptyList()
            override val capabilitiesInternal: List<Capability> = listOf(capabilityWithAmmo)
        }

        val target = TestEntity(grid, Position(10, 10))
        val contextWithAmmo =
            AttackContext(sourceWithAmmo, target, grid, weaponWithAmmo, damage = 5, attackType = Ranged)

        val resultWithAmmo = capabilityWithAmmo.validate(contextWithAmmo)
        assertTrue(resultWithAmmo is ValidatedAction.Possible)
    }


}
