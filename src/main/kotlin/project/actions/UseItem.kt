package project.actions

import project.*

object UseItem : ActionMode

// Context for using an item
class UseItemContext(
    override val source: Actor,
    override val target: Entity,       // usually self
    override val sourceItem: Item      // the item being used
) : ActionContext {
    override val mode = UseItem
}

// Capability for using an item (mostly a "gate" for validation)
class UseItemCapability(
    override val sourceItem: Item?,
    override val targetType: TargetType = TargetType.SELF_ONLY
) : Capability {

    override val mode: ActionMode = UseItem

    override fun validateOutside(context: ActionContext): ActionFailure? {
        return null
    }
}
