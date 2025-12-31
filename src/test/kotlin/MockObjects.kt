package test

import new.Capability
import new.Entity
import new.IGrid
import new.Interaction
import new.InteractionMode
import new.Position

class EmptyEntity(override var position: Position, override val grid: IGrid): Entity(){
    override var capabilities: List<Capability<out InteractionMode>> = emptyList()
    override val interactionHooks: Map<InteractionMode, (Interaction.Possible<*>) -> Unit> = emptyMap()
    override val interactionModes: List<InteractionMode> = emptyList()

}