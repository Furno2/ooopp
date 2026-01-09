package project

import project.actions.ActionFailure
import project.actions.AttackType
import project.actions.Capability
import project.actions.ReloadContext

abstract class Item{
    abstract val capabilities: List<Capability>
}

abstract class Weapon: Item() {
    abstract val damage: Int
    abstract fun canAttack(attackType: AttackType): Boolean

    abstract fun hasAmmo(): Boolean
    abstract val maxAmmo: Int

    abstract fun prepareReload(requested: Int): Int

    abstract fun acceptReload(accepted: Int)
}

abstract class Armor : Item() {
    open val protection: Int = 0
}


enum class EquipmentSlot{
    WEAPON, ARMOR
}

interface IInventory {
    fun addItem(item: Item)
    fun removeItem(item: Item) : Item?
    fun removeBulk(item: Item, count: Int) : Pair<Item,Int>
    val data: Map<Item,Int>

}

class InventoryContainer(private val _inventory: MutableMap<Item,Int>):IInventory{
    override fun addItem(item: Item){
        _inventory[item] = (_inventory[item] ?: 0) + 1
    }
    override fun removeItem(item: Item): Item?{
        val count = _inventory[item] ?: return null

        if (count <= 1) {
            _inventory.remove(item)
        } else {
            _inventory[item] = count - 1
        }

        return item
    }
    override fun removeBulk(item: Item, countToRemove: Int): Pair<Item,Int> {
        val count = _inventory[item] ?: return Pair(item,0)
        if (count <= countToRemove) {
            _inventory.remove(item)
            return Pair(item, count)
        }
        else {
            _inventory[item] = _inventory[item] as Int - count
            return Pair(item,count)

        }
    }
    override val data: Map<Item,Int> get() = _inventory.toMap()
}
