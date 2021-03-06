package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.MessageStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class MessageData(val sender: UUID,
                       val messageID: UUID,
                       val chat: UUID,
                       val content: String,
                       val relatedTo: MessageData?,
                       val attachedMedia: List<MediaData>,
                       val reactions: List<ReactionData>,
                       val index: Long,
                       val sent: Long,
                       val edited: Boolean,
                       val status: MessageStatus)

object MessageDB: Table("messages"), DBManager<MessageData, UUID> {

    private val muid = varchar("message_id", 36)
    private val sender = varchar("message_sender_uuid", 36)
    private val chatID = varchar("message_chat_id", 36)
    private val content = text("message_content")
    private val sent = long("message_timestamp_sent")
    private val index = long("message_index")
    private val relatedTo = varchar("message_relation_id", 36).nullable()
    private val mediaID = varchar("message_media_id", 36).nullable()
    private val edited = bool("message_edited")
    private val status = varchar("message_status", 50)

    override val primaryKey = PrimaryKey(muid)
    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }
    override fun has(id: UUID): Boolean {
        return transaction { select { muid eq id.toString() }.count() > 0 }
    }

    fun editMessageStatus(id: UUID, status: MessageStatus) {
        transaction {
            update ({muid eq id.toString()}) {
                it[MessageDB.status] = status.toString()
            }
        }
    }

    override fun get(id: UUID): MessageData {
        return transaction { select(muid eq id.toString())
            .map { MessageData(UUID.fromString(it[sender]),
                UUID.fromString(it[muid]),
                UUID.fromString(it[chatID]),
                it[content],
                if (it[relatedTo] != null) get(UUID.fromString(it[relatedTo])) else null,
                if (it[mediaID] != null) MediaDB.get(UUID.fromString(it[mediaID])) else emptyList(),
                ReactionDB.getReactionsForMessage(id),
                it[index],
                it[sent],
                it[edited],
                MessageStatus.valueOf(it[status])
            )
            } }[0]
    }

    fun getFreeUUID(): UUID {
        var uuid = UUID.randomUUID()
        while (has(uuid)) uuid = UUID.randomUUID()
        return uuid
    }

    fun getNextIndex(chatId: UUID): Long {
        if (getMessagesForChat(chatId).isEmpty()) return 0
        val highestIndex = transaction { select { chatID eq chatId.toString()}.orderBy(index to SortOrder.DESC).map { it[index] }[0] }
        return highestIndex + 1
    }
    fun getMessagesForChat(chatID: UUID): List<MessageData> {
        val returnList = mutableListOf<MessageData>()
        transaction {
            for (messageID in select { MessageDB.chatID eq chatID.toString() }.map { UUID.fromString(it[muid]) }) {
                returnList += get(messageID)
            }
        }
        return returnList
    }

    fun removeMessage(messageID: UUID) {
        transaction {
            update({relatedTo eq messageID.toString()}) {
                it[relatedTo] = null
            }
            deleteWhere { muid eq messageID.toString() }
        }

    }

    fun editMessageContent(messageID: UUID, content: String) {
        transaction { update({muid eq messageID.toString()}) {
            it[MessageDB.content] = content
            it[edited] = true
            }
        }
    }
    override fun insert(obj: MessageData) {
        transaction {
            insert {
                it[sender] = obj.sender.toString()
                it[chatID] = obj.chat.toString()
                it[content] = obj.content
                it[sent] = obj.sent
                it[muid] = obj.messageID.toString()
                it[mediaID] = if (obj.attachedMedia.isNotEmpty()) obj.attachedMedia[0].id.toString() else null
                it[index] = obj.index
                it[relatedTo] = if (obj.relatedTo != null) obj.relatedTo.messageID.toString() else null
                it[edited] = obj.edited
                it[status] = obj.status.toString()
            }
        }
    }
}