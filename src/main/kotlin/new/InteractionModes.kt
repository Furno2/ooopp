package new

object MovementFailure: InteractionFailure

object Movement: InteractionMode

class MovementCapability(override val source: Entity) : Capability<Movement>
{
    override val mode = Movement
    override fun validate(context: InteractionContext<in Movement>): Interaction<Movement> {
        val castedContext = context as MovementContext
        return if(castedContext.grid.canEnter(castedContext.targetPosition)){
            Interaction.Possible<Movement>(
                castedContext
            )
        }
        else{
            Interaction.Impossible<Movement>(
                castedContext,
                MovementFailure
            )
        }
    }
}

class MovementContext(
    override val source: Entity,
    val grid: IGrid,
    val targetPosition: Position
) : InteractionContext<Movement>
{
    override val mode = Movement
    override val target = source
}
