package de.devin.monity.network.db.user

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.Status
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


/**
 * Creates a new default user
 * @param username the username
 * @param uuid uuid of the user
 * @return UserProfile
 */
fun createDefaultUser(username: String, uuid: UUID): UserProfile {
    return UserProfile(username, "/images/monity/default.png", uuid, "Hi im new here", Status.ONLINE, "I LOVE Monity", System.currentTimeMillis(), Status.ONLINE)
}

/**
 * Creates a default userprofile for an anonymoususer.
 * @see UserSettings
 * @param username the username
 * @param uuid the uuid of the user
 * @return Anonymous user
 */
fun createAnonymousUser(username: String, uuid: UUID): UserProfile {
    return UserProfile(username, "/images/monity/default.png", uuid, "", Status.OFFLINE, "", System.currentTimeMillis(), Status.OFFLINE)
}

/**
 * A user profile contains all detailed information about a user.
 * This information is also called the UserProfile
 * @param userName the username
 * @param profileImageLocation the location of the profile image
 * @param uuid the uuid of the user
 * @param description the description of a user
 * @param status the online status of the user
 * @param shortStatus the short status of the user (max length 128)
 * @param lastSeen timestamp when the user logged off the last time
 * @param preferredStatus the preferredStatus of the user. When the user is online it will display this status, when the user is offline it will state offline
 */
data class UserProfile(val userName: String,
                       val profileImageLocation: String,
                       val uuid: UUID,
                       val description: String,
                       val status: Status,
                       val shortStatus: String,
                       val lastSeen: Long,
                       val preferredStatus: Status)

object DetailedUserDB: Table("user_profile"), DBManager<UserProfile, UUID> {

    private val username = varchar("user_data_name", 48)
    private val profileImageLocation = varchar("user_data_image_location", 150)
    private val uuid = varchar("user_data_uuid", 36)
    private val description = varchar("user_data_description", 8000)
    private val status = varchar("user_data_status", 50)
    private val lastSeen = long("user_data_last_seen")
    private val shortStatus = varchar("user_data_short_status", 128)
    private val preferredStatus = varchar("user_data_preferred_status", 50)

    override val primaryKey = PrimaryKey(uuid)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(DetailedUserDB)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { uuid eq id.toString() }.count() > 0}
    }

    override fun get(id: UUID): UserProfile {
        return transaction { select (uuid eq id.toString()).map { UserProfile(it[username],
            it[profileImageLocation],
            UUID.fromString(it[(uuid)]),
            it[description],
            Status.valueOf(it[status]),
            it[shortStatus], it[lastSeen],
            Status.valueOf(it[preferredStatus])) } }[0]
    }

    /**
     * Sets the online status of the user
     * @param user the user
     * @param status the status
     */
    fun setStatus(user: UUID, status: Status) {
        transaction {
            update({ uuid eq user.toString()}) { it[DetailedUserDB.status] = status.toString() }
        }
    }

    /**
     * Updates when the user was last seen
     * @param user the user
     */
    fun updateLastSeen(user: UUID) {
        transaction {
            update({ uuid eq user.toString()}) { it[lastSeen] = System.currentTimeMillis() }
        }
    }

    /**
     * Updates the profile picture of the user
     * @param user the user
     * @param url new path to image
     */
    fun updateProfilePicture(user: UUID, url: String) {
        transaction {
            update({ uuid eq user.toString() }) {
                it[profileImageLocation] = url
            }
        }
    }

    /**
     * Updates the users profile
     * @param user the user
     * @param profile the new profile
     */
    fun updateProfile(user: UUID, profile: UserProfile) {
        transaction {
            update({ uuid eq user.toString()}) {
                it[description] = profile.description
                it[shortStatus] = profile.shortStatus
                it[profileImageLocation] = profile.profileImageLocation
                it[preferredStatus] = profile.preferredStatus.toString()
                it[status] = profile.preferredStatus.toString()
            }
        }
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
                it[preferredStatus] = obj.preferredStatus.toString()
            }
        }
    }
}