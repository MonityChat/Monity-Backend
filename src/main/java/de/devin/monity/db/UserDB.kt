package de.devin.monity.db

import de.devin.monity.db.util.DBManager
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table

object UserDB: Table(), DBManager {
    private val uuid = varchar("user_uuid", 36)
    private val name = varchar("user_name", 48)
    override val primaryKey = PrimaryKey(uuid)

    override fun load() {
        SchemaUtils.create(UserDB)
    }
}

class UserDBManager() {

}


