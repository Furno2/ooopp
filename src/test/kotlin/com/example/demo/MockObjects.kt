package com.example.demo

import org.example.Actor
import org.example.Entity
import org.example.IGrid
import org.example.Tile
import org.example.TileGrid
import org.example.TileSimple
import org.example.IWorld
import new.Position

class EmptyEntity(): Entity(0,0, EmptyWorld()){
    override val char = '!'
}

class EmptyWorld() : IWorld{
    override fun addEntity(entity: Entity):Boolean {
        TODO("Not yet implemented")
    }
    override fun dispatchInteraction(entityIndex: Int) {

    }

    override fun moveEntity(entityIndex: Int, delta: Position): Boolean {
        TODO("Not yet implemented")
    }

    override fun performInteraction(self: Actor, position: Position) {
        TODO("Not yet implemented")
    }

    override fun printGrid() {
        TODO("Not yet implemented")
    }

    override fun removeEntity(entity: Entity) {
        TODO("Not yet implemented")
    }
}
