package new

abstract class Entity {
    abstract val grid: IGrid
    protected abstract var position: Position

    abstract val interactionModes: List<InteractionMode>
    protected abstract val interactionHooks: Map<InteractionMode, (Interaction.Possible<*>) -> Unit>

    protected abstract var capabilities: List<Capability<out InteractionMode>>
    var interactionList = PotentialInteractions()

    internal val handler = InteractionHandlers

    fun validateHooks(){
        require(interactionModes.isEmpty() && interactionHooks.isEmpty() ||
                interactionModes.containsAll(interactionHooks.keys)) {
            "All listed interaction hooks must have modes"
        }
        require(interactionModes.isEmpty() && interactionHooks.isEmpty() ||
                interactionHooks.keys.containsAll(interactionModes)) {
            "All supported interaction modes must have hooks"
        }
    }
    @JvmName("getPositionPublic")
    fun getPosition() = position
}
