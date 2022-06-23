package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.GroupRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Contains all data around
 * @see GroupMemberData
 */
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

    /**
     * Returns a list of groups where a user is included
     * @param user the user
     * @return list of all groupsid where the user is included
     */
    fun getGroupsWhereUserIsIncluded(user: UUID): List<UUID> {
        return transaction { select { userID eq user.toString() }.map { UUID.fromString(it[groupID]) } }
    }

    /**
     * Checks if the user is in the given group
     * @param user the user
     * @param group the group
     * @return whether the user is in the group or not
     */
    fun isInGroup(user: UUID, group: UUID): Boolean {
        return transaction { select((groupID eq group.toString()) and (userID eq user.toString())).count() > 0 }
    }

    /**
     * Returns the group for a given member
     * @param group the group
     * @param user the user
     * @return groupmemberdata for the given group
     */
    fun getGroupMemberFor(group: UUID, user: UUID): GroupMemberData {
        return get(group).first { it.id == user }
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