package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


data class GroupInvite(val user: UUID, val group: UUID)
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

    fun userInvitedToGroup(groupID: UUID, userID: UUID): Boolean {
        return transaction { select((group eq groupID.toString() and (user eq userID.toString()))).count() > 0 }
    }

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