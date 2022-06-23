package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


/**
 * A group invite is created when a member of a group invites a non-member
 * @param user who is invited
 * @param group group he is invited to
 */
data class GroupInvite(val user: UUID, val group: UUID)


/**
 * Contains all data around
 * @see GroupInvite
 */
object GroupMemberInvitesDB: Table("group_invites"), DBManager<List<GroupInvite>, UUID> {

    private val user = varchar("group_invites_user_id", 36)
    private val group = varchar("group_invites_group_id", 36)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select(group eq  id.toString()).count() > 0 }
    }

    override fun get(id: UUID): List<GroupInvite> {
        return transaction { select(group eq id.toString()).map { GroupInvite(id, UUID.fromString(it[group])) } }
    }


    /**
     * Checks of the user is invited into the group
     * @param groupID id of the group
     * @param userID id of the user
     * @return whether the user is invited or not
     */
    fun userInvitedToGroup(groupID: UUID, userID: UUID): Boolean {
        return transaction { select((group eq groupID.toString() and (user eq userID.toString()))).count() > 0 }
    }

    /**
     * Deletes a pending invitation
     * @param groupID the group
     * @param userID user who was invited
     */
    fun deleteInvitation(groupID: UUID, userID: UUID) {
        transaction { deleteWhere { (group eq groupID.toString()) and (user eq userID.toString()) } }
    }

    override fun insert(obj: List<GroupInvite>) {
        transaction {
            insert {
                for (group in obj) {
                    it[user] = group.user.toString()
                    it[GroupMemberInvitesDB.group] = group.group.toString()
                }
            }
        }
    }
}