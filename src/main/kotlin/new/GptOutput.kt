package redo

import kotlin.math.abs

// ======================================================
// Core math & grid
// ======================================================

data class Vec2(val x: Int, val y: Int) {
    fun distanceTo(other: Vec2) = abs(x - other.x) + abs(y - other.y)
}

class EntityGrid(private val width: Int, private val height: Int) {
    private val cells = Array(height) { arrayOfNulls<Entity>(width) }

    fun get(pos: Vec2): Entity? = if (pos.x in 0 until width && pos.y in 0 until height) cells[pos.y][pos.x] else null
    fun set(pos: Vec2, entity: Entity?) { if (pos.x in 0 until width && pos.y in 0 until height) cells[pos.y][pos.x] = entity }
}

// ======================================================
// Interaction Modes & Validation
// ======================================================

enum class InteractionMode { OPEN, SHOOT, PICK_UP, TALK }

sealed interface ValidationResult<C : InteractionContext> {
    data class Valid<C : InteractionContext>(val context: C) : ValidationResult<C> //things you can do right now
    data class Feasible<C : InteractionContext>(val context: C, val reason: InteractionFailure) :
        ValidationResult<C> //things that are valid, but not curr. possible
    data class Invalid<C : InteractionContext>(val reason: InteractionFailure) :
        ValidationResult<C> //invalid is not available for entities, things that
}                           //physically impossible like opening an item. separate later

sealed interface InteractionFailure
object OutOfRange : InteractionFailure
object Locked : InteractionFailure
object NoTarget : InteractionFailure
object TargetNotVisible : InteractionFailure
object InvalidItem : InteractionFailure

// ======================================================
// Contexts & Commands
// ======================================================

interface InteractionContext

interface InteractionCommand<C : InteractionContext> {
    fun validate(): ValidationResult<C>
    fun execute(context: C)
}

// ======================================================
// Capabilities & Entities
// ======================================================

interface Capability<C : InteractionContext> {
    val mode: InteractionMode
    val description: String
    fun createCommand(): InteractionCommand<C>
}

interface Entity {
    val position: Vec2
    val modes: Set<InteractionMode>
    val capabilities: List<Capability<out InteractionContext>>
    val items: List<Item>
}

// ======================================================
// Items
// ======================================================

class Item(val name: String)

// ======================================================
// Example Contexts
// ======================================================

data class ShootContext(
    val shooter: Entity,
    val target: Entity,
    val weapon: Item,
    val distance: Int,
    val lineOfSight: Boolean,
    val damage: Int
) : InteractionContext

data class OpenChestContext(
    val actor: Entity,
    val chest: Chest,
    val key: Item?
) : InteractionContext

// ======================================================
// Example Commands
// ======================================================

class ShootCommand(
    private val shooter: Entity,
    private val target: Entity,
    private val weapon: Item,
    private val damage: Int,
    private val grid: EntityGrid
) : InteractionCommand<ShootContext> {

    override fun validate(): ValidationResult<ShootContext> {
        val distance = shooter.position.distanceTo(target.position)
        val los = computeLineOfSight(shooter.position, target.position)
        val context = ShootContext(shooter, target, weapon, distance, los, damage)
        return when {
            !los -> ValidationResult.Feasible(context, TargetNotVisible)
            distance > weaponRange(weapon) -> ValidationResult.Feasible(context, OutOfRange)
            else -> ValidationResult.Valid(context)
        }
    }

    override fun execute(context: ShootContext) {
        println("${context.shooter} shoots ${context.target} with ${context.weapon.name} for ${context.damage} damage")
    }

    private fun computeLineOfSight(from: Vec2, to: Vec2) = true // simplified
    private fun weaponRange(item: Item) = 5
}

class OpenChestCommand(
    private val actor: Entity,
    private val chest: Chest,
    private val key: Item?
) : InteractionCommand<OpenChestContext> {

    override fun validate(): ValidationResult<OpenChestContext> {
        val context = OpenChestContext(actor, chest, key)
        return if (chest.locked && key == null) ValidationResult.Invalid(Locked)
        else ValidationResult.Valid(context)
    }

    override fun execute(context: OpenChestContext) {
        chest.open()
    }
}

// ======================================================
// Example Capabilities
// ======================================================

class ShootCapability(
    private val shooter: Entity,
    private val target: Entity,
    private val weapon: Item,
    private val damage: Int,
    private val grid: EntityGrid
) : Capability<ShootContext> {
    override val mode = InteractionMode.SHOOT
    override val description = "Shoot ${target} with ${weapon.name}"
    override fun createCommand(): InteractionCommand<ShootContext> = ShootCommand(shooter, target, weapon, damage, grid)
}

class OpenChestCapability(
    private val actor: Entity,
    private val chest: Chest,
    private val key: Item?
) : Capability<OpenChestContext> {
    override val mode = InteractionMode.OPEN
    override val description = "Open chest ${chest.position} with key ${key?.name ?: "none"}"
    override fun createCommand(): InteractionCommand<OpenChestContext> = OpenChestCommand(actor, chest, key)
}

// ======================================================
// Example Entities
// ======================================================

class Player(
    override val position: Vec2,
    override val items: List<Item>,
    private val grid: EntityGrid
) : Entity {
    override val modes = setOf(InteractionMode.SHOOT)
    override val capabilities: List<Capability<out InteractionContext>>
        get() {
            val caps = mutableListOf<Capability<out InteractionContext>>()
            // Add shoot capabilities for each gun + target
            val targets = listOf<NPC>() // would query grid or known entities
            for (weapon in items) {
                for (target in targets) {
                    caps.add(ShootCapability(this, target, weapon, 10, grid))
                }
            }
            return caps
        }

    override fun toString() = "Player@$position"
}

class Chest(override val position: Vec2, val locked: Boolean) : Entity {
    override val modes = setOf(InteractionMode.OPEN)
    override val items = emptyList<Item>()
    override val capabilities = emptyList<Capability<out InteractionContext>>()
    fun open() { println("Chest at $position opened") }
    override fun toString() = "Chest@$position"
}

class NPC(override val position: Vec2) : Entity {
    override val modes = setOf(InteractionMode.SHOOT, InteractionMode.TALK)
    override val items = emptyList<Item>()
    override val capabilities = emptyList<Capability<out InteractionContext>>()
    override fun toString() = "NPC@$position"
}

// ======================================================
// Interaction evaluation helper
// ======================================================

fun <C : InteractionContext> executeCapability(cap: Capability<C>) {
    val command = cap.createCommand()
    when (val result = command.validate()) {
        is ValidationResult.Valid -> command.execute(result.context)
        is ValidationResult.Feasible -> println("Action feasible but not valid: ${cap.description} reason: ${result.reason}")
        is ValidationResult.Invalid -> println("Action invalid: ${cap.description} reason: ${result.reason}")
    }
}

// ======================================================
// Example usage
// ======================================================

fun main() {
    val grid = EntityGrid(10, 10)
    val player = Player(Vec2(5, 5), items = listOf(Item("Pistol"), Item("Key")), grid = grid)
    val chest = Chest(Vec2(5, 6), locked = true)
    val npc = NPC(Vec2(7, 5))

    grid.set(player.position, player)
    grid.set(chest.position, chest)
    grid.set(npc.position, npc)

    // Example: Open chest capability
    val openCap = OpenChestCapability(player, chest, key = player.items.firstOrNull { it.name == "Key" })
    executeCapability(openCap)

    // Example: Shooting capability
    val shootCap = ShootCapability(player, npc, weapon = player.items.first { it.name == "Pistol" }, damage = 10, grid = grid)
    executeCapability(shootCap)
}
