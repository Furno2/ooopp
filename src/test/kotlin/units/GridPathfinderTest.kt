package test.units

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import project.*

class GridPathfinderTest {

    @Test
    fun `finds straight path on empty grid`() {
        val grid = Grid(5,5)
        val start = Position(0,0)
        val goal = Position(0,3)

        val path = GridPathfinder.findPathOnGrid(grid, start, goal)
        assertNotNull(path)
        assertEquals(start, path!!.first())
        assertEquals(goal, path.last())
        // length should be 4 steps => 4+1 nodes
        assertEquals(4, path.size)
    }

    @Test
    fun `returns null when goal out of bounds`() {
        val grid = Grid(3,3)
        val start = Position(0,0)
        val goal = Position(10,10)
        val path = GridPathfinder.findPathOnGrid(grid, start, goal)
        assertNull(path)
    }

    @Test
    fun `start equals goal returns single node`() {
        val grid = Grid(3,3)
        val start = Position(1,1)
        val path = GridPathfinder.findPathOnGrid(grid, start, start)
        assertNotNull(path)
        assertEquals(1, path!!.size)
        assertEquals(start, path[0])
    }

    @Test
    fun `blocked goal returns null`() {
        val grid = Grid(3,3)
        val start = Position(0,0)
        val goal = Position(2,2)

        // place an entity on the goal
        val blocker = object : project.Entity() {
            override val grid: IGrid = grid
            override var position: Position = goal
            override var char: Char = 'b'
            override fun isAlive(): Boolean = true
            override val interactionDefinitions: List<project.InteractionDefinition> = emptyList()
        }
        grid.setEntity(blocker, goal)

        val path = GridPathfinder.findPathOnGrid(grid, start, goal)
        assertNull(path)
    }

    @Test
    fun `finds path around obstacle`() {
        val grid = Grid(5,5)
        val start = Position(0,0)
        val goal = Position(2,0)

        // place blockers at (1,0) so path must go around
        val blocker = object : project.Entity() {
            override val grid: IGrid = grid
            override var position: Position = Position(1,0)
            override var char: Char = 'x'
            override fun isAlive(): Boolean = true
            override val interactionDefinitions: List<project.InteractionDefinition> = emptyList()
        }
        grid.setEntity(blocker, Position(1,0))

        val path = GridPathfinder.findPathOnGrid(grid, start, goal)
        assertNotNull(path)
        assertTrue(path!!.first() == start && path.last() == goal)
        // ensure path length > straight line (straight would be 2 steps -> size 3)
        assertTrue(path.size > 3)
        // ensure none of path nodes except maybe start/goal are the blocked position
        assertFalse(path.any { it == Position(1,0) })
    }

    @Test
    fun `treats walls as impassable`() {
        val grid = Grid(3,3)
        val start = Position(0,0)
        val goal = Position(2,0)
        // set (1,0) tile to wall: use setTileGrid
        val tiles = Array(grid.getHeight()) { y -> Array(grid.getWidth()) { x -> Tile.Floor } }
        tiles[0][1] = Tile.Wall
        tiles[1][1] = Tile.Wall
        tiles[2][1] = Tile.Wall
        grid.setTileGrid(tiles)

        val path = GridPathfinder.findPathOnGrid(grid, start, goal)
        assertNull(path) // cannot pass through wall
    }
}

