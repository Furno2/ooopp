package test.units

import project.Entity
import project.Grid
import project.Position
import project.Tile
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class GridTest {
    @Test
    fun `successful setEntity`(){
        val grid = Grid(2, 2)
        val entity = EmptyEntity(Position(1,1),grid)
        grid.setEntity(entity, Position(1, 1))
    }
    @Test()
    fun `multiple setEntity on same Position`(){
        assertThrows<IllegalArgumentException> {
            val grid = Grid(2, 2)
            val entity = EmptyEntity(Position(1,1),grid)
            grid.setEntity(entity, Position(1, 1))
            grid.setEntity(entity, Position(1, 1))
        }
    }
    @Test()
    fun `different entities multiple setEntity on different Positions`(){
        val grid = Grid(2, 2)
        val entity = EmptyEntity(Position(1,1),grid)
        grid.setEntity(entity, Position(1, 1))
        val entity2 = EmptyEntity(Position(1,0),grid)
        grid.setEntity(entity2, Position(1, 0))
    }
    @Test()
    fun `different entities multiple setEntity on same Positions`(){
        assertThrows<IllegalArgumentException> {
            val grid = Grid(2, 2)
            val entity = EmptyEntity(position = Position(1,1),grid)
            grid.setEntity(entity, Position(1, 1))
            val entity2 = EmptyEntity(position = Position(1,1),grid)
            grid.setEntity(entity2, Position(1, 1))
        }
    }
    @Test()
    fun `setEntity out of bounds`(){
        assertThrows<IllegalArgumentException> {
            val grid = Grid(2, 2)
            val entity = EmptyEntity(position = Position(1,2),grid)
            grid.setEntity(entity, Position(1, 2))
        }
    }
    @Test()
    fun `getEntity on empty Tile`(){
        val grid = Grid(2, 2)
        val empty: Entity? = grid.getEntity(Position(1, 1))
        assertEquals(empty, null)
    }
    @Test()
    fun `getEntity on Entity`(){
        val grid = Grid(2, 2)
        val entity = EmptyEntity(position = Position(1,1),grid)
        grid.setEntity(entity, Position(1, 1))
        val entity2: Entity? = grid.getEntity(Position(1, 1))
        assertEquals(entity, entity2)
    }
    @Test()
    fun `getEntity on out of bounds`(){
        val grid = Grid(2, 2)
        val empty = grid.getEntity(Position(1, 3))
        assertEquals(empty, null)
    }
    @Test()
    fun `moveEntity on empty Tile`(){
        val grid = Grid(2, 2)
        val entity = EmptyEntity(Position(1,0),grid)
        grid.setEntity(entity, Position(1, 0))
        grid.moveEntity(entity, Position(1, 1))
        assertEquals(entity, grid.getEntity(Position(1, 1))!!)
    }
    @Test()
    fun `moveEntity on non empty Tile`(){
        assertThrows<IllegalArgumentException> {
            val grid = Grid(2, 2)
            val entity = EmptyEntity(Position(1,0),grid)
            val entity2 = EmptyEntity(Position(1,1),grid)
            grid.setEntity(entity, Position(1, 0))
            grid.setEntity(entity2, Position(1, 1))
            grid.moveEntity(entity, Position(1, 1))
        }
    }
    @Test()
    fun `moveEntity on initial Tile`(){
        assertThrows<IllegalArgumentException> {
            val grid = Grid(2, 2)
            val entity = EmptyEntity(Position(1,0),grid)
            grid.setEntity(entity, Position(1, 0))
            grid.moveEntity(entity, Position(1, 0))
        }
    }
    @Test()
    fun `moveEntity out of bounds`(){
        assertThrows<IllegalArgumentException> {
            val grid = Grid(2, 2)
            val entity = EmptyEntity(Position(1,0),grid)
            grid.setEntity(entity, Position(1, 0))
            grid.moveEntity(entity, Position(2, 0))
        }
    }
    @Test()
    fun `isNonNullTile on NonNullTile`(){
        val grid = Grid(2, 2)
        val tile = grid.getTileType(Position(1, 1))
        assertEquals(tile, Tile.Floor)
    }
    @Test()
    fun `isNonNullTile on outOfBounds`(){
        val grid = Grid(2, 2)
        val tile = grid.getTileType(Position(1, 2))
        assertEquals(tile, Tile.Wall)
    }
    @Test()
    fun `isNonNullTile on NullTile`(){
        throw Exception("TODO")
    }



}