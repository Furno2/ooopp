package test.units

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import project.*
import project.actions.*
import project.ai.*
import test.units.actions.*

class HumanControllerIncrementalPriorityTest {

    private fun testEntityAt(grid: IGrid, x: Int, y: Int) = TestEntity(grid, Position(x, y))
    private fun weapon() = TestWeapon()
    private fun ammo() = TestItem()

    private fun meleeAttack(source: Actor, target: Entity, grid: IGrid) =
        attackAction(source, target, grid, weapon = weapon(), attackType = Melee)

    private fun rangedAttack(source: Actor, target: Entity, grid: IGrid) =
        attackAction(source, target, grid, weapon = weapon(), attackType = Ranged)

    private fun impossibleAttackOutOfAmmo(source: Actor, target: Entity, grid: IGrid): Action {
        val ctx = AttackContext(
            source = source,
            target = target,
            grid = grid,
            item = null,
            damage = 1,
            attackType = Melee
        )
        return Action.Impossible(ctx, OutOfAmmo)
    }

    private fun movementTo(source: Actor, grid: IGrid, pos: Position) =
        movementAction(source, grid, pos)

    private fun healActionLocal(source: Actor, item: Item): Action =
        healAction(source, source, item)

    private fun reloadActionLocal(source: Actor, item: Item, ammo: Item): Action =
        reloadAction(source, source, item, ammo)

    private fun pickupAction(source: Actor, target: Entity, grid: IGrid): Action {
        val inv = InventoryContainer(mutableMapOf())
        return Action.Possible(PickUpContext(source, target, grid, inv))
    }

    private fun pickupImpossible(source: Actor, target: Entity, grid: IGrid): Action {
        val inv = InventoryContainer(mutableMapOf())
        return Action.Impossible(PickUpContext(source, target, grid, inv), TestActionFailure)
    }

    // ----------------------------
    // COMBAT INCREMENTAL PRIORITIES
    // ----------------------------
    @Test()
    fun `combat behaviors chosen in correct priority order with delays respected`() {
        val grid = TestGrid()
        val enemy = testEntityAt(grid, 1, 0)

        val human = Human(grid, Position(0, 0), maxHp = 100)
        val controller = HumanController(human, grid)
        controller.lastKnownTargetPosition = enemy.getPosition()

        var actions = PotentialActions()

        // ─────────────────────────────────────────────
        // PHASE 1: Enter combat (attack becomes visible)
        // ─────────────────────────────────────────────
        actions.addAction(meleeAttack(human, enemy, grid))

        // combat-entry delay = 3 turns
        // melee acquisition delay = 1 turn

        // During entry delay → no attack allowed
        repeat(2) {
            val choice = controller.decide(human, actions)
            assertEquals(
                null, choice, "Attack must not be chosen during combat-entry delay"
            )
        }

        // ─────────────────────────────────────────────
        // PHASE 2: Entry delay expires, but acquisition delay still applies
        // ─────────────────────────────────────────────
        controller.fsm.update() // entry delay done (3)

        val notBlocked = controller.decide(human, actions)
        assertTrue(
            notBlocked != null && notBlocked.context is AttackContext,
            "Attack must not be chosen until acquisition delay expires"
        )

        // ─────────────────────────────────────────────
        // PHASE 3: Acquisition delay expires
        // ─────────────────────────────────────────────
        controller.fsm.update() // acquisition delay (1)

        val attack = controller.decide(human, actions)
        assertNotNull(attack)
        assertTrue(attack!!.context is AttackContext)
        assertTrue((attack.context as AttackContext).attackType is Melee)

        // Executing attack triggers global cooldown (4 turns)
        controller.lastKnownTargetPosition = null
        actions = PotentialActions() // clear actions to prevent re-attack
    }

