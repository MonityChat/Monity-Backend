package de.devin.monity.network.db.user

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.network.httprouting.UserData
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object UserDB: Table("user"), DBManager<UserData, UUID> {
    private val uuid = varchar("user_uuid", 36)
    private val name = varchar("user_name", 48)
    private val email = varchar("user_email", 320)
    private val password = varchar("user_password", 64)
    private val salt = varchar("user_salt", 64)
    private val confirmed = bool("user_confirmed")

    override val primaryKey = PrimaryKey(uuid)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(UserDB)
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
        return transaction { select { name eq userName}.count() > 0 }
    }

    fun getByName(userName: String): UserData {
        return transaction { select (name eq userName).map { UserData(it[name], it[password], it[salt], it[email], it[uuid]) } }[0]
    }

    fun getByEmail(email: String): UserData {
        return transaction { select (UserDB.email eq email).map { UserData(it[name], it[password], it[salt], it[UserDB.email], it[uuid]) } }[0]
    }

    fun hasEmailOrUser(input: String): Boolean {
        return hasEmail(input) || hasUserName(input)
    }

    fun getByUserOrEmail(input: String): UserData {
        return if (hasUserName(input)) getByName(input)
        else getByEmail(input)
    }

    fun getUsersLike(keyWord: String): List<UserData> {
        return transaction { select(name like "%$keyWord%").map { UserData(it[name], it[password], it[salt], it[email], it[uuid]) } }
    }
    override fun insert(obj: UserData) {
        transaction {
            UserDB.insert {
                it[name] = obj.username
                it[password] = obj.password
                it[salt] = obj.salt
                it[email] = obj.email
                it[uuid] = obj.uuid
                it[confirmed] = true
            }
        }
    }
}

