package org.example

import new.Position
import com.sun.org.apache.xpath.internal.operations.Bool

interface IWorld{
    fun moveEntity(entityIndex:Int,delta: Position): Boolean
    fun dispatchInteraction(entityIndex:Int)
    fun performInteraction(self: Actor,position: Position)
    fun addEntity(entity:Entity):Boolean
    fun removeEntity(entity:Entity)
    fun printGrid()
}

class World(
    val grid: IGrid,
) : IWorld{
    val entities: MutableList<Entity> = mutableListOf()

    override fun addEntity(entity:Entity): Boolean{
        if(grid.isPassable(entity.pos)) {
            entities.add(entity)
            grid.setEntity(entity.pos,entity)
            return true
        }
        else{
            println("Cannot add entity ${entity.javaClass.simpleName} at pos ${entity.pos}")
            return false
        }
    }

   override fun moveEntity(entityIndex:Int,delta:Position): Boolean {
        val oldPos = entities[entityIndex].pos
        val newPos = oldPos + delta
        if (grid.isPassable(newPos)) {
            entities[entityIndex].pos = newPos
            grid.setEntity(oldPos,null)
            grid.setEntity(newPos,entities[entityIndex])
        }
       println("$entityIndex: $newPos")
       return true
    }

   override fun dispatchInteraction(entityIndex:Int){
        (entities[entityIndex] as? Actor)?.dispatchInteraction()
        //(grid[pos] as? GenericInteractable)?.interact()
   }

    override fun performInteraction(self: Actor, position: Position){
        (grid.getEntity(position) as? GenericInteractable)?.interact(self)
        println("performInteraction {$position}, ${(grid.getEntity(position) as? GenericInteractable)}")
    }

    override fun removeEntity(entity: Entity) {
        TODO("Not yet implemented")
    }
    override fun printGrid(){
        grid.printGrid()
    }
}


