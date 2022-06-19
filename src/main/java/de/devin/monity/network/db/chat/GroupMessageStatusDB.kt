package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.MessageStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


data class GroupMessageStatus(val messageID: UUID, val chatID: UUID, val user: UUID, val status: MessageStatus)

object GroupMessageStatusDB: Table("group_message_status"), DBManager<GroupMessageStatus, UUID> {

    private val messageID = varchar("group_message_status_message_id", 36)
    private val chatID = varchar("group_message_status_chat_id", 36)
    private val status = varchar("group_message_status_status", 50)
    private val userID = varchar("group_message_status_user", 36)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select(messageID eq id.toString()).count() > 0 }
    }

    override fun get(id: UUID): GroupMessageStatus {
        return transaction { select(messageID eq id.toString()).map { GroupMessageStatus(id, UUID.fromString(it[chatID]), UUID.fromString(it[userID]), MessageStatus.valueOf(it[status])) } }[0]
    }

    fun deleteAllMessages(messageID: UUID) {
        transaction { deleteWhere { GroupMessageStatusDB.messageID eq messageID.toString() } }
    }

    override fun insert(obj: GroupMessageStatus) {
        transaction {
            insert {
                it[messageID] = obj.messageID.toString()
                it[chatID] = obj.chatID.toString()
                it[status] = obj.status.toString()
                it[userID] = obj.user.toString()
            }
        }
    }
}