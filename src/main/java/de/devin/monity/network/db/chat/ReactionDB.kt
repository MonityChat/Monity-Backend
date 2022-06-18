package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


data class ReactionData(val messageID: UUID, val reaction: String, val id: UUID, val reactors: List<UUID>)
object ReactionDB: Table("message_reactions"), DBManager<ReactionData, UUID> {

    private val reaction = varchar("reaction_reaction", 3)
    private val id = varchar("reaction_id", 36)
    private val reactor = varchar("reaction_reactor_id", 36)
    private val messageID = varchar("reaction_message_id", 36)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { ReactionDB.id eq id.toString() }.count() > 0 }
    }

    override fun get(id: UUID): ReactionData {
        val reactions = transaction {
            select(ReactionDB.id eq  id.toString()).map { ReactionData(UUID.fromString(it[messageID]), it[reaction], id, listOf(UUID.fromString(it[reactor]))) }
        }

        val reactors = mutableListOf<UUID>()

        for (reaction in reactions) {
            reactors.add(reaction.id)
        }
        return ReactionData(reactions[0].messageID, reactions[0].reaction, id, reactors)
    }

    fun getReactionsForMessage(messageID: UUID): List<ReactionData> {
        return transaction { select (ReactionDB.messageID eq messageID.toString()) }.map { get(UUID.fromString(it[id])) }
    }

    fun hasUserReacted(messageID: UUID, user: UUID, reaction: String): Boolean {
        return transaction { select ((ReactionDB.messageID eq messageID.toString()) and (reactor eq user.toString()) and (ReactionDB.reaction eq reaction)).count() > 0 }
    }

    fun addReactionToMessage(messageID: UUID, user: UUID, reaction: String): ReactionData {
        val id = newUUID()
        runBlocking {
            transaction {
                insert {
                    it[ReactionDB.messageID] = messageID.toString()
                    it[reactor] = user.toString()
                    it[ReactionDB.reaction] = reaction
                    it[ReactionDB.id] = id.toString()
                }
            }
        }

        return get(id)
    }

    fun newUUID(): UUID {
        var uuid = UUID.randomUUID()
        while (has(uuid)) uuid = UUID.randomUUID()
        return uuid
    }

    override fun insert(obj: ReactionData) {
        transaction {
            for (reactor in obj.reactors) {
                insert {
                    it[reaction] = obj.reaction
                    it[id] = obj.id.toString()
                    it[ReactionDB.reactor] = reactor.toString()
                    it[messageID] = obj.messageID.toString()
                }
            }
        }

    }
}