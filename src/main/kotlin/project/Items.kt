package project

import project.actions.*


class SimpleWeapon(
    override val damage: Int = 5,
    val range: Int = 3,
    var ammoCount: Int = 30,
    override val maxAmmo: Int = 30
) : Weapon() {
    val ammoType: Ammo? = Ammo()
    private val rangedHandler = InteractionHandler (
        hook = { ammoCount-- },
        validator = {
            if (!hasAmmo()) {
                return@InteractionHandler OutOfAmmo
            }
            else {
                null
            }
        }
    )
    override val capabilities: List<Capability>
        get() = listOf(
            AttackCapability(weapon = this, attackType = Ranged, damage = damage, interactionHandler = rangedHandler),
            AttackCapability(weapon = this, attackType = Melee, damage = damage),
            ReloadCapability(sourceItem = this)
        )

    override fun canAttack(attackType: AttackType): Boolean {
        return when (attackType) {
            Melee -> true
            Ranged -> ammoType != null
            Other -> true
            else -> false
        }
    }

    override fun hasAmmo(): Boolean = ammoType == null || ammoCount > 0

    override fun prepareReload(requested: Int): Int {
        val need = maxAmmo - ammoCount
        if (need <= 0) return 0
        return requested.coerceAtMost(need)
    }

    override fun acceptReload(accepted: Int) {
        if (accepted <= 0) return
        ammoCount = (ammoCount + accepted).coerceAtMost(maxAmmo)
    }
}

class SimpleArmor(
    val armorValue: Int = 1
) : Armor() {
    override val capabilities: List<Capability>
        get() = listOf()

    override val protection: Int get() = armorValue
}

class MedKit(
    val healAmount: Int = 20
) : Item() {
    private val handler = InteractionHandler(
        hook = { _ -> Heal(healAmount) },
        validator = { context ->
            val healContext = context as HealContext
            val human = healContext.target as? Human ?: return@InteractionHandler HealOnNonHuman
            if (human.hp == human.maxHp){
                return@InteractionHandler ActorMaxHp
            }
            else{
                 null
            }
        }
    )
    override val capabilities: List<Capability> = listOf(project.actions.HealCapability(sourceItem = this, interactionHandler = handler))
}

class Ammo(
) : Item() {
    override val capabilities: List<Capability> = listOf()
}