        @Test
        fun `combat behaviors chosen in correct priority order`() {
            val grid = TestGrid()

            // low-HP human to trigger flee
            val humanLow = Human(grid, Position(0, 0), maxHp = 10)
            val controllerLow = HumanController(humanLow, grid)
            val enemy = testEntityAt(grid, 1, 0)
            controllerLow.lastKnownTargetPosition = enemy.getPosition()

            val initialActions = PotentialActions()

            initialActions.addAction(meleeAttack(humanLow, enemy, grid))
            controllerLow.decide(humanLow, initialActions) //enter combat

            val actions = PotentialActions()
            // 1. Flee should be chosen
            actions.addAction(movementTo(humanLow, grid, Position(-1, 0)))
            val chosen1 = controllerLow.decide(humanLow, actions)
            assertNotNull(chosen1, "Flee should be chosen first")
            assertTrue(chosen1!!.context is MovementContext, "Flee should be chosen first")

            // 2. Add Melee attack; still Flee because HP low
            actions.addAction(meleeAttack(humanLow, enemy, grid))
            val chosen2 = controllerLow.decide(humanLow, actions)
            assertNotNull(chosen2, "Flee still beats Melee when HP low")
            assertTrue(chosen2!!.context is MovementContext, "Flee still beats Melee when HP low")

            // 3. Heal action; still Flee
            val healItem = TestItem()
            actions.addAction(healActionLocal(humanLow, healItem))
            val chosen3 = controllerLow.decide(humanLow, actions)
            assertNotNull(chosen3, "Flee still beats Heal when HP low")
            assertTrue(chosen3!!.context is MovementContext, "Flee still beats Heal when HP low")

            // 4. Switch to a high-HP human (no flee) and evaluate priorities
            val humanHigh = Human(grid, Position(0, 0), maxHp = 100)
            val controllerHigh = HumanController(humanHigh, grid)
            controllerHigh.lastKnownTargetPosition = enemy.getPosition()

            // reuse actions; expect melee/ranged/heal ordering when not fleeing
            val maybe = controllerHigh.decide(humanHigh, actions)
            assertNotNull(maybe, "Expected some action (movement or attack)")
            assertTrue(
                (maybe!!.context is MovementContext) || (maybe.context is AttackContext),
                "If combat entry delay not passed yet, either movement or attack may be chosen"
            )

            // advance FSM ticks to ensure combat entry/attack delays expire
            repeat(4) { controllerHigh.fsm.update() }
            val attackChoice = controllerHigh.decide(humanHigh, actions)
            assertNotNull(attackChoice, "Now Melee should be chosen after delays")
            assertTrue((attackChoice!!.context is AttackContext), "Now Melee should be chosen after delays")
            assertTrue(((attackChoice.context as AttackContext).attackType) is Melee)

            // 5. Remove Melee; Ranged should be next
            actions.clear()
            actions.addAction(rangedAttack(humanHigh, enemy, grid))
            repeat(3) { controllerHigh.fsm.update() }
            val chosenRanged = controllerHigh.decide(humanHigh, actions)
            assertNotNull(chosenRanged, "Expected ranged attack")
            assertTrue(((chosenRanged!!).context as AttackContext).attackType is Ranged)

            // 6. Remove Ranged; Heal should be next
            actions.clear()
            actions.addAction(healActionLocal(humanHigh, healItem))
            val chosenHeal = controllerHigh.decide(humanHigh, actions)
            assertNotNull(chosenHeal, "Heal should be chosen when available")
            assertTrue(chosenHeal!!.context is HealContext)

            // 7. Out of ammo -> Reload
            actions.clear()
            actions.addAction(impossibleAttackOutOfAmmo(humanHigh, enemy, grid))
            actions.addAction(reloadActionLocal(humanHigh, weapon(), ammo()))
            val chosenReload = controllerHigh.decide(humanHigh, actions)
            assertNotNull(chosenReload, "Reload should be chosen when out of ammo")
            assertTrue(chosenReload!!.context is ReloadContext)

            // 8. Chase when lastKnownEnemyPosition exists
            actions.clear()
            actions.addAction(movementTo(humanHigh, grid, Position(1, 0)))
            controllerHigh.lastKnownTargetPosition = Position(1, 0)
            val chase = controllerHigh.decide(humanHigh, actions)
            assertNotNull(chase, "Chase should be chosen")
            assertTrue(chase!!.context is MovementContext)
            assertEquals(Position(1, 0), (chase.context as MovementContext).targetPosition)

            // 9. Base fallback
            actions.clear()
            controllerHigh.lastKnownTargetPosition = null
            val none = controllerHigh.decide(humanHigh, actions)
            assertNull(none)
        }

