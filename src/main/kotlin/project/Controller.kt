package project // mark to remove

import project.actions.Action
import project.actions.PotentialActions

/**
 * Example controller that picks the first Possible action found across all modes.
 * Stateless and useful for tests or simple AI.
 */
class FirstPossibleController : Controller {
    override fun decide(actor: Actor, actions: PotentialActions): Action? {
        val all = actions.getAllActions()
        for ((_, list) in all) {
            val firstPossible = list.firstOrNull { it is Action.Possible }
            if (firstPossible != null) return firstPossible
        }
        return null
    }
}

/**
 * Simple stateful controller that remembers the last chosen mode and attempts to
 * pick the next available Possible action in round-robin order across modes.
 */
class RoundRobinController(private val modeOrder: List<project.actions.ActionMode>) : Controller {
    private var lastIndex = -1

    override fun decide(actor: Actor, actions: PotentialActions): Action? {
        val all = actions.getAllActions()
        if (all.isEmpty()) return null

        // Build a list of modes filtered by available actions and follow modeOrder
        val availableModes = modeOrder.filter { all[it]?.any { a -> a is Action.Possible } == true }
        if (availableModes.isEmpty()) return null

        lastIndex = (lastIndex + 1) % availableModes.size
        val chosenMode = availableModes[lastIndex]
        val list = all[chosenMode] ?: return null
        return list.firstOrNull { it is Action.Possible }
    }
}
