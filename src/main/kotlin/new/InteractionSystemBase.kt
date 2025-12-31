package new
//
//interface CapabilityBase{
//    val mode: InteractionMode
//    val sourceItem: Any?
//    fun validate(context: InteractionContextBase): ValidInteractionBase
//}
//
//sealed interface ValidInteractionBase {
//    //val capability: CapabilityBase
//    //val target: Entity
//    //val context: InteractionContextBase
//    data class PossibleInteraction(val capability: CapabilityBase)
//        : ValidInteractionBase
//
//    data class ImpossibleInteraction(
//        val capability: CapabilityBase,
//        val reason: InteractionFailure
//    ) : ValidInteractionBase
//}
//
//interface InteractionContextBase
//