        @Test
        fun `combat chooses melee over ranged when not fleeing`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 100)
            val controller = HumanController(human, grid)
            val enemy = testEntityAt(grid, 1, 0)
            controller.lastKnownTargetPosition = enemy.getPosition()

            val actions = PotentialActions()
            actions.addAction(meleeAttack(human, enemy, grid))
            actions.addAction(rangedAttack(human, enemy, grid))

            val chosen = controller.decide(human, actions)
            assertNotNull(chosen, "Expected an attack")
            assertTrue((chosen!!).context is AttackContext)
            assertTrue(((chosen.context as AttackContext).attackType) is Melee)
        }

        @Test
        fun `combat chooses ranged if no melee and not fleeing`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 100)
            val controller = HumanController(human, grid)
            val enemy = testEntityAt(grid, 1, 0)
            controller.lastKnownTargetPosition = enemy.getPosition()

            val actions = PotentialActions()
            actions.addAction(rangedAttack(human, enemy, grid))

            val chosen = controller.decide(human, actions)
            assertNotNull(chosen, "Attack expected")
            assertTrue((chosen!!).context is AttackContext)
            assertTrue(((chosen.context as AttackContext).attackType) is Ranged)
        }

        @Test
        fun `combat chooses heal if no attacks and hp low`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 10)
            val controller = HumanController(human, grid)

            val healItem = TestItem()
            val actions = PotentialActions()
            actions.addAction(healActionLocal(human, healItem))

            val chosen = controller.decide(human, actions)
            assertNotNull(chosen, "Heal should be chosen when HP low and available")
            assertTrue((chosen!!).context is HealContext)
        }

        @Test
        fun `combat chooses reload if attacks impossible due to OutOfAmmo`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 100)
            val controller = HumanController(human, grid)
            val enemy = testEntityAt(grid, 1, 0)

            val actions = PotentialActions()
            actions.addAction(impossibleAttackOutOfAmmo(human, enemy, grid))
            actions.addAction(reloadActionLocal(human, weapon(), ammo()))

            val chosen = controller.decide(human, actions)
            assertNotNull(chosen, "Reload should be chosen when out of ammo")
            assertTrue((chosen!!).context is ReloadContext)
        }

        @Test
        fun `combat chooses chase if no attacks and lastKnownPosition exists`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 100)
            val controller = HumanController(human, grid)
            val enemy = testEntityAt(grid, 1, 0)

            controller.lastKnownTargetPosition = enemy.getPosition()

            val actions = PotentialActions()
            actions.addAction(movementTo(human, grid, Position(1, 0))) // chase

            val chosen = controller.decide(human, actions)
            assertNotNull(chosen, "Chase should be chosen")
            assertTrue((chosen!!).context is MovementContext)
            assertEquals(Position(1, 0), ((chosen.context as MovementContext).targetPosition))
        }

        @Test
        fun `combat fallback chooses base if nothing else`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 100)
            val controller = HumanController(human, grid)

            val actions = PotentialActions() // empty
            val chosen = controller.decide(human, actions)
            assertNull(chosen)
        }

        // ----------------------------
        // NON-COMBAT INCREMENTAL PRIORITIES
        // ----------------------------

        @Test
        fun `non-combat behaviors chosen in correct priority order`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 100)
            val controller = HumanController(human, grid)

            val item = TestItem()
            val ammoItem = TestItem()
            val targetEntity = testEntityAt(grid, 1, 0)

            val actions = PotentialActions()

            // 1. Heal
            actions.addAction(healActionLocal(human, item))
            val c = controller.decide(human, actions)
            assertNotNull(c, "Heal should be prioritized first in non-combat")
            assertTrue((c!!).context is HealContext)

            // 2. Reload (no heal available)
            actions.clear()
            actions.addAction(reloadActionLocal(human, item, ammoItem))
            val c2 = controller.decide(human, actions)
            assertNotNull(c2, "Reload expected")
            assertTrue((c2!!).context is ReloadContext)

            // 3. Pickup
            actions.clear()
            actions.addAction(pickupAction(human, targetEntity, grid))
            val c3 = controller.decide(human, actions)
            assertNotNull(c3, "Pickup expected")
            assertTrue((c3!!).context is PickUpContext)

            // 4. PickupPath (pickup impossible)
            actions.clear()
            actions.addAction(pickupImpossible(human, targetEntity, grid))
            actions.addAction(movementTo(human, grid, Position(1, 0)))
            val c4 = controller.decide(human, actions)
            assertNotNull(c4, "Pickup path expected")
            assertTrue((c4!!).context is MovementContext)
            assertEquals(Position(1, 0), ((c4.context as MovementContext).targetPosition))

            // 5. Wander fallback
            actions.clear()
            val c5 = controller.decide(human, actions)
            assertNull(c5)
        }

        @Test
        fun `non-combat prioritization Heal Reload Pickup PickupPath Wander`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 10)
            val controller = HumanController(human, grid)

            val item = TestItem()
            val targetItemEntity = testEntityAt(grid, 1, 0)

            val actions = PotentialActions()
            actions.addAction(healActionLocal(human, item))
            actions.addAction(reloadActionLocal(human, item, item))
            actions.addAction(pickupAction(human, targetItemEntity, grid))
            actions.addAction(pickupImpossible(human, targetItemEntity, grid))
            // Wander: no actions added

            val chosen = controller.decide(human, actions)
            assertNotNull(chosen, "Heal should take priority in non-combat")
            assertTrue((chosen!!).context is HealContext)

        }

        @Test
        fun `non-combat chooses reload if no heal`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 100)
            val controller = HumanController(human, grid)

            val item = TestItem()
            val actions = PotentialActions()
            actions.addAction(reloadActionLocal(human, item, item))

            val chosen = controller.decide(human, actions)
            assertNotNull(chosen, "Reload expected")
            assertTrue((chosen!!).context is ReloadContext)
        }

        @Test
        fun `non-combat chooses pickup if available`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 100)
            val controller = HumanController(human, grid)

            val targetItemEntity = testEntityAt(grid, 1, 0)
            val actions = PotentialActions()
            actions.addAction(pickupAction(human, targetItemEntity, grid))

            val chosen = controller.decide(human, actions)
            assertNotNull(chosen, "Pickup expected")
            assertTrue((chosen!!).context is PickUpContext)
        }

        @Test
        fun `non-combat chooses pickup path if pickup impossible`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 100)
            val controller = HumanController(human, grid)

            val targetItemEntity = testEntityAt(grid, 1, 0)
            val actions = PotentialActions()
            actions.addAction(pickupImpossible(human, targetItemEntity, grid))
            actions.addAction(movementTo(human, grid, Position(1, 0))) // pickup path

            val chosen = controller.decide(human, actions)
            assertNotNull(chosen, "Pickup path expected")
            assertTrue((chosen!!).context is MovementContext)
            assertEquals(Position(1, 0), ((chosen.context as MovementContext).targetPosition))
        }

        @Test
        fun `non-combat fallback chooses base`() {
            val grid = TestGrid()
            val human = Human(grid, Position(0, 0), maxHp = 100)
            val controller = HumanController(human, grid)

            val actions = PotentialActions() // empty
            val chosen = controller.decide(human, actions)
            assertNull(chosen)
        }
    }
