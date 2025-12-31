package org.example
import java.util.Scanner
//import kotlin.io.core
fun main() {
    var a:Int=10
    val input = Input()
    val scanner = Scanner(System.`in`)
    val size = 5
    print(2*a)
    val tileArrayInner = Array(size){ row ->
        Array(size){ col ->
            if(row == 0 || row == size - 1 ||  col == 0 || col == size - 1) TileSimple.Wall else TileSimple.Floor}}
    val tileArray = TileGrid(size,size).apply{this.fromSimple(tileArrayInner)}
    //tileArray.fromSimple(tileArrayInner)
    var world = World(tileArray)
    world.addEntity(Player(2, 2, world))

    world.addEntity(Door(3,3,world))

    world.printGrid()
    while (true) {
        if (gameLoop(scanner, input, world)) break
    }
}
