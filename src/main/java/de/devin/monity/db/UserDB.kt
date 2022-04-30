package de.devin.monity.db

import de.devin.monity.db.util.DBManager
import de.devin.monity.httprouting.UserData
import io.ktor.html.*
import kotlinx.coroutines.selects.select
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object UserDB: Table(), DBManager<UserData, UUID> {
    val uuid = varchar("user_uuid", 36)
    val name = varchar("user_name", 48)
    val email = varchar("user_email", 320)
    val password = varchar("user_password", 64)
    val salt = varchar("user_salt", 64)
    val confirmed = bool("user_confirmed")

    override val primaryKey = PrimaryKey(uuid)

    override fun load() {
        SchemaUtils.create(UserDB)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { uuid eq id.toString() }.count() > 0}
    }

    override fun get(id: UUID): UserData {
        return transaction { select (uuid eq id.toString()).map { UserData(it[name], it[password], it[salt], it[email], id.toString()) } }[0]
    }

    fun hasEmail(email: String): Boolean {
        return transaction { select (UserDB.email eq email).count() > 0 }
    }

    fun hasUserName(userName: String): Boolean {
        return transaction { select {name eq userName}.count() > 0 }
    }

    override fun insert(obj: UserData) {
        transaction {
            UserDB.insert {
                it[name] = obj.username
                it[password] = obj.password
                it[salt] = obj.salt
                it[email] = obj.email
                it[uuid] = obj.uuid
            }
        }
    }
}

