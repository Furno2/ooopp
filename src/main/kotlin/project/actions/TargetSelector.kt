package project.actions

import project.Entity

/**
 * Seed-based selector that discovers candidate targets without requiring a target to be provided.
 */
interface TargetSelector {
    fun select(seed: ActionSeed): Set<Entity>
}

class SelfTargetSelector : TargetSelector {
    override fun select(seed: ActionSeed): Set<Entity> = setOf(seed.source)
}

