package de.devin.monity.network.db.user

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.FriendStatus
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
        SchemaUtils.createMissingTablesAndColumns(UserContactDB)
    }

    override fun has(id: UUID): Boolean {
        return transaction { select(root eq id.toString()).count() > 0 }
    }

    override fun get(id: UUID): List<FriendData> {
        return transaction { select(root eq id.toString()).map { FriendData(UUID.fromString(it[root]), UUID.fromString(it[to]), FriendStatus.valueOf(it[status])) } }
    }

    fun getContactsFrom(id: UUID): List<UUID> {
        return transaction { select((root eq id.toString()) or (to eq id.toString()) and (status eq FriendStatus.ACCEPTED.toString())).map {
            if (it[root] == id.toString()) UUID.fromString(it[to]) else UUID.fromString(it[root])
        } }
    }

    fun removeRequest(from: UUID, to: UUID) {
        transaction {
            deleteWhere {
                (root eq from.toString()) and (UserContactDB.to eq to.toString())
            }
        }
    }

    fun getOpenRequestsFrom(id: UUID): List<UUID> {
        return get(id).filter { it.status == FriendStatus.PENDING }.map { it.from }
    }

    fun areFriends(from: UUID, to: UUID): Boolean {
        return get(from).any { it.to == to && it.status == FriendStatus.ACCEPTED} ||
                get(to).any { it.to == from && it.status == FriendStatus.ACCEPTED}
    }

    fun hasBlocked(from: UUID, to: UUID): Boolean {
        return get(from).any { it.from == from && it.to == to && it.status == FriendStatus.BLOCKED}
    }

    fun sendRequest(from: UUID, to: UUID): Boolean {
        return get(from).any { it.from == from && it.to == to && it.status == FriendStatus.PENDING}
    }

    fun hasRequest(user: UUID, from: UUID): Boolean {
        return get(from).any { it.from == from && it.to == user && it.status == FriendStatus.PENDING}
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