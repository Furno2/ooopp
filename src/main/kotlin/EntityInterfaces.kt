package org.example

interface GenericInteractable{
    fun interact(initiator: Actor)
}

interface AllowEntry{
    val isPassable: Boolean
    var entityHolder: Entity?
    fun enter(entity: Entity){
        entityHolder = entity
    }
    fun exit(entity: Entity){

    }
}