package com.example.demo
import org.example.Entity
import new.Position
import org.example.TileGrid
import org.example.Tile
import org.example.TileSimple
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GridTest {
    //@Test
//    fun inBounds() {
//        val grid = TileGrid(1,1)
//        val resultInBounds = grid.inBounds(Position(0, 0))
//        assertTrue(resultInBounds)
//    }
//    @Test
//    fun outBounds() {
//        val grid = TileGrid(1,1)
//        val resultNotInBounds = grid.inBounds(Position(3, 3))
//        assertFalse(resultNotInBounds)
//    }
//    @Test
//    fun getTileInBounds(){
//        val grid = TileGrid(1,1)
//        val tileInBounds = grid.getTile(Position(0, 0))
//        val expected = Tile(null)
//        assertEquals(expected,tileInBounds)
//    }
//    @Test
//    fun getTileOutBounds(){
//        val grid = TileGrid(1,1)
//        val tileOutBounds = grid.getTile(Position(3, 3))
//        assertEquals(tileOutBounds,null)
//    }
    @Test
    fun setGetEntity(){
        val grid = TileGrid(1,1)
        val validEntity = EmptyEntity()
        val setSuccess = grid.setEntity(Position(0,0), validEntity)
        assertTrue(setSuccess)
        val getEntitySuccess = grid.getEntity(Position(0,0))
        assertEquals(getEntitySuccess,validEntity)
    }
    @Test
    fun nullGetEntity(){
        val grid = TileGrid(1,1)
        val nullEntity = grid.getEntity(Position(1,1))
        assertEquals(null,nullEntity)
    }
    @Test
    fun nullGetEntityWall(){
        val grid = TileGrid(1,1)
        val wallGrid: Array<Array<TileSimple>> = arrayOf(arrayOf(TileSimple.Wall))
        grid.fromSimple(wallGrid)
        val getEntityOnWall = grid.getEntity(Position(0,0))
        assertEquals(null,getEntityOnWall)
    }
    @Test
    fun outBoundsSetEntity(){
        val grid = TileGrid(1,1)
        val entity = EmptyEntity()
        val setFailure = grid.setEntity(Position(3,1), entity)
        assertFalse(setFailure)
    }
}

class TileTests{
//    @Test
//    fun asFloorOnFloor(){
//        val floor = Tile(null)
//        val floorOnFloor = floor.asFloor()
//        assertEquals(floor,floorOnFloor)
//    }
//    @Test
//    fun asFloorOnWall(){
//        val wall = Tile
//        val wallOnFloor = wall
//        assertEquals(null,wallOnFloor)
//    }

}