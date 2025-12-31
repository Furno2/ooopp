package new

abstract class Entity {
    abstract val grid: Grid
    abstract var position: Position
    val modes: List<InteractionMode> = listOf()
    val capabilities: List<Capability<*>> = listOf()
    var interactionList = PotentialInteractions()

    fun <C : InteractionMode> buildPotentialInteractions(
        actor: Entity,
        targets: List<Entity>
    )  {
        var possible = PotentialInteractions()
        for (cap in actor.capabilities) {
            for (target in targets) {
                if (cap.mode !in target.modes) continue
                val context = createContext(this, target, grid)
                val result = cap.validate(context)
                if(result != null) {
                    possible.addAction(result)
                }
            }
        }
        interactionList = possible
    }
    fun createContext(self: Entity, target: Entity, grid: Grid): InteractionContext<C> {
        TODO()
    }
}