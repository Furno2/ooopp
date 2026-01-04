package project.actions

import project.Actor
import project.Entity
import project.IGrid
import project.Weapon

object OutOfRange : ActionFailure              // Target too far for melee/ranged
object LineOfSightBlocked : ActionFailure      // Ranged attack cannot see target
object InvalidWeapon : ActionFailure           // Weapon cannot perform this attack type

object Attack: ActionMode

class AttackCapability(override val sourceItem: Weapon?, val attackType: AttackType) : Capability
{
    private val _mode: ActionMode = Attack
    override val mode: ActionMode get() = _mode
    override val targetType = TargetType.OTHERS_ONLY
    override fun validateOutside(context: ActionContext): ActionFailure? {
        val castedContext = context as AttackContext

        if (castedContext.sourceItem?.canAttack(castedContext.attackType) == false) {
            return InvalidWeapon
        }

        if (!castedContext.attackType.canAttack(castedContext)) {
            return when (castedContext.attackType) {
                is Melee -> OutOfRange
                is Ranged -> LineOfSightBlocked
                else -> OutOfRange
            }
        }
        return null
    }
}

class AttackContext(
    override val source: Actor,
    override val target: Entity,
    val grid: IGrid,
    override val sourceItem: Weapon?,
    val damage: Int,
    val attackType: AttackType,
) : ActionContext
{
    override val mode = Attack
}

interface AttackType{
    fun canAttack(context: AttackContext): Boolean
}

object Melee: AttackType{
    override fun canAttack(context: AttackContext): Boolean {
        val sourcePosition = context.source.getPosition()
        val targetPosition = context.target.getPosition()
        return sourcePosition.isAdjacent(targetPosition)
    }
}
object Ranged: AttackType{
    override fun canAttack(context: AttackContext): Boolean {
        return true
    }
}

object Other: AttackType{
    override fun canAttack(context: AttackContext): Boolean {
        return true
    }
}
