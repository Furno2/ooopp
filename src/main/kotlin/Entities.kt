package org.example

import new.Position
class Item(){
    var name = "Item"
}

class Inventory(){
    var elements=listOf<Item>()
    fun addItem(item: Item){
        elements+=item
    }
    fun getItems():List<Item>{
        return elements
    }
    fun removeItem(item: Item){
        elements-=item
    }
}

abstract class Entity(x:Int,y:Int,protected val world: IWorld?) {
    var pos=Position(x,y)
    abstract val char: Char
    override fun toString(): String {
        return char.toString()
    }
}

abstract class Actor(x:Int,y:Int,world: IWorld?):Entity(x, y,world){
    open fun dispatchInteraction(){
        world?.performInteraction(this,pos+Position(1,0))
    }
    var inventory=Inventory()

}

class Door(x:Int,y:Int,world: IWorld): Entity(x,y,world), GenericInteractable, AllowEntry{
    override var isPassable = false
    override var entityHolder: Entity? = null
    override var char = 'H'
    override fun interact(initiator:Actor) {
        isPassable = !isPassable
        char = if (char.isUpperCase()) char.lowercaseChar() else char.uppercaseChar()
        println("interact Door")
    }
}

class Player(x:Int,y: Int,world: IWorld?): Actor(x,y,world){
    override val char = '@'
    val direction: Position = Position(1,0)
    fun dropItem(item: Item){
        inventory.removeItem(item)
    }

}

class ItemHolder(x:Int,y:Int,world: IWorld): Entity(x,y,world), GenericInteractable, AllowEntry{
    override var isPassable = true
    override var entityHolder: Entity? = null
    override var char = '*'
    val held = Item()
    override fun interact(initiator:Actor) {
        initiator.inventory.addItem(held)

    }
}
object Wall: Entity(0,0,null){
    override val char = '#'
}
