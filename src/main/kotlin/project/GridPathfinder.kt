package project


import java.util.PriorityQueue
import kotlin.math.abs

// Object-oriented A* Pathfinder for a Grid
class GridPathfinder(private val grid: Grid) {

    // Public API: find a path from start to goal (inclusive) or null if impossible
    fun findPath(start: Position, goal: Position): List<Position>? {
        if (!grid.inBounds(start) || !grid.inBounds(goal)) return null
        if (!grid.canEnter(goal) && start != goal) return null
        if (start == goal) return listOf(start)

        val astar = AStar(start, goal)
        return astar.search()
    }

    // ----------------------------
    // Heuristic
    // ----------------------------
    protected open fun heuristic(a: Position, b: Position): Int {
        return abs(a.x - b.x) + abs(a.y - b.y) // Manhattan distance
    }

    // ----------------------------
    // Neighbor generator
    // ----------------------------
    protected open fun neighbors(pos: Position, goal: Position): List<Position> {
        val directions = listOf(Position(1,0), Position(-1,0), Position(0,1), Position(0,-1))
        return directions.map { pos + it }
            .filter { grid.inBounds(it) && (it == goal || grid.canEnter(it)) }
    }

    // ----------------------------
    // Node class
    // ----------------------------
    private inner class Node(val pos: Position, val g: Int, val h: Int) : Comparable<Node> {
        val f: Int get() = g + h
        override fun compareTo(other: Node) = f.compareTo(other.f)
    }

    // ----------------------------
    // A* search encapsulated
    // ----------------------------
    private inner class AStar(private val start: Position, private val goal: Position) {
        private val open = PriorityQueue<Node>()
        private val cameFrom = mutableMapOf<Position, Position>()
        private val gScore = mutableMapOf<Position, Int>().withDefault { Int.MAX_VALUE }
        private val closed = mutableSetOf<Position>()

        fun search(): List<Position>? {
            gScore[start] = 0
            open.add(Node(start, 0, heuristic(start, goal)))

            while (open.isNotEmpty()) {
                val current = open.poll().pos
                if (current == goal) return reconstructPath(current)

                if (!closed.add(current)) continue
                val currentG = gScore.getValue(current)

                for (neighbor in neighbors(current, goal)) {
                    if (closed.contains(neighbor)) continue
                    val tentativeG = currentG + 1
                    if (tentativeG < gScore.getValue(neighbor)) {
                        cameFrom[neighbor] = current
                        gScore[neighbor] = tentativeG
                        open.add(Node(neighbor, tentativeG, heuristic(neighbor, goal)))
                    }
                }
            }

            return null
        }

        private fun reconstructPath(end: Position): List<Position> {
            val path = mutableListOf<Position>()
            var current: Position? = end
            while (current != null) {
                path.add(current)
                current = cameFrom[current]
            }
            path.reverse()
            return path
        }
    }

    companion object {
        fun findPathOnGrid(grid: Grid, start: Position, goal: Position): List<Position>? {
            return GridPathfinder(grid).findPath(start, goal)
        }
    }
}
