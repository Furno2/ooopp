package new

abstract class Entity {
    abstract val grid: IGrid
    protected abstract var position: Position
    abstract var char : Char

    abstract val interactionModes: List<InteractionMode>
    abstract val interactionHooks: Map<InteractionMode, (Interaction.Possible<*>) -> Unit>

    abstract var capabilities: List<Capability<out InteractionMode>>
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
    @JvmName("setPositionPublic")
    fun setPosition(positionInput: Position) {
        position = positionInput
    }
    override fun toString() = char.toString()
}


class Player(override var position: Position, override val grid: IGrid) : Entity() {
    override var char: Char = '@'
    override var interactionModes: List<InteractionMode> = emptyList()
    override var interactionHooks: Map<InteractionMode, (Interaction.Possible<*>) -> Unit> = mapOf(


        Movement to { interaction ->
            val context = interaction.context
            val castedContext = context as MovementContext
            with (castedContext) {
                grid.moveEntity(source, targetPosition)
                source.setPosition(targetPosition)
            }
        }

    )
    override var capabilities: List<Capability<out InteractionMode>> = listOf(
        MovementCapability(this)
    )



}



