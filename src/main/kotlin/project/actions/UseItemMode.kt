package project.actions

import project.*

object UseItemMode : ActionMode

data class UseItemContext(
    override val source: Actor,
    override val target: Entity,
    override val item: Item,
    val itemContext: ActionContext,
    val requiredSlot: EquipmentSlot? = null,
) : ActionContext {
    override val mode = UseItemMode
    override var capability: Capability? = null
}

data class UseItemCapability(
    override val sourceItem: Item,
    override val interactionHandler: InteractionHandler? = null,
) : Capability {
    override val mode: ActionMode = UseItemMode
    override val targetType = TargetType.SELF_ONLY
    override val targetSelector: TargetSelector = SelfTargetSelector()

    override fun validateOutside(context: ActionContext): ActionFailure? {
        return null
    }

    override fun validate(context: ActionContext): Action? {
        return if(context.mode != UseItemMode) {
            null //difference in handling nulls and impossibles returned from validate
        } else super.validate(context)
    }
}
