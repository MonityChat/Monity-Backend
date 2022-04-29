package de.devin.monity.db

import de.devin.monity.db.util.DBManager
import io.ktor.html.*
import kotlinx.coroutines.selects.select
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object UserDB: Table(), DBManager {
    val uuid = varchar("user_uuid", 36)
    val name = varchar("user_name", 48)
    val email = varchar("user_email", 320)
    val password = varchar("user_password", 256)
    val salt = varchar("user_salt", 16)
    val confirmed = bool("user_confirmed")

    override val primaryKey = PrimaryKey(uuid)

    override fun load() {
        SchemaUtils.create(UserDB)
    }
}

