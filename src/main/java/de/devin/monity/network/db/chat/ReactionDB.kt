package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


data class ReactionData(val messageID: UUID, val reaction: String, val count: Long)

object ReactionDB: Table("message_reactions"), DBManager<ReactionData, String> {
    private val reaction = varchar("reaction_reaction", 10)
    private val count = long("count")
    private val messageID = varchar("reaction_message_id", 36)
    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }


    override fun has(id: String): Boolean {
        return transaction { select { ReactionDB.reaction eq id }.count() > 0 }
    }

    override fun get(id: String): ReactionData {
        return ReactionData(UUID.randomUUID(), "", 0)
    }

    fun getReactionsForMessage(messageID: UUID): List<ReactionData> {
        return transaction { select (ReactionDB.messageID eq messageID.toString()).map { ReactionData(messageID, it[reaction], it[count]) } }
    }

    fun hasMessageReaction(messageID: UUID, reaction: String): Boolean  {
        return transaction { select((ReactionDB.messageID eq messageID.toString()) eq (ReactionDB.reaction eq reaction)).count() > 0  }
    }

    fun getReactionCount(messageID: UUID, reaction: String): Long  {
        return transaction { select((ReactionDB.messageID eq messageID.toString()) eq (ReactionDB.reaction eq reaction)).count()  }
    }

    fun addReactionToMessage(messageID: UUID, reaction: String) {
        if (!hasMessageReaction(messageID, reaction)) {
            transaction {
                insert {
                    it[ReactionDB.messageID] = messageID.toString()
                    it[count] = 1
                    it[ReactionDB.reaction] = reaction
                }
            }
        } else {
            transaction {
                update({(ReactionDB.messageID eq messageID.toString()) and (ReactionDB.reaction eq reaction)}) {
                    it[count] = getReactionCount(messageID, reaction) + 1
                }
            }
        }

    }

    fun deleteReactions(messageID: UUID) {
        transaction { deleteWhere { ReactionDB.messageID eq messageID.toString() } }
    }

    override fun insert(obj: ReactionData) {
        transaction {
            insert {
                it[reaction] = obj.reaction
                it[count] = obj.count
                it[messageID] = obj.messageID.toString()
            }
        }
    }
}