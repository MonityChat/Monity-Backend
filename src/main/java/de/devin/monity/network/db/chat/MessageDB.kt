package de.devin.monity.network.db

import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class MessageData(val sender: UUID,
                       val messageID: UUID,
                       val chat: ChatData,
                       val content: String,
                       val relatedTo: MessageData?,
                       val attachedMedia: List<MediaData>,
                       val index: Long,
                       val sent: Long)
object MessageDB: Table("messages"), DBManager<MessageData, UUID> {

    private val muid = varchar("message_id", 36)
    private val sender = varchar("message_sender_uuid", 36)
    private val chatID = varchar("message_chat_id", 36)
    private val content = text("message_content")
    private val sent = long("message_timestamp_sent")
    private val index = long("message_index")
    private val relatedTo = varchar("message_relation_id", 36).nullable()

    override val primaryKey = PrimaryKey(muid)
    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }
    override fun has(id: UUID): Boolean {
        return transaction { select { muid eq id.toString() }.count() > 0 }
    }

    override fun get(id: UUID): MessageData {
        return transaction { select(muid eq id.toString())
            .map { MessageData(UUID.fromString(it[sender]),
                UUID.fromString(it[muid]),
                ChatDB.get(UUID.fromString(it[chatID])),
                it[content],
                get(UUID.fromString(it[relatedTo])),
                MediaDB.get(id),
                it[index],
                it[sent]
            )} }[0]
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

    override fun insert(obj: MessageData) {
        transaction {
            insert {
                it[sender] = obj.sender.toString()
                it[chatID] = obj.chat.id.toString()
                it[content] = obj.content
                it[sent] = obj.sent
                it[muid] = obj.messageID.toString()
                it[index] = obj.index
                it[relatedTo] = obj.relatedTo?.messageID.toString()
            }
        }
    }
}