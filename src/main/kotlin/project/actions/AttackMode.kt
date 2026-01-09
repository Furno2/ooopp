package project.actions

import project.Actor
import project.Entity
import project.IGrid
import project.InteractionDefinition
import project.InteractionHandler
import project.SimpleWeapon
import project.Weapon

object AttackMode: ActionMode

interface AttackFailure : ActionFailure
object OutOfRange : AttackFailure              // Target too far for melee/ranged
object LineOfSightBlocked : AttackFailure      // Ranged attack cannot see target
object InvalidWeapon : AttackFailure           // Weapon cannot perform this attack type
object OutOfAmmo : AttackFailure              // New: weapon has no ammo (definition-only)

data class AttackContext(
    override val source: Actor,
    override val target: Entity,
    override val item: Weapon?,
    val grid: IGrid,
    val damage: Int,
    val attackType: AttackType,
) : ActionContext {
    override val mode = AttackMode
    override var capability: Capability? = null
}

data class AttackCapability(
    val weapon: Weapon?,
    val attackType: AttackType,
    val damage: Int,
    override val interactionHandler: InteractionHandler? = null,
) : Capability {
    override val mode = AttackMode
    override val targetType = TargetType.OTHERS_ONLY
    override val targetSelector: TargetSelector = SelfTargetSelector() // TODO
    override val sourceItem: Weapon? get() = weapon
    override fun validateOutside(context: ActionContext): ActionFailure? {
        val castedContext = context as AttackContext
        if(castedContext.item != weapon){
            return ItemMismatchBetweenContextAndCapability
        }
        with (castedContext){
            if (weapon?.canAttack(attackType) == false) {
                return InvalidWeapon
            }
            return attackType.checkFeasibility(castedContext)
        }
    }

    // TODO: provide a specialized TargetSelector (range/LOS/awareness) when implementing
    // attack targeting beyond the default targetType semantics.

}

interface AttackType{
    fun checkFeasibility(context: AttackContext): AttackFailure?
}

object Melee: AttackType{
    override fun checkFeasibility(context: AttackContext): AttackFailure? {
        val sourcePosition = context.source.getPosition()
        val targetPosition = context.target.getPosition()
        return if(sourcePosition.isAdjacent(targetPosition)){
            null
        }
        else{
            OutOfRange
        }
    }
}
object Ranged: AttackType{
    override fun checkFeasibility(context: AttackContext): AttackFailure? {
        val sourcePosition = context.source.getPosition()
        val targetPosition = context.target.getPosition()
        val weapon = context.item
        // Check range using manhattanDistance
        val range: Int = (weapon as? SimpleWeapon)?.range ?: 1 // Default to 1 if not available
        if (sourcePosition.manhattanDistance(targetPosition) > range) {
            return OutOfRange
        }
        if (weapon is Weapon && !weapon.hasAmmo()) {
            return OutOfAmmo
        }
        // Check line of sight (stubbed as always true, TODO: implement)
        if (weapon is Weapon && weapon.ammoType != null) {
            val ammoCount = weapon.getAmmoCount()
            if (ammoCount <= 0) {
                return OutOfAmmo
            }
        }
        return null
    }
}
object Other: AttackType{
    override fun checkFeasibility(context: AttackContext): AttackFailure? {
        // Default: always feasible
        return null
    }
}
