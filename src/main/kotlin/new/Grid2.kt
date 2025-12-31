package new

data class Position(var x:Int, var y:Int) {

    operator fun plus(rhs: Position): Position {
        return Position(x +rhs.x,y+rhs.y)
    }

    override fun toString(): String {
        return "Position(x=$x, y=$y)"
    }
}

interface IGrid{
    fun setEntity(entity: Entity?, position: Position) : Throwable//throws Exception
    fun getEntity(position: Position): Entity?
    fun moveEntity(entity: Entity, newPosition: Position)
    fun getTileType(position: Position): Tile
    fun canEnter(position: Position): Boolean
    //fun setTileMap()
}

class Grid(private val height: Int, private val width: Int) : IGrid {
    private var tileGrid = Array(height) { Array(width){ Tile.Floor } }
    private var grid: Array<Array<Entity?>> = Array(height){Array(width){ null } }

    fun inBounds(position: Position): Boolean{
        return position.x in 0 until width && position.y in 0 until height
    }

    override fun getTileType(position: Position): Tile {
        return if(inBounds(position)){tileGrid[position.y][position.x]} else Tile.Wall
    }

    override fun getEntity(position: Position): Entity?{
        return if(inBounds(position)){grid[position.y][position.x]} else null
    }

    override fun setEntity(entity: Entity?, position: Position) : Throwable{
        //require(inBounds(position)) {"position out of bounds"}
        require(getTileType(position) == Tile.Floor) {"position is not floor"}
        require(getEntity(position) == null || entity == null) {"position is occupied"}
        grid[position.y][position.x] = entity
        return Throwable()
    }


    override fun moveEntity(entity: Entity, newPosition: Position){
        val oldPosition = entity.getPosition()
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
    fun char(): String {
        return when(this){
            Tile.Floor-> "."
            Tile.Wall-> "#"
        }
    }
}
