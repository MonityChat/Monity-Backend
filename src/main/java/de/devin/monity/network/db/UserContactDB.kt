package de.devin.monity.network.db

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.FriendStatus
import filemanagment.util.logInfo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

data class FriendData(val from: UUID, val to: UUID, val status: FriendStatus)

object UserContactDB: Table("user_contact"), DBManager<List<FriendData>, UUID> {

    private val root = varchar("user_from", 36)
    private val to = varchar("user_to", 36)
    private val status = varchar("friend_status", 50)

    override fun load() {
        SchemaUtils.create(UserContactDB)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select(root eq id.toString()).count() > 0 }
    }

    override fun get(id: UUID): List<FriendData> {
        return transaction { select(root eq id.toString()).map { FriendData(UUID.fromString(it[root]), UUID.fromString(it[to]), FriendStatus.valueOf(it[status])) } }
    }

    override fun insert(obj: List<FriendData>) {
        for (friendData in obj) {
            transaction {
                insert {
                    it[root] = friendData.from.toString()
                    it[to] = friendData.to.toString()
                    it[status] = friendData.status.toString()
                }
            }
        }
    }

    fun updateStatus(from: UUID, to: UUID, status: FriendStatus) {
        transaction {
            update({(root eq from.toString()) and (UserContactDB.to eq to.toString())}) {
                it[UserContactDB.status] = status.toString()
            }
        }
    }
}