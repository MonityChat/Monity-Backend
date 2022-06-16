package de.devin.monity.network.db.util


/**
 * Util interface to help model a database with a link between ID and a TYPE
 * @param TYPE value to link to
 * @param ID id class which links to the type
 */
interface DBManager<TYPE, ID> {

    /**
     * Load the database
     */
    fun load()

    /**
     * Has the given object saved in the database
     * @param id id
     */
    fun has(id: ID): Boolean

    /**
     * Returns the type linked to the ID
     * @param id id
     * @return object linked to id
     */
    fun get(id: ID): TYPE

    /**
     * inserts the obj into the database
     * @param obj object to insert
     */
    fun insert(obj: TYPE)

}