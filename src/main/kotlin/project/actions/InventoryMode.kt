package project.actions

import project.Actor
import project.Entity
import project.IInventory
import project.InteractionHandler
import project.Item

object InventoryMode : ActionMode

object ItemNotPresentInContainer : ActionFailure

data class InventoryContext(
    override val source: Actor,
    override val target: Entity,
    override val item: Item,
    val action: InventoryOperationType,
) : ActionContext {
    override val mode = InventoryMode
    override var capability: Capability? = null
}

data class InventoryCapability(
    override val interactionHandler: InteractionHandler? = null
) : Capability {
    override val targetType = TargetType.OTHER
    override val mode = InventoryMode
    override val sourceItem = null
    override val targetSelector: TargetSelector = SelfTargetSelector()
    override fun validateOutside(context: ActionContext): ActionFailure? {
        with(context as InventoryContext) {
            val srcPos = source.getPosition()
            val tgtPos = target.getPosition()

            return if (source == target || srcPos.isAdjacent(tgtPos)) {
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