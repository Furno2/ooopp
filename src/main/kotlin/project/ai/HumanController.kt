package project.ai

import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.ai.msg.Telegram
import project.*
import project.actions.*

/* =========================
   Flattened view of all actions
   ========================= */

class ActionView(private val all: Map<ActionMode, List<Action>>) {
    val possible: List<Pair<Action.Possible, ActionContext>> =
        all.values.flatten()
            .filterIsInstance<Action.Possible>()
            .map { it to if (it.context is UseItemContext) (it.context as UseItemContext).itemContext else it.context }

    fun anyAttackSeen() = all[AttackMode]?.isNotEmpty() == true
    fun melee() = possible.mapNotNull { (a, c) -> (c as? AttackContext)?.let { a to it } }.firstOrNull { it.second.attackType is Melee }
    fun ranged() = possible.mapNotNull { (a, c) -> (c as? AttackContext)?.let { a to it } }.firstOrNull { it.second.attackType is Ranged }
    fun heal() = possible.firstOrNull { it.second is HealContext }?.first
    fun reload() = possible.firstOrNull { it.second is ReloadContext }?.first
    fun pickup() = possible.firstOrNull { it.second is PickUpContext }?.first
    fun pickupImpossible() = all[PickUpMode]?.isNotEmpty() == true && pickup() == null
    fun attackImpossibleReasons() = all[AttackMode]?.filterIsInstance<Action.Impossible>()?.map { it.reason } ?: emptyList()
    fun movementTo(pos: Position): Action.Possible? =
        all[MovementMode]?.filterIsInstance<Action.Possible>()?.firstOrNull { (it.context as MovementContext).targetPosition == pos }
}

/* =========================
   Behavior interface
   ========================= */

interface Behavior {
    fun produce(controller: HumanController): Action.Possible?
}

/* =========================
   Combat Behaviors
   ========================= */

class CombatFleeBehavior : Behavior {
    override fun produce(controller: HumanController): Action.Possible? {
        if (controller.human.hp > controller.fleeHpThreshold) return null
        val goal = controller.computeFleePosition() ?: return null
        return controller.view.movementTo(goal)
    }
}

class CombatMeleeBehavior : Behavior {
    override fun produce(controller: HumanController): Action.Possible? {
        if (!controller.combatState.canAttack()) return null
        return controller.view.melee()?.first
    }
}

class CombatRangedBehavior : Behavior {
    override fun produce(controller: HumanController): Action.Possible? {
        if (!controller.combatState.canAttack()) return null
        return controller.view.ranged()?.first
    }
}

class CombatHealBehavior : Behavior {
    override fun produce(controller: HumanController): Action.Possible? {
        if (controller.human.hp > controller.healHpThreshold) return null
        return controller.view.heal()
    }
}

class CombatReloadBehavior : Behavior {
    override fun produce(controller: HumanController): Action.Possible? {
        if (!controller.view.attackImpossibleReasons().any { it is OutOfAmmo }) return null
        return controller.view.reload()
    }
}

class CombatChaseBehavior : Behavior {
    override fun produce(controller: HumanController): Action.Possible? {
        val goal = controller.lastKnownTargetPosition ?: return null
        val next = controller.getNextStepTo(goal) ?: return null
        return controller.view.movementTo(next)
    }
}

class CombatBaseBehavior : Behavior {
    override fun produce(controller: HumanController) = null
}

/* =========================
   Non-Combat Behaviors
   ========================= */

class NonCombatHealBehavior : Behavior {
    override fun produce(controller: HumanController) = controller.view.heal()
}

class NonCombatReloadBehavior : Behavior {
    override fun produce(controller: HumanController) = controller.view.reload()
}

class NonCombatPickupBehavior : Behavior {
    override fun produce(controller: HumanController) = controller.view.pickup()
}

class NonCombatPickupPathBehavior : Behavior {
    override fun produce(controller: HumanController): Action.Possible? {
        if (!controller.view.pickupImpossible()) return null
        val goal = controller.lastKnownTargetPosition ?: return null
        val next = controller.getNextStepTo(goal) ?: return null
        return controller.view.movementTo(next)
    }
}

class WanderBehavior : Behavior {
    override fun produce(controller: HumanController) = null
}

/* =========================
   FSM States
   ========================= */

class CombatState(
    private val controller: HumanController,
    private val behaviors: List<Behavior> = listOf(
        CombatFleeBehavior(),
        CombatMeleeBehavior(),
        CombatRangedBehavior(),
        CombatHealBehavior(),
        CombatReloadBehavior(),
        CombatChaseBehavior(),
        CombatBaseBehavior()
    )
) : State<HumanController> {

    private var entryDelay = 3
    private var attackDelay = 1
    private var exitDelay = 4

    fun canAttack() = entryDelay <= 0 && attackDelay <= 0

    override fun enter(entity: HumanController) {
        entryDelay = 3; attackDelay = 1; exitDelay = 4
    }

    override fun update(entity: HumanController) {
        if (entryDelay > 0) entryDelay--
        if (attackDelay > 0) attackDelay--

        controller.currentAction = behaviors.firstNotNullOfOrNull { it.produce(controller) }

        if (!controller.view.anyAttackSeen() && controller.lastKnownTargetPosition == null) {
            if (--exitDelay <= 0) controller.fsm.changeState(controller.nonCombatState)
        } else exitDelay = 4
    }

    override fun exit(entity: HumanController) {}
    override fun onMessage(entity: HumanController, telegram: Telegram) = false
}

class NonCombatState(
    private val controller: HumanController,
    private val behaviors: List<Behavior> = listOf(
        NonCombatHealBehavior(),
        NonCombatReloadBehavior(),
        NonCombatPickupBehavior(),
        NonCombatPickupPathBehavior(),
        WanderBehavior()
    )
) : State<HumanController> {
    override fun update(entity: HumanController) {
        controller.currentAction = behaviors.firstNotNullOfOrNull { it.produce(controller) }
    }
    override fun enter(entity: HumanController) {}
    override fun exit(entity: HumanController) {}
    override fun onMessage(entity: HumanController, telegram: Telegram) = false
}

/* =========================
   Controller
   ========================= */

class HumanController(
    val human: Human,
    val grid: IGrid
) : Controller {

    internal lateinit var view: ActionView
    internal var currentAction: Action.Possible? = null
    internal var lastKnownTargetPosition: Position? = null

    val fleeHpThreshold = 20
    val healHpThreshold = 30

    internal val combatState = CombatState(this)
    internal val nonCombatState = NonCombatState(this)
    internal val fsm: DefaultStateMachine<HumanController, State<HumanController>> = DefaultStateMachine(this, nonCombatState)

    override fun decide(actor: Actor, pa: PotentialActions): Action? {
        view = ActionView(pa.getAllActions())
        if (view.anyAttackSeen() && fsm.currentState != combatState) fsm.changeState(combatState)
        fsm.update()
        return currentAction
    }

    fun computeFleePosition(): Position? {
        val enemy = lastKnownTargetPosition ?: return null
        val me = human.getPosition()
        // placeholder for A* pathfinding
        val next = Position(me.x + (me.x - enemy.x).coerceIn(-1, 1),
            me.y + (me.y - enemy.y).coerceIn(-1, 1))
        return if (grid.canEnter(next)) next else null
    }

    fun getNextStepTo(goal: Position): Position? {
        if (grid !is Grid) return null
        val path = GridPathfinder(grid).findPath(human.getPosition(), goal)
        return path?.getOrNull(1)
    }
}
