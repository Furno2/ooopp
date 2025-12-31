package new

interface InteractionMode

interface Capability<C: InteractionMode> : CapabilityBase{
    override val mode: C
    override val sourceItem: Any?
    fun validate(context: InteractionContext<C>): ValidInteraction<C>?
    fun createCommand(): InteractionCommand<out C>
}

interface InteractionContext<C: InteractionMode> : InteractionContextBase

sealed interface ValidInteraction<out C : InteractionMode> : ValidInteractionBase {
    val capability: Capability<out C>
    val context: InteractionContext<out C>
    data class PossibleInteraction<out C : InteractionMode>(
        override val capability: Capability<out C>,
        val target: Entity,
        override val context: InteractionContext<out C>)
        : ValidInteraction<C>

    data class ImpossibleInteraction<out C : InteractionMode> (
        override val capability: Capability<out C>,
        val target: Entity,
        override val context: InteractionContext<out C>,
        val reason: InteractionFailure
    ) : ValidInteraction<C>
}

interface InteractionCommand<C: InteractionMode>{
    fun execute(context: ValidInteraction.PossibleInteraction<C>)
}

interface InteractionFailure



data class PotentialInteractions(
    val actionsByMode: MutableMap<InteractionMode, List<ValidInteraction<out InteractionMode>>> = mutableMapOf()
)
{
    fun addAction(action: ValidInteraction<out InteractionMode>) {
    actionsByMode[action.capability.mode] =
        actionsByMode.getOrDefault(action.capability.mode, emptyList()) + action
}

    fun getActionsForMode(mode: InteractionMode): List<ValidInteraction<out InteractionMode>> {
        return actionsByMode[mode] ?: emptyList()
    }
}
