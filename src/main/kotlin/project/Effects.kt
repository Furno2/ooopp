package project

interface HookReturnValue

object NoReturnValue : HookReturnValue
sealed interface HumanEffect : HookReturnValue



data class Heal(val amount: Int) : HumanEffect

// data class Eat(val nutrition: Int) : HumanEffect  // commented as requested

data class Reload(
    val ammoType: AmmoType,
    val amount: Int
) : HumanEffect
