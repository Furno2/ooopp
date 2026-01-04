package project.actions

import project.Actor
import project.Entity
import project.Item

interface ActionMode

enum class TargetType {
    ANY,
    SELF_ONLY,
    OTHERS_ONLY
}

interface Capability{
    val mode: ActionMode
    val sourceItem: Item?
    val targetType: TargetType
    fun validateOutside(context: ActionContext): ActionFailure?


    fun equalsCapability(other: Capability): Boolean {
        if (this === other) return true
        if (mode != other.mode) return false
        if (sourceItem != other.sourceItem) return false
        if (targetType != other.targetType) return false
        return true
    }
    // New helper to check mode on the capability implementation. Default compares to the stored mode.
    fun matchesMode(m: ActionMode): Boolean = this.mode == m

    fun validate(context: ActionContext): ValidatedAction?{
        // use the capability-provided mode check
        if(!matchesMode(context.mode)){
            return null
        }

        val isSelf = context.source === context.target
        if (targetType == TargetType.SELF_ONLY && !isSelf) return null
        if (targetType == TargetType.OTHERS_ONLY && isSelf) return null

        // use entity's containment helper instead of direct list equality
        if(!context.source.doesContainCapability(this)){
            return null
        }

        val outsideFailure = validateOutside(context)
        if (outsideFailure != null) {
            return ValidatedAction.Impossible(context, outsideFailure)
        }

        val insideFailure = context.target.validateInside(context)
        if (insideFailure != null) {
            return ValidatedAction.Impossible(context, insideFailure)
        }

        return ValidatedAction.Possible(context)
    }
}

interface ActionContext {
    val mode: ActionMode
    val source : Actor
    val target : Entity
    val sourceItem : Item?
}

sealed interface ValidatedAction{
    val context: ActionContext
    data class Possible(
        override var context: ActionContext
    ) : ValidatedAction

    data class Impossible (
        override val context: ActionContext,
        val reason: ActionFailure
    ) : ValidatedAction
}

interface ActionFailure
object EntityDoesNotExist: ActionFailure
object InternalOnlyInteraction: ActionFailure

class PotentialActions(
    private val actionsByMode: MutableMap<ActionMode, MutableList<ValidatedAction>> = mutableMapOf()
)
{
    fun addAction(action: ValidatedAction) {
        val mode = action.context.mode
        actionsByMode.getOrPut(mode) { mutableListOf() }.add(action)
    }

    fun getActionsForMode(mode: ActionMode): List<ValidatedAction> {
        return actionsByMode[mode]?.toList() ?: emptyList()
    }

    fun getAllActions(): Map<ActionMode, List<ValidatedAction>> {
        return actionsByMode.mapValues { it.value.toList() }
    }

    fun clear() {
        actionsByMode.clear()
    }
}
