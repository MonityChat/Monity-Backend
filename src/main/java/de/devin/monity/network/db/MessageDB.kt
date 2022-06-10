package de.devin.monity.network.db

import de.devin.monity.model.Message
import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class MessageData(val sender: UUID, val chat: ChatData, val content: String, val sent: Long)

object MessageDB: Table("messages"), DBManager<MessageData, UUID> {

    private val sender = varchar("message_sender_uuid", 36)
    private val chatID = varchar("message_chat_id", 36)
    private val content = text("message_content")
    private val sent = long("message_timestamp_sent")

    override fun load() {
        SchemaUtils.create(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select(chatID eq id.toString()).count() > 0 }
    }

    override fun get(id: UUID): MessageData {
        return transaction { select(chatID eq id.toString()).map { MessageData(UUID.fromString(it[sender]), ChatDB.get(UUID.fromString(it[chatID])),it[content], it[sent]) } }[0]
    }

    override fun insert(obj: MessageData) {
        transaction {
            insert {
                it[sender] = obj.sender.toString()
                it[chatID] = obj.chat.id.toString()
                it[content] = obj.content
                it[sent] = obj.sent
            }
        }
    }
}