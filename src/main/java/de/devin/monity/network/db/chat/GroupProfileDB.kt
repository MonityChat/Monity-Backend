package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * Group profile describes a profile of group similar to a UserProfile
 * @param id of the group
 * @param title of the group
 * @param description of the group
 * @param shortStatus of the group
 * @param image location of the group image
 * @param r color r
 * @param g color g
 * @param b color b
 */
data class GroupProfile(val id: UUID, val title: String, val description: String, val shortStatus: String, val image: String, val r: Byte, val b: Byte, val g: Byte)


/**
 * Contains all data around
 * @see GroupProfile
 */
object GroupProfileDB: Table("group_profiles"), DBManager<GroupProfile, UUID> {

    private val groupID = varchar("group_profile_id", 36)
    private val title = varchar("group_profile_title", 128)
    private val description = varchar("group_profile_short_status", 128)
    private val image = varchar("group_profile_image_file_location",512)
    private val shortStatus = varchar("group_profile_shortstatus", 8000)
    private val r = byte("group_profile_r")
    private val g = byte("group_profile_g")
    private val b = byte("group_profile_b")

    override val primaryKey = PrimaryKey(groupID)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { groupID eq id.toString() }.count() > 0 }
    }

    override fun get(id: UUID): GroupProfile {
        return transaction { select { groupID eq id.toString() }.map { GroupProfile(id, it[title], it[description], it[shortStatus], it[image], it[r], it[b], it[g]) } }[0]
    }

    override fun insert(obj: GroupProfile) {
        transaction {
            insert {
                it[groupID] = obj.id.toString()
                it[title] = obj.title
                it[description] = obj.description
                it[image] = obj.image
                it[shortStatus] = obj.shortStatus
                it[r] = obj.r
                it[g] = obj.g
                it[b] = obj.b
            }
        }
    }
}