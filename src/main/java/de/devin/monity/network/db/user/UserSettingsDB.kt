package de.devin.monity.network.db.user

import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


enum class FriendRequestLevel { ALL, FRIENDS_OF_FRIENDS, NONE }
enum class DataOptions { ALL, ONLY_CONTACTS, NONE }
data class UserSettings(val id: UUID, val friendRequestLevel: FriendRequestLevel, val dataOptions: DataOptions)
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
    fun setUserDataOptions(id: UUID, dataOptions: DataOptions) {
        transaction {
            update ({userID eq id.toString()}) {
                it[UserSettingsDB.dataOptions] = dataOptions.toString()
            }
        }
    }
    fun setUserRequestLevel(id: UUID, requestLevel: FriendRequestLevel) {
        transaction {
            update({ userID eq id.toString() }) {
                it[friendRequestLevel] = requestLevel.toString()
            }
        }
    }
}