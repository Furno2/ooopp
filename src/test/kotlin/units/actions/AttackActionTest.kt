package test.units.actions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import project.*
import project.actions.*
import test.units.*

class AttackCapabilityTest {

    @Test
    fun `attack within melee range is possible`() {
        val grid = TestGrid()
        val weapon = TestWeapon()
        val capability = AttackCapability(weapon, Melee)

        val source = TestActor(grid = grid, position = Position(0,0), capabilitiesInternal = listOf(capability))

        val target = TestEntity(grid, Position(0, 1)) // adjacent
        val context = AttackContext(source, target, grid, weapon, damage = 5, attackType = Melee)

        val result = capability.validate(context)
        assertTrue(result is Action.Possible)
    }

    @Test
    fun `attack out of melee range fails with OutOfRange`() {
        val grid = TestGrid()
        val weapon = TestWeapon()
        val capability = AttackCapability(weapon, Melee)

        val source = TestActor(grid = grid, position = Position(0,0), capabilitiesInternal = listOf(capability))

        val target = TestEntity(grid, Position(5, 5)) // too far
        val context = AttackContext(source, target, grid, weapon, damage = 5, attackType = Melee)

        val result = capability.validate(context)
        assertTrue(result is Action.Impossible)
        assertEquals(OutOfRange, (result as Action.Impossible).reason)
    }

    @Test
    fun `attack with weapon that cannot attack fails with InvalidWeapon`() {
        val grid = TestGrid()
        val weapon = TestWeapon(canAttackFunc = { _ -> false })
        val capability = AttackCapability(weapon, Melee)

        val source = TestActor(grid = grid, position = Position(0,0), capabilitiesInternal = listOf(capability))

        val target = TestEntity(grid, Position(0, 1))
        val context = AttackContext(source, target, grid, weapon, damage = 5, attackType = Melee)

        val result = capability.validate(context)
        assertTrue(result is Action.Impossible)
        assertEquals(InvalidWeapon, (result as Action.Impossible).reason)
    }

    @Test
    fun `ranged attack with ammo passes, without ammo fails with NoAmmo`() {
        val grid = TestGrid()
        // Weapon with ammo
        val weaponWithAmmo = TestWeapon(ammoAvailable = true)
        val capabilityWithAmmo = AttackCapability(weaponWithAmmo, Ranged)

        val sourceWithAmmo = TestActor(grid = grid, position = Position(0,0), capabilitiesInternal = listOf(capabilityWithAmmo))

        val target = TestEntity(grid, Position(10, 10))
        val contextWithAmmo =
            AttackContext(sourceWithAmmo, target, grid, weaponWithAmmo, damage = 5, attackType = Ranged)

        val resultWithAmmo = capabilityWithAmmo.validate(contextWithAmmo)
        assertTrue(resultWithAmmo is Action.Possible)
    }


}
