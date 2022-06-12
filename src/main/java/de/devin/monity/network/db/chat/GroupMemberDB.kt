package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.GroupRole
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object GroupMemberDB: Table("group_members"), DBManager<List<GroupMemberData>, UUID> {

    private val groupID = varchar("groupmember_groupid", 36)
    private val userID = varchar("groupmember_user_id", 36)
    private val role = varchar("groupmember_user_role", 50)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { groupID eq id.toString() }.count() > 0 }
    }

    override fun get(id: UUID): List<GroupMemberData> {
        return transaction { select { groupID eq id.toString() }.map { GroupMemberData(UUID.fromString(it[userID]), id, GroupRole.valueOf(it[role])) } }
    }

    override fun insert(obj: List<GroupMemberData>) {
        transaction {
            insert {
                for (item in obj) {
                    it[groupID] = item.groupID.toString()
                    it[userID] = item.id.toString()
                    it[role] = item.role.toString()
                }

            }
        }
    }
}