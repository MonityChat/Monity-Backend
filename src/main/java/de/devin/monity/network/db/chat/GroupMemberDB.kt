package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.GroupRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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

    fun getGroupsWhereUserIsIncluded(user: UUID): List<UUID> {
        return transaction { select { userID eq user.toString() }.map { UUID.fromString(it[groupID]) } }
    }

    fun isInGroup(user: UUID, group: UUID): Boolean {
        return transaction { select((groupID eq group.toString()) and (userID eq user.toString())).count() > 0 }
    }
    fun getGroupMemberFor(chatID: UUID, user: UUID): GroupMemberData {
        return get(chatID).first { it.id == user }
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