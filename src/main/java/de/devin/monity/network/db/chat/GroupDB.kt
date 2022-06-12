package de.devin.monity.network.db.chat

import de.devin.monity.network.db.GroupProfile
import de.devin.monity.network.db.GroupProfileDB
import de.devin.monity.network.db.MessageDB
import de.devin.monity.network.db.MessageData
import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.GroupRole
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


/**
 * A group chat is similar to a normal chat. Instead of consisting of 2 users, it has an unlimited amount of users,
 * which can either join the chat if open or be invited to it.
 *
 * GroupChats can either be open or closed. An open group chat can be found by anyone in the monity network and allows everybody to request to join.
 * A closed group chat is only visible to members who are in the groupchat. New members can only join if invited.
 *
 * @param initiator the creator of the groupchat
 * @param members a list of every member in the group chat
 * @param id the unique id of the groupchat
 * @param started the timestamp the groupchat was created
 * @param groupProfile a profile where all displayed information are stored
 */
data class GroupChatData(val initiator: UUID,
                         val members: List<GroupMemberData>,
                         val messages: List<MessageData>,
                         val id: UUID,
                         val started: Long,
                         val groupSettings: GroupSettings,
                         val groupProfile: GroupProfile,)


/**
 * A group member is a user which is in the group with the given id
 * Every user has a role which allows them to do different actions inside of the group
 * @see GroupRole
 *
 * @param id the user id
 * @param groupID the group ID
 * @param role role of the user
 */
data class GroupMemberData(val id: UUID, val groupID: UUID, val role: GroupRole)
object GroupDB: Table("groups"), DBManager<GroupChatData, UUID> {

    private val initiatorID = varchar("group_initiator_uuid", 36)
    private val groupID = varchar("group_id", 36)
    private val started = long("group_started_timestamp")

    override val primaryKey = PrimaryKey(groupID)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { groupID eq id.toString() }.count() > 0 }
    }

    override fun get(id: UUID): GroupChatData {
        return transaction {
            select { groupID eq id.toString() }.map {
                GroupChatData(
                    UUID.fromString(it[initiatorID]),
                    GroupMemberDB.get(id),
                    MessageDB.getMessagesForChat(id),
                    id,
                    it[started],
                    GroupSettingDB.get(id),
                    GroupProfileDB.get(id)
                )
            }[0]
        }
    }

    override fun insert(obj: GroupChatData) {
        transaction {
            insert {
                it[groupID] = obj.id.toString()
                it[initiatorID] = obj.initiator.toString()
                it[started] = obj.started
            }
        }
    }
}