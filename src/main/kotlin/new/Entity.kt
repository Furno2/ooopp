package new

abstract class Entity {
    abstract val grid: Grid
    protected abstract var position: Position

    protected abstract val interactionModes: List<InteractionMode>
    protected abstract val interactionHooks: Map<InteractionMode, (Interaction.Possible<*>) -> Unit>

    protected abstract var capabilities: List<Capability<out InteractionMode>>
    var interactionList = PotentialInteractions()

    internal val handler = InteractionHandlers
}


internal object InteractionHandlers{
    private val handlers: MutableMap<InteractionMode, (Capability<*>, Entity) -> Interaction<out InteractionMode>>
                          = mutableMapOf()

    fun <C : InteractionMode> register(
        mode: C,
        handler: (Capability<C>, Entity) -> Interaction<C>
    ) {
        handlers[mode] = { cap, target ->
            castCapabilityForMode(cap, mode)?.let { typedCap ->
                handler(typedCap, target)
            } ?: throw IllegalStateException("Capability mode mismatch")
        }
    }

    fun get(mode: InteractionMode): ((Capability<*>, Entity) -> Interaction<out InteractionMode>)? =
        handlers[mode]

    @Suppress("UNCHECKED_CAST")
    private fun <C : InteractionMode> castCapabilityForMode(
        capability: Capability<*>,
        expectedMode: C
    ): Capability<C>? =
        if (capability.mode == expectedMode) capability as Capability<C> else null
}