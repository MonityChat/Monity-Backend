package de.devin.monity.network.db

import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


/**
 * A MediaData is always in connection with a MessageData, it will link the messageID to and filePath so that media can be sent via messages
 * The corresponding table MediaDB will allow for 1 ID to have multiple MediaData attached so it. This allows a message to carry multiple medias such as
 * images, videos, documents and more.
 *
 * @param id will be the corresponding message id
 * @param filePath leads to where the media is saved on the drive
 */
data class MediaData(val id: UUID, val filePath: String)
object MediaDB: Table("media"), DBManager<List<MediaData>, UUID> {

    val messageID = varchar("media_message_id", 36)
    val filePath = varchar("media_file_path",512)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { messageID eq id.toString() }.count() > 0 }
    }

    override fun get(id: UUID): List<MediaData> {
        return transaction { select(messageID eq  id.toString()).map { MediaData(id, it[filePath]) } }
    }

    override fun insert(obj: List<MediaData>) {
        transaction {
            insert {
                for (item in obj) {
                    it[messageID] = item.id.toString()
                    it[filePath] = item.filePath
                }
            }
        }
    }
}