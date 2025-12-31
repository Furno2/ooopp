package new


interface InteractionMode
//{
    //fun <C: InteractionMode> handleIncoming(interaction: Interaction.Possible<C>)
//}

interface InteractionContext<C: InteractionMode>{
    val mode: C
    val source : Entity
    val target: Entity
}

interface Capability<out C: InteractionMode>{
    val mode: C
    val source: Any?
    fun validate(context: InteractionContext<in C>): Interaction<C>
}

sealed interface Interaction<out C : InteractionMode>{
    val context: InteractionContext<out C>

    data class Possible<out C : InteractionMode>(
        override val context: InteractionContext<out C>
        ) : Interaction<C>

    data class Impossible<out C : InteractionMode> (
        override val context: InteractionContext<out C>,
        val reason: InteractionFailure
    ) : Interaction<C>
}

interface InteractionFailure

data class PotentialInteractions(
    val actionsByMode: MutableMap<InteractionMode, List<Interaction<out InteractionMode>>> = mutableMapOf()
)
{
    fun addAction(action: Interaction<out InteractionMode>) {
    actionsByMode[action.context.mode] =
        actionsByMode.getOrDefault(action.context.mode, emptyList()) + action
}

    fun getActionsForMode(mode: InteractionMode): List<Interaction<out InteractionMode>> {
        return actionsByMode[mode] ?: emptyList()
    }
}
