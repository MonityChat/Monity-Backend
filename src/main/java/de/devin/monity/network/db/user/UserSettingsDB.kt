package de.devin.monity.network.db.user

import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


/**
 * Models the different settings of who can send the user a friend request
 */
enum class FriendRequestLevel { ALL, FRIENDS_OF_FRIENDS, NONE }

/**
 * Models the different settings of who can view the profile of the user
 */
enum class DataOptions { ALL, ONLY_CONTACTS, NONE }


/**
 * User settings store what kind of settings a user has made
 * @see FriendRequestLevel
 * @see DataOptions
 */
data class UserSettings(val id: UUID, val friendRequestLevel: FriendRequestLevel, val dataOptions: DataOptions)

/**
 * Contains all data around
 * @see UserSettingsDB
 */
object UserSettingsDB: Table("user_settings"), DBManager<UserSettings, UUID> {

    private val userID = varchar("user_settings_uuid", 36)
    private val friendRequestLevel = varchar("user_settings_request_level", 50)
    private val dataOptions = varchar("user_settings_data_options", 50)

    override val primaryKey = PrimaryKey(userID)
    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select(userID eq id.toString()).count() > 0 }
    }

    override fun get(id: UUID): UserSettings {
        return transaction { select(userID eq id.toString()).map { UserSettings(id, FriendRequestLevel.valueOf(it[friendRequestLevel]),
            DataOptions.valueOf(it[dataOptions])) }[0] }
    }

    override fun insert(obj: UserSettings) {
        transaction {
            insert {
                it[userID] = obj.id.toString()
                it[friendRequestLevel] = obj.friendRequestLevel.toString()
                it[dataOptions] = obj.dataOptions.toString()
            }
        }
    }

    /**
     * Sets the users new dataOptions
     * @param id the user
     * @param dataOptions the new dataOptions
     */
    fun setUserDataOptions(id: UUID, dataOptions: DataOptions) {
        transaction {
            update ({userID eq id.toString()}) {
                it[UserSettingsDB.dataOptions] = dataOptions.toString()
            }
        }
    }

    /**
     * Sets the users new FriendRequestLevel
     * @param id of the user
     * @param requestLevel the new friend request level
     */
    fun setUserRequestLevel(id: UUID, requestLevel: FriendRequestLevel) {
        transaction {
            update({ userID eq id.toString() }) {
                it[friendRequestLevel] = requestLevel.toString()
            }
        }
    }
}