package project.actions

import project.Actor
import project.Entity
import project.IInventory
import project.Item


// 1. Action Mode
//data class OpenInventory(val whoCanOpen: Boolean) : ActionMode
object Inventory : ActionMode

// 2. Failure Reasons
    object TooFar : ActionFailure
    object Locked : ActionFailure
    object ItemNotPresentInContainer : ActionFailure

// 3. Context
class InventoryContext(
    override val source: Actor,
    override val target: Entity,
    val action: InventoryOperationType,
    override val sourceItem: Item,
) : ActionContext {
    override val mode = Inventory
}

// 4. Capability (What the Player/Actor has)
class InventoryCapability(override val targetType: TargetType) : Capability {
    private val _mode: ActionMode = Inventory
    override val mode: ActionMode get() = _mode
    override val sourceItem = null
    override fun validateOutside(context: ActionContext): ActionFailure? {
        with(context as InventoryContext) {
            val srcPos = source.getPosition()
            val tgtPos = target.getPosition()


            // Realism check: Must be adjacent to open inventory
            return if (srcPos.isAdjacent(tgtPos) || srcPos == tgtPos ) {
                null
            } else {
                TooFar
            }
        }
    }
}

interface InventoryOperationType {
    fun operation(inventory: IInventory, item: Item): Any?
}

object InventoryAdd : InventoryOperationType {
    override fun operation(inventory: IInventory, item: Item){
            inventory.addItem(item)
    }
}

object InventoryRemove : InventoryOperationType {
    override fun operation(inventory: IInventory, item: Item){
            inventory.removeItem(item)
    }
}

object InventoryView : InventoryOperationType {
    override fun operation(inventory: IInventory, item: Item): Map<Item, Int> {
        return inventory.data
    }
}