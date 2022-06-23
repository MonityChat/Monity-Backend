package de.devin.monity.network.db.user

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.network.httprouting.UserData
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


/**
 * Contains all data around
 * @see UserData
 */
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
        return transaction { select (uuid eq id.toString()).map { UserData(it[name], it[password], it[salt], it[email], id) } }[0]
    }

    /**
     * Whether the email is used or not
     * @param email the email
     * @return true if exists false otherwise
     */
    fun hasEmail(email: String): Boolean {
        return transaction { select (UserDB.email eq email).count() > 0 }
    }

    /**
     * Whether the username is used or not
     * @param userName the email
     * @return true if exists false otherwise
     */
    fun hasUserName(userName: String): Boolean {
        return transaction { select { name eq userName}.count() > 0 }
    }

    /**
     * Returns the userData based on the given name
     * @param userName the name of the user
     * @return userdata
     */
    fun getByName(userName: String): UserData {
        return transaction { select (name eq userName).map { UserData(it[name], it[password], it[salt], it[email], UUID.fromString(it[uuid])) } }[0]
    }

    /**
     * return the userData based on the given email
     * @param email the email of the user
     * @return userdata
     */
    fun getByEmail(email: String): UserData {
        return transaction { select (UserDB.email eq email).map { UserData(it[name], it[password], it[salt], it[UserDB.email], UUID.fromString(it[uuid])) } }[0]
    }

    /**
     * Checks if whether the email or the username are already existing
     * @param input either email or username
     * @return true if exists false otherwise
     */
    fun hasEmailOrUser(input: String): Boolean {
        return hasEmail(input) || hasUserName(input)
    }

    /**
     * Gets the user on either the email or the name depending on what exists
     * @param input either email or username
     * @return userdata
     */
    fun getByUserOrEmail(input: String): UserData {
        return if (hasUserName(input)) getByName(input)
        else getByEmail(input)
    }

    /**
     * Updates the password and salt for the user
     * @param uuid of the user
     * @param newPassword of the user
     * @param newSalt of the user
     */
    fun updatePasswordAndSalt(uuid: UUID, newPassword: String, newSalt: String) {
        transaction {
            update({UserDB.uuid eq uuid.toString()}) {
                it[password] = newPassword
                it[salt] = newSalt
            }
        }
    }

    /**
     * Searches for users whose name contains the keyword
     * @param keyWord of the user
     * @return list of all users containing the keyword
     */
    fun getUsersLike(keyWord: String): List<UserData> {
        return transaction { select(name like "%$keyWord%").map { UserData(it[name], it[password], it[salt], it[email], UUID.fromString(it[uuid])) } }
    }
    override fun insert(obj: UserData) {
        transaction {
            UserDB.insert {
                it[name] = obj.username
                it[password] = obj.password
                it[salt] = obj.salt
                it[email] = obj.email
                it[uuid] = obj.uuid.toString()
                it[confirmed] = true
            }
        }
    }
}

