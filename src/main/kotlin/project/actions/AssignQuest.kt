package project.actions

import project.Actor
import project.Entity
import project.Item


object AssignQuest : ActionMode // potentially wrong place for this

interface Quest {
    val id: String
}

sealed interface AssignQuestFailure : ActionFailure {
    object TargetNotActor : AssignQuestFailure
    object AlreadyHasQuest : AssignQuestFailure
}

class AssignQuestContext(
    override val source: Actor,
    override val target: Entity,
    val quest: Quest
) : ActionContext {
    override val mode: ActionMode = AssignQuest
    override val sourceItem: Item? = null
}

class AssignQuestCapability(
    private val quest: Quest
) : Capability {

    override val mode: ActionMode = AssignQuest
    override val sourceItem: Item? = null
    override val targetType: TargetType = TargetType.OTHERS_ONLY

    override fun validateOutside(context: ActionContext): ActionFailure? {
        val ctx = context as AssignQuestContext

        val targetActor = ctx.target as? Actor
            ?: return AssignQuestFailure.TargetNotActor

        // Placeholder quest ownership check
        val alreadyHasQuest = false

        return if (alreadyHasQuest) {
            AssignQuestFailure.AlreadyHasQuest
        } else {
            null
        }
    }
}
