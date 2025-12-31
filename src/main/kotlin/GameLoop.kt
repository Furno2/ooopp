package org.example

import new.Position
import java.util.Scanner


enum class Direction { W, A, S, D, F, P }

class Input {
    private val pressedKeys = mutableSetOf<Direction>()

    fun press(key: Char) {
        when (key.uppercaseChar()) {
            'W' -> pressedKeys += Direction.W
            'A' -> pressedKeys += Direction.A
            'S' -> pressedKeys += Direction.S
            'D' -> pressedKeys += Direction.D
            'F' -> pressedKeys += Direction.F
            'P' -> pressedKeys += Direction.P

        }
    }


    fun isPressed(direction: Direction) = direction in pressedKeys

    fun clear() = pressedKeys.clear()
}

fun gameLoop(scanner: Scanner,input:Input,world: World): Boolean{
    print("Keys pressed: ")
    val line = scanner.nextLine().trim()

    if (line.equals("exit", ignoreCase = true)) return false

    // Clear previous key states
    input.clear()

    // Mark all keys in the line as pressed
    line.forEach { input.press(it) }
    with(world) {
        when{
            input.isPressed(Direction.W) -> moveEntity(0, Position(-1,0))
            input.isPressed(Direction.S) -> moveEntity(0, Position(1,0))
            input.isPressed(Direction.A) -> moveEntity(0, Position(0,-1))
            input.isPressed(Direction.D) -> moveEntity(0, Position(0,1))
            input.isPressed(Direction.F) -> dispatchInteraction(0)
            input.isPressed(Direction.P) -> return true

        }
        printGrid()

    }
    return false
}
