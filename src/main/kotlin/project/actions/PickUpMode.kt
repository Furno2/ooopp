package project.actions

import project.Actor
import project.Entity
import project.IGrid
import project.IInventory
import project.InteractionHandler
import project.Item

object PickUpMode : ActionMode

interface PickUpFailure : ActionFailure
object InventoryFull : PickUpFailure

data class PickUpContext(
    override val source: Actor,
    override val target: Entity, // The ItemEntity on the ground
    val grid: IGrid,
    val targetInventory: IInventory
) : ActionContext {
    override val mode = PickUpMode
    override val item = null
    override var capability: Capability? = null
}


data class PickUpCapability(
    override val interactionHandler: InteractionHandler? = null,
) : Capability {
    override val mode = PickUpMode
    override val sourceItem: Item? = null
    override val targetType = TargetType.OTHERS_ONLY
    override val targetSelector: TargetSelector = SelfTargetSelector() //TODO(): Change to proper selector

    override fun validateOutside(context: ActionContext): ActionFailure? {
        val srcPos = context.source.getPosition()
        val tgtPos = context.target.getPosition()

        if (!srcPos.isAdjacent(tgtPos) && srcPos != tgtPos) {
            return TooFar
        }

        if ((context as PickUpContext).grid.getEntity(tgtPos) != context.target) {
            return EntityDoesNotExist
        }

        return null
    }
    fun handlePickUp(inventory: IInventory, item: Item){
        inventory.addItem(item)
    }
}
