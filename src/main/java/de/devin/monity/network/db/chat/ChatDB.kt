package de.devin.monity.network.db

import de.devin.monity.network.db.chat.GroupDB
import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.GroupRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*



/**
 * A chat consists out of 2 Users who can chat inside the chat.
 * The chat will be automatically created when the first User sends a direct message to another user
 *
 * @param initiator the user who started the chat
 * @param otherUser the other user
 * @param id the unique id of the chat
 * @param started the timestamp when the chat was created
 * @param messages a list of all messages sent in the chat
 */
data class ChatData(val initiator: UUID, val otherUser: UUID, val id: UUID, val started: Long, val messages: List<MessageData>)

object ChatDB: Table("chats"), DBManager<ChatData, UUID> {
    private val initiator = varchar("chat_initiator_uuid", 36)
    private val otherUser = varchar("chat_other_uuid", 36)
    private val chatID = varchar("chat_id", 36)
    private val started = long("chat_started_timestamp")
    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select(chatID eq id.toString()).count() > 0 }
    }

    fun getChatsFor(user: UUID): List<ChatData> {
        return transaction { select((initiator eq user.toString()) or (otherUser eq user.toString())) }.map { get(UUID.fromString(it[chatID])) }
    }
    override fun get(id: UUID): ChatData {
        return transaction {
            select(chatID eq id.toString()).map {
                ChatData(UUID.fromString(it[initiator]),
                    UUID.fromString(it[otherUser]),
                    id,
                    it[started],
                    MessageDB.getMessagesForChat(id))}[0]
        }
    }

    fun newID(): UUID {
        var uuid = UUID.randomUUID()
        while (has(uuid) || GroupDB.has(uuid)) uuid = UUID.randomUUID()
        return uuid
    }

    override fun insert(obj: ChatData) {
        transaction {
            insert {
                it[initiator] = obj.initiator.toString()
                it[otherUser] = obj.otherUser.toString()
                it[chatID] = obj.id.toString()
                it[started] = obj.started
            }
        }
    }
}