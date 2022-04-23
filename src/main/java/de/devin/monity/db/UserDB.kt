package de.devin.monity.db

import org.jetbrains.exposed.sql.Table

object UserDB: Table() {
    private val uuid = varchar("user_uuid", 36)
    private val name = varchar("user_name", 48)
    override val primaryKey = PrimaryKey(uuid)
}

class UserDBManager() {

}


