package test.units.actions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import project.*
import project.actions.*
import test.units.*

class EquipTest {

    @Test
    fun `equip weapon into weapon slot succeeds`() {
        val weapon = TestWeapon()
        val cap = EquipmentCapability(sourceItem = weapon)
        val actor = TestActor(capabilitiesInternal = listOf(cap))
        val ctx = EquipContext(source = actor, target = actor, item = weapon, slot = EquipmentSlot.WEAPON, operation = EquipOperation.EQUIP)

        val result = cap.validate(ctx)
        assertTrue(result is Action.Possible)
    }

    @Test
    fun `equip weapon into armor slot fails invalid slot`() {
        val weapon = TestWeapon()
        val cap = EquipmentCapability(sourceItem = weapon)
        val actor = TestActor(capabilitiesInternal = listOf(cap))
        val ctx = EquipContext(source = actor, target = actor, item = weapon, slot = EquipmentSlot.ARMOR, operation = EquipOperation.EQUIP)

        val result = cap.validate(ctx)
        assertTrue(result is Action.Impossible)
        if (result is Action.Impossible) {
            assertTrue(result.reason is EquipFailure)
            assertSame(InvalidSlot, result.reason)
        }
    }

    @Test
    fun `equip missing item returns MissingItem failure`() {
        val cap = EquipmentCapability(sourceItem = null)
        val actor = TestActor(capabilitiesInternal = listOf(cap))
        val ctx = EquipContext(source = actor, target = actor, item = null, slot = EquipmentSlot.WEAPON, operation = EquipOperation.EQUIP)

        val result = cap.validate(ctx)
        assertTrue(result is Action.Impossible)
        if (result is Action.Impossible) {
            assertSame(MissingItem, result.reason)
        }
    }

    @Test
    fun `unequip operation allowed`() {
        val cap = EquipmentCapability(sourceItem = null)
        val actor = TestActor(capabilitiesInternal = listOf(cap))
        val ctx = EquipContext(source = actor, target = actor, item = null, slot = EquipmentSlot.WEAPON, operation = EquipOperation.UNEQUIP)

        val result = cap.validate(ctx)
        assertTrue(result is Action.Possible)
    }
}
