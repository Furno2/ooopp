package project.actions

import project.Actor
import project.Entity
import project.HookReturnValue
import project.InteractionHandler
import project.Item
import project.OtherTargetSelector

interface ActionMode

enum class TargetType {
    SELF_ONLY,
    OTHERS_ONLY,
    OTHER
}

data class ActionSeed(
    val mode: ActionMode,
    val source: Actor,
    val sourceItem: Item?,
)

interface ActionFailure
object EntityDoesNotExist : ActionFailure
object InternalOnlyInteraction : ActionFailure
object ItemMismatchBetweenContextAndCapability : ActionFailure
object TooFar : ActionFailure

interface Capability {
    val mode: ActionMode
    val sourceItem: Item?

    // Nullable interaction handler now replaces previous single definition
    val interactionHandler: InteractionHandler?

    // Merged targetType + selector concept
    val targetSelector: TargetSelector
        get() = when (targetType) {
            TargetType.SELF_ONLY -> SelfTargetSelector()
            TargetType.OTHERS_ONLY -> OtherTargetSelector()
            TargetType.OTHER -> OtherTargetSelector() // customize if needed
        }

    val targetType: TargetType

    fun validateOutside(context: ActionContext): ActionFailure?

    fun discoverTargets(seed: ActionSeed): Set<Entity> = targetSelector.select(seed)

    fun matchesMode(m: ActionMode): Boolean = this.mode == m

    fun validate(context: ActionContext): Action? {
        if (!matchesMode(context.mode)) return null

        val isSelf = context.source === context.target
        if (targetType == TargetType.SELF_ONLY && !isSelf) return null
        if (targetType == TargetType.OTHERS_ONLY && isSelf) return null

        if (!context.source.doesContainCapability(this)) return null

        val outsideFailure = validateOutside(context)
        if (outsideFailure != null) {
            return Action.Impossible(context, outsideFailure)
        }
        // Source-side validation
        val sourceInsideFailure = interactionHandler?.validateInside(context)
        if (sourceInsideFailure != null) {
            return Action.Impossible(context, sourceInsideFailure)
        }

        // Target-side validation
        val targetInsideFailure = context.target.validateInside(context)
        if (targetInsideFailure != null) {
            return Action.Impossible(context, targetInsideFailure)
        }

        return Action.Possible(context,interactionHandler.hook.apply {  })
    }
}

interface ActionContext {
    val mode: ActionMode
    val source: Actor
    val target: Entity
    val item: Item?
    var capability: Capability?
}

sealed interface Action {
    val context: ActionContext

    data class Possible(
        override var context: ActionContext,
        private val sourceHook: (Action) -> Unit,
        private val targetHook: (Action) -> Unit,
    ) : Action{
        fun execute(){
            sourceHook(this)
            targetHook(this)
        }
    }

    data class Impossible(
        override val context: ActionContext,
        val reason: ActionFailure
    ) : Action
}


class PotentialActions(
    private val actionsByMode: MutableMap<ActionMode, MutableList<Action>> = mutableMapOf()
) {
    fun addAction(action: Action) {
        val mode = action.context.mode
        actionsByMode.getOrPut(mode) { mutableListOf() }.add(action)
    }

    fun getActionsForMode(mode: ActionMode): List<Action> {
        return actionsByMode[mode]?.toList() ?: emptyList()
    }

    fun getAllActions(): Map<ActionMode, List<Action>> {
        return actionsByMode.mapValues { it.value.toList() }
    }

    fun clear() {
        actionsByMode.clear()
    }
}
