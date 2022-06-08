package de.devin.monity.network.db

import de.devin.monity.Monity
import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.Status
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


fun createDefaultUser(username: String, uuid: UUID): UserProfile {
    return UserProfile(username, Monity.dataFolder.absolutePath + "/images/monity/default.png", uuid, "Hi im new here", Status.ONLINE, "I LOVE Monity", System.currentTimeMillis())
}

data class UserProfile(val userName: String,
                       val profileImageLocation: String,
                       val uuid: UUID,
                       val description: String,
                       val status: Status,
                       val shortStatus: String,
                       val lastSeen: Long)

object DetailedUserDB: Table("user_profile"), DBManager<UserProfile, UUID> {

    val username = varchar("user_data_name", 48)
    val profileImageLocation = varchar("user_data_image_location", 150)
    val uuid = varchar("user_data_uuid", 36)
    val description = varchar("user_data_description", 8000)
    val status = varchar("user_data_status", 50)
    val lastSeen = long("user_data_last_seen")
    val shortStatus = varchar("user_data_short_status", 128)

    override val primaryKey = PrimaryKey(uuid)

    override fun load() {
        SchemaUtils.create(DetailedUserDB)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { uuid eq id.toString() }.count() > 0}
    }

    override fun get(id: UUID): UserProfile {
        return transaction { select (uuid eq id.toString()).map { UserProfile(it[username], it[profileImageLocation], UUID.fromString(it[(uuid)]), it[description], Status.valueOf(it[status]), it[shortStatus], it[lastSeen]) } }[0]
    }

    override fun insert(obj: UserProfile) {
        transaction {
            insert {
                it[username] = obj.userName
                it[uuid] = obj.uuid.toString()
                it[description] = obj.description
                it[status] = obj.status.toString()
                it[profileImageLocation] = obj.profileImageLocation
                it[shortStatus] = obj.shortStatus
                it[lastSeen] = obj.lastSeen
            }
        }
    }
}