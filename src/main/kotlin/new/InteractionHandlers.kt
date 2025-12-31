package new

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


    fun get(mode: InteractionMode): ((Capability<*>, Entity) -> Interaction<out InteractionMode>) =
        handlers[mode] ?: throw IllegalArgumentException("Capability mode mismatch")

    @Suppress("UNCHECKED_CAST")
    private fun <C : InteractionMode> castCapabilityForMode(
        capability: Capability<*>,
        expectedMode: C
    ): Capability<C>? =
        if (capability.mode == expectedMode) capability as Capability<C> else null
}