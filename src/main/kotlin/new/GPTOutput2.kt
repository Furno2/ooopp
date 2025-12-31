package main

// ======================================================
// Core math & grid
// ======================================================

data class Vec2(val x: Int, val y: Int)

class EntityGrid(
    private val width: Int,
    private val height: Int
) {
    private val cells = Array(height) { arrayOfNulls<Entity>(width) }

    fun get(pos: Vec2): Entity? =
        if (pos.x in 0 until width && pos.y in 0 until height)
            cells[pos.y][pos.x]
        else null

    fun set(pos: Vec2, entity: Entity?) {
        if (pos.x in 0 until width && pos.y in 0 until height)
            cells[pos.y][pos.x] = entity
    }
}

// ======================================================
// Interaction model
// ======================================================

enum class InteractionType {
    INTERACT,
    OPEN,
    TALK,
    USE
}

data class InteractionContext(
    val interactor: Interactor,
    val target: Entity,
    val grid: EntityGrid
)

sealed class InteractionValidation {
    data class Valid(val validation: Validation) : InteractionValidation()
    data class Invalid(val failure: InteractionFailure) : InteractionValidation()
}

data class Validation(
    val context: InteractionContext,
    val actions: List<InteractionAction>
)

sealed interface InteractionFailure
object InteractionNotSupported : InteractionFailure
object NoEntityAtTarget : InteractionFailure
object TooFar : InteractionFailure

// ======================================================
// Interaction actions and triggers
// ======================================================

interface InteractionAction {
    val description: String
    fun execute(ctx: InteractionContext)
}

interface Trigger {
    val description: String
    fun apply(ctx: InteractionContext)
}

// ======================================================
// Entities & items
// ======================================================

interface Entity {
    val position: Vec2

    // Base actions
    val baseActions: Map<InteractionType, List<InteractionAction>>

    // Items this entity holds
    val items: List<Item>

    // Dynamic computation of actions including items
    fun actions(type: InteractionType): List<InteractionAction> {
        val itemActions = items.flatMap { it.actions[type].orEmpty() }
        val base = baseActions[type].orEmpty()
        return base + itemActions
    }

    // Triggers that affect this entity
    fun triggers(triggerType: TriggerType): List<Trigger>
}

interface Interactor {
    val position: Vec2
}

class Item(
    val name: String,
    val actions: Map<InteractionType, List<InteractionAction>> = emptyMap(),
    val triggers: Map<TriggerType, List<Trigger>> = emptyMap()
)

enum class TriggerType {
    DAMAGE,
    HEAL,
    BUFF
}

// ======================================================
// Flattened interaction command
// ======================================================

class InteractionCommand(
    private val interactionType: InteractionType,
    private val interactor: Interactor,
    private val targetPos: Vec2,
    private val grid: EntityGrid,
    private val maxDistance: Int = 1
) {

    fun validate(): InteractionValidation {
        val target = grid.get(targetPos) ?: return InteractionValidation.Invalid(NoEntityAtTarget)
        val actions = target.actions(interactionType)
        if (actions.isEmpty()) return InteractionValidation.Invalid(InteractionNotSupported)
        if (!inRange(target)) return InteractionValidation.Invalid(TooFar)
        return InteractionValidation.Valid(
            Validation(InteractionContext(interactor, target, grid), actions)
        )
    }

    fun execute(valid: InteractionValidation.Valid) {
        valid.validation.actions.forEach { it.execute(valid.validation.context) }
    }

    private fun inRange(target: Entity): Boolean {
        val dx = kotlin.math.abs(interactor.position.x - target.position.x)
        val dy = kotlin.math.abs(interactor.position.y - target.position.y)
        return dx + dy <= maxDistance
    }
}

// ======================================================
// Example interaction actions
// ======================================================

class OpenAction(override val description: String, private val action: () -> Unit) : InteractionAction {
    override fun execute(ctx: InteractionContext) = action()
}

class TalkAction(override val description: String, private val action: () -> Unit) : InteractionAction {
    override fun execute(ctx: InteractionContext) = action()
}

class UseItemAction(override val description: String, private val action: () -> Unit) : InteractionAction {
    override fun execute(ctx: InteractionContext) = action()
}

// Example trigger
class DamageTrigger(override val description: String, private val amount: Int) : Trigger {
    override fun apply(ctx: InteractionContext) {
        println("${ctx.target} takes $amount damage from trigger: $description")
    }
}

// ======================================================
// Example entities
// ======================================================

class Player(override val position: Vec2) : Entity, Interactor {
    override val baseActions: Map<InteractionType, List<InteractionAction>> = emptyMap()

    override val items: List<Item> = listOf(
        Item(
            "Potion A",
            actions = mapOf(InteractionType.USE to listOf(UseItemAction("Potion A") { println("Player heals 50 HP") })),
        ),
        Item(
            "Potion B",
            actions = mapOf(InteractionType.USE to listOf(UseItemAction("Potion B") { println("Player heals 30 HP") })),
        )
    )

    override fun triggers(triggerType: TriggerType): List<Trigger> = emptyList()

    override fun toString() = "Player@$position"
}

class Chest(override val position: Vec2) : Entity {
    private var opened = false

    override val baseActions: Map<InteractionType, List<InteractionAction>> = mapOf(
        InteractionType.OPEN to listOf(OpenAction("Chest") { openChest() }),
        InteractionType.INTERACT to listOf(OpenAction("Inspect Chest") { inspectChest() })
    )

    override val items: List<Item> = emptyList()

    override fun triggers(triggerType: TriggerType): List<Trigger> = emptyList()

    private fun openChest() {
        if (!opened) {
            opened = true
            println("Chest opened")
        } else {
            println("Chest already opened")
        }
    }

    private fun inspectChest() = println("You see an old wooden chest")
}

class Npc(override val position: Vec2, private val name: String) : Entity {
    override val baseActions: Map<InteractionType, List<InteractionAction>> = mapOf(
        InteractionType.TALK to listOf(TalkAction("Talk to $name") { talk() }),
        InteractionType.INTERACT to listOf(TalkAction("Inspect $name") { inspect() })
    )

    override val items: List<Item> = emptyList()

    override fun triggers(triggerType: TriggerType): List<Trigger> = emptyList()

    private fun talk() = println("$name says: Hello there!")
    private fun inspect() = println("It's $name")
}

// ======================================================
// Example usage
// ======================================================

fun main() {
    val grid = EntityGrid(10, 10)

    val player = Player(Vec2(5, 5))
    val chest = Chest(Vec2(5, 6))
    val npc = Npc(Vec2(4, 5), "Guard")

    grid.set(player.position, player)
    grid.set(chest.position, chest)
    grid.set(npc.position, npc)

    val commands = listOf(
        InteractionCommand(InteractionType.OPEN, player, Vec2(5, 6), grid),
        InteractionCommand(InteractionType.TALK, player, Vec2(4, 5), grid),
        InteractionCommand(InteractionType.USE, player, Vec2(5, 5), grid) // uses items
    )

    for (cmd in commands) {
        when (val result = cmd.validate()) {
            is InteractionValidation.Valid -> cmd.execute(result)
            is InteractionValidation.Invalid -> println("Interaction failed: ${result.failure}")
        }
    }
}
