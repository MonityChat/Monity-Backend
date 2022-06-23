package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * A group request models a request from a user to a group
 * @param user of the requester
 * @param group he is requesting to
 * @param request his request
 */
data class GroupRequest(val user: UUID, val group: UUID, val request: String)


/**
 * Contains all data around
 * @see GroupRequest
 */
object GroupRequestDB: Table("group_requests"), DBManager<List<GroupRequest>, UUID>  {

    private var groupID = varchar("group_request_group_id", 36)
    private var userID = varchar("group_request_user_id", 36)
    private var request = varchar("group_request_request", 512)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { groupID eq id.toString() }.count() > 0 }
    }

    override fun get(id: UUID): List<GroupRequest> {
        return transaction { select { groupID eq id.toString() }.map { GroupRequest(UUID.fromString(it[userID]), id, it[request]) } }
    }


    /**
     * Removed a request from a user
     * @param user uuid of the user
     * @param group group id
     */
    fun removeRequest(user: UUID, group: UUID) {
        transaction { deleteWhere { (groupID eq group.toString()) and (userID eq user.toString()) } }
    }

    /**
     * Checks if a user has requested
     * @param user uuid of the user
     * @param group he is requesting to
     * @return whether he has requested or not
     */
    fun hasRequested(user: UUID, group: UUID): Boolean {
        return transaction { select { (groupID eq group.toString()) and (userID eq user.toString()) }.count() > 0 }
    }

    override fun insert(obj: List<GroupRequest>) {
        transaction {
            for (item in obj) {
                insert {
                    it[groupID] = item.group.toString()
                    it[userID] = item.user.toString()
                    it[request] = item.request
                }
            }
        }
    }
}