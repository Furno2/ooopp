package project

import project.actions.TargetSelector

class OtherTargetSelector(): TargetSelector() {

    override fun select(seed: project.actions.ActionSeed): Set<Entity> {
        // Logic to select other entities excluding the source
        return emptySet() // Placeholder implementation
    }

}
