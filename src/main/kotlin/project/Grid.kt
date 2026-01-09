package project

import kotlin.math.abs

interface IGrid{
    val width: Int
    val height: Int
    fun setEntity(entity: Entity?, position: Position)
    fun getEntity(position: Position): Entity?
    fun moveEntity(entity: Entity, oldPosition: Position, newPosition: Position)
    fun getTileType(position: Position): Tile
    fun canEnter(position: Position): Boolean
    //fun setTileMap()
}

class Grid(override val height: Int, override val width: Int) : IGrid {
    private var tileGrid = Array(height) { Array(width){ Tile.Floor } }
    private var grid: Array<Array<Entity?>> = Array(height){Array(width){ null } }

    fun inBounds(position: Position): Boolean {
        val (x, y) = position
        return x in 0 until width && y in 0 until height
    }

    override fun getTileType(position: Position): Tile {
        return if(inBounds(position)){tileGrid[position.y][position.x]} else Tile.Wall
    }

    override fun getEntity(position: Position): Entity?{
        return if(inBounds(position)){grid[position.y][position.x]} else null
    }

    override fun setEntity(entity: Entity?, position: Position)  {
        //require(inBounds(position)) {"position out of bounds"}
        require(getTileType(position) == Tile.Floor) {"position is not floor"}
        require(getEntity(position) == null || entity == null) {"position is occupied"}
        grid[position.y][position.x] = entity
    }


    override fun moveEntity(entity: Entity,oldPosition: Position, newPosition: Position){
        require(getEntity(oldPosition) == entity) {"entity not at its recorded position"}
        setEntity(entity, newPosition)
        setEntity(null, oldPosition)
    }

    override fun canEnter(position: Position): Boolean{
        return getTileType(position) == Tile.Floor && getEntity(position) == null

    }

    fun printGrid(){
        println("   " + grid.indices.joinToString(" ") { "%2d".format(it) })
        for (i in grid.indices) {
            print("%2d ".format(i))
            for (j in grid.indices) {
                print("%2s ".format((grid[i][j] ?: tileGrid[i][j]).toString()))
            }
            println()
        }
    }
    fun setTileGrid(tileGridInput: Array<Array<Tile>>){
        for (i in tileGrid.indices) {
            for (j in tileGrid[i].indices) {
               tileGrid[i][j] = tileGridInput[i][j]
            }
        }
    }

}

enum class Tile{
    Floor{
        override fun toString(): String = "."
    }, Wall{
        override fun toString(): String = "#"
    };
}
data class Position(val x:Int,val y:Int) {

    operator fun plus(rhs: Position): Position {
        return Position(x +rhs.x,y+rhs.y)
    }

    override fun toString(): String {
        return "Position(x=$x, y=$y)"
    }

    fun isAdjacent(other: Position): Boolean {
        return (x == other.x && abs(y - other.y) == 1) || (y == other.y && abs(x - other.x) == 1)
    }

    // Manhattan distance in tiles
    fun manhattanDistance(other: Position): Int {
        return abs(x - other.x) + abs(y - other.y)
    }
}
