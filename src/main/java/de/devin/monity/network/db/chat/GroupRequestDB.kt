package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class GroupRequest(val user: UUID, val group: UUID, val request: String)
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

    fun removeRequest(user: UUID, group: UUID) {
        transaction { deleteWhere { (groupID eq group.toString()) and (userID eq user.toString()) } }
    }

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