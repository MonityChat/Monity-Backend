package de.devin.monity.network.db.chat

import de.devin.monity.network.db.util.DBManager
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


data class GroupSettings(val groupID: UUID, val opened: Boolean, val inviteOnly: Boolean, val requiresRequest: Boolean)
object GroupSettingDB: Table("groupsettings"), DBManager<GroupSettings, UUID> {

    private val opened = bool("group_setting_opened")
    private val inviteOnly = bool("group_setting_invite_only")
    private val requiresRequest = bool("group_setting_requires_request")
    private val groupID = varchar("group_setting_group_id", 36)

    override val primaryKey = PrimaryKey(groupID)

    override fun load() {
        SchemaUtils.createMissingTablesAndColumns(this)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select { groupID eq id.toString() }.count() > 0 }
    }

    override fun get(id: UUID): GroupSettings {
        return transaction { select(groupID eq id.toString()).map { GroupSettings(id, it[opened], it[inviteOnly], it[requiresRequest]) } }[0]
    }

    override fun insert(obj: GroupSettings) {
        transaction {
            insert {
                it[opened] = obj.opened
                it[inviteOnly] = obj.inviteOnly
                it[requiresRequest] = obj.requiresRequest
                it[groupID] = obj.groupID.toString()
            }
        }
    }
}