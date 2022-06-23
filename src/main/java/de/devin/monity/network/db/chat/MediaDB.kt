package de.devin.monity.network.db.chat

import de.devin.monity.network.db.user.UserProfile
import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


/**
 * A MediaData is always in connection with a MessageData, it will link the messageID to and filePath so that media can be sent via messages
 * The corresponding table MediaDB will allow for 1 ID to have multiple MediaData attached so it. This allows a message to carry multiple medias such as
 * images, videos, documents and more.
 *
 * @param id embedID
 * @param filePath leads to where the media is saved on the drive
 */
data class MediaData(val id: UUID, val filePath: String)


/**
 * Contains all data around
 * @see MediaData
 */
object MediaDB: Table("media"), DBManager<List<MediaData>, UUID> {

    private val embedID = varchar("media_message_embedID", 36)
    private val filePath = varchar("media_file_path",512)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { embedID eq id.toString() }.count() > 0 }
    }

    override fun get(id: UUID): List<MediaData> {
        return transaction { select(embedID eq id.toString()).map { MediaData(id, it[filePath]) } }
    }

    /**
     * Deletes the embed if it exists
     * @param embedID the embed
     */
    fun deleteIfExists(embedID: UUID) {
        transaction { deleteWhere { MediaDB.embedID eq embedID.toString() } }
    }

    /**
     * inserts a single embed
     * @param item the embed to insert
     */
    fun insertSingle(item: MediaData) {
        transaction {
            insert {
                it[embedID] = item.id.toString()
                it[filePath] = item.filePath
            }
        }
    }
    override fun insert(obj: List<MediaData>) {
        transaction {
            insert {
                for (item in obj) {
                    it[filePath] = item.filePath
                }
            }
        }
    }
}