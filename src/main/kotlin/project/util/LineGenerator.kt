package project.util

import project.Position

// Direct Zircon imports
import org.hexworks.zircon.api.data.Position as ZPosition
import org.hexworks.zircon.api.util.LineFactory

/**
 * Line generator adapter using Zircon's LineFactory directly.
 *
 * This function converts project.Position to Zircon's Position, uses Zircon to
 * build the line, and converts back.
 *
 * Note: This file now directly depends on Zircon; ensure the dependency in
 * pom.xml matches the Zircon API (2022.1.0-RELEASE is declared in the project).
 */
object LineGenerator {

    fun lineBetween(start: Position, end: Position): List<Position> {
        val zs = ZPosition.create(start.x, start.y)
        val ze = ZPosition.create(end.x, end.y)
        // Use Zircon LineFactory to get positions between zs and ze (inclusive)
        val zline = LineFactory.fromPositions(zs, ze)
        return zline.map { Position(it.x, it.y) }
    }
}
