package project

import project.actions.ActionContext
import project.actions.ActionMode
import project.actions.AttackType
import project.actions.Capability

abstract class Item : InteractionDefinitionProvider{
    abstract val name: String

    abstract val capabilities: List<Capability>
    protected abstract val interactionDefinitions: List<InteractionDefinition>

    val modes: List<ActionMode> get() = interactionDefinitions.map{it.mode}.toList()

    override fun getInteractionDefinitionForMode(mode: ActionMode, contextIfNeeded: ActionContext?): InteractionDefinition? {
        return if(contextIfNeeded?.sourceItem === this) {interactionDefinitions.firstOrNull{it.mode == mode}} else null
    }
}

abstract class Weapon: Item() {
    abstract val damage: Int
    abstract fun canAttack(attackType: AttackType): Boolean
}

abstract class Armor : Item()

abstract class Artefact : Item()

enum class EquipmentSlot{
    WEAPON, ARMOR, ARTEFACT
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
    override val data: Map<Item,Int> get() = _inventory
}