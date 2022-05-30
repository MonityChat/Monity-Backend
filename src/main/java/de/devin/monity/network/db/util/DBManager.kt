package de.devin.monity.network.db.util

interface DBManager<TYPE, ID> {

    fun load()
    fun has(id: ID): Boolean
    fun get(id: ID): TYPE
    fun insert(obj: TYPE)
    fun update(new: TYPE)

}