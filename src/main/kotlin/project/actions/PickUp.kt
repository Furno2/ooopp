package project.actions

import project.Actor
import project.Entity
import project.EntityDoesntExist
import project.IGrid
import project.IInventory
import project.Item

// 1. Action Mode
object PickUp : ActionMode

// 2. Failure Reasons
sealed class PickUpFailure : ActionFailure {
    object TooFar : PickUpFailure()
    object InventoryFull : PickUpFailure()
}

// 3. Context
class PickUpContext(
    override val source: Actor,
    override val target: Entity, // The ItemEntity on the ground
    val grid: IGrid,
    val targetInventory: IInventory
) : ActionContext {
    override val mode = PickUp
    override val sourceItem = null
}


// 4. Capability (The "Outside" Gate)
class PickUpCapability : Capability {
    override val mode: ActionMode get() = PickUp
    override val sourceItem: Item? = null
    override val targetType = TargetType.OTHERS_ONLY

    override fun validateOutside(context: ActionContext): ActionFailure? {
        val srcPos = context.source.getPosition()
        val tgtPos = context.target.getPosition()

        // Physical check: Must be on same tile or adjacent
        if (!srcPos.isAdjacent(tgtPos) && srcPos != tgtPos) {
            return PickUpFailure.TooFar
        }

        // Reality check: Is the item still on the grid?
        if ((context as PickUpContext).grid.getEntity(tgtPos) != context.target) {
            return EntityDoesntExist
        }

        return null
    }
    fun handlePickUp(inventory: IInventory, item: Item){
        inventory.addItem(item)
    }
}
