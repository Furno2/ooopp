package project

interface HookReturnValue

sealed interface HumanEffect : HookReturnValue



data class Heal(val amount: Int) : HumanEffect

// data class Eat(val nutrition: Int) : HumanEffect  // commented as requested

data class ReloadRequest(
    val ammoType: Ammo,
    val requested: Int,
    val weapon: Weapon
) : HumanEffect
