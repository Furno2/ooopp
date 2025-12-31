package org.example

import new.Position

interface IGrid {
    fun getEntity(position: Position): Entity?
    fun setEntity(position: Position, entity: Entity?) : Boolean
    fun isPassable(newPos: Position): Boolean
    fun printGrid()
    fun fromSimple(tileArray: Array<Array<TileSimple>>)

}

class TileGrid(val width: Int, val height: Int) : IGrid{

    private var data:Array<Array<Tile?>> = Array(width) { Array(height) { Tile(null) } }

     private fun inBounds(position: Position): Boolean {
        return position.x in 0 until width && position.y in 0 until height
    }

     private fun getTile(position: Position): Tile? {
        return if(inBounds(position)) data[position.y][position.x] else null
    }

    // Inside TileGrid class
    override fun getEntity(position: Position): Entity?{
        return getTile(position)?.getEntity()
    }

    override fun setEntity(position: Position, entity: Entity?): Boolean {
        val floor = getTile(position) ?: return false
        val e = entity ?: return false

        floor.setEntity(e)
        return true
    }

    override fun fromSimple(simple: Array<Array<TileSimple>>){ //improper use will cause IndexOutOfBoundExceptions, and I don't care
        for (i in simple.indices) {
            for (j in simple[i].indices) {
                data[i][j] = when(simple[i][j]){
                    TileSimple.Wall -> null
                    TileSimple.Floor -> Tile(null)
                }
            }
        }
    }

    override fun isPassable(position: Position): Boolean {
        return getTile(position)?.isPassable() ?: false
    }

    fun isFloor(position: Position): Boolean {
        return getTile(position) != null
    }

    override fun printGrid(){
        println("   " + data.indices.joinToString(" ") { "%2d".format(it) })
        for (i in data.indices) {
            print("%2d ".format(i))
            for (j in data.indices) {
                print("%2s ".format(data[i][j]?.let{it.toString()} ?: "#"))
            }
            println()
        }
    }
}

class Tile(private var entity: Entity? = null) {
    override fun toString(): String {
        return entity?.toString() ?: "."
    }
    fun setEntity(newEntity: Entity?) {
        entity = newEntity
    }
    fun getEntity(): Entity? {
        return entity
    }

    fun isPassable(): Boolean{
        return this.entity == null
                //val currentEntity = entity ?: return true
                //(currentEntity as? AllowEntry)?.isPassable == true
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if(other !is Tile) return false
        return this.entity == other.entity
    }
}

enum class TileSimple{
    Wall, Floor
}

