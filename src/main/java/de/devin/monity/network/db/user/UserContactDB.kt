package de.devin.monity.network.db.user

import de.devin.monity.network.db.util.DBManager
import de.devin.monity.util.FriendStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


/**
 * Frienddata models the relationship between 2 users.
 * @see FriendStatus
 *
 * @param from the user who sent the request
 * @param to the user who received the request
 * @param status of the relationship
 */
data class FriendData(val from: UUID, val to: UUID, val status: FriendStatus)


/**
 * Contains all data around
 * @see FriendData
 */
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

    /**
     * Returns a list of contacts from the user
     * @param id of the user
     * @return list of contacts
     */
    fun getContactsFrom(id: UUID): List<UUID> {
        return transaction { select((root eq id.toString()) or (to eq id.toString()) and (status eq FriendStatus.ACCEPTED.toString())).map {
            if (it[root] == id.toString()) UUID.fromString(it[to]) else UUID.fromString(it[root])
        } }
    }

    /**
     * Removes an existing request
     * @param from who sent the request
     * @param to where the request is going to
     */
    fun removeRequest(from: UUID, to: UUID) {
        transaction {
            deleteWhere {
                (root eq from.toString()) and (UserContactDB.to eq to.toString())
            }
        }
    }

    /**
     * Returns not accepted requests from a user
     * @param id of the user
     * @return list with all user who requested friendship
     */
    fun getOpenRequestsFrom(id: UUID): List<UUID> {
        return get(id).filter { it.status == FriendStatus.PENDING }.map { it.from }
    }

    /**
     * Whether the 2 users are friends or not
     * @param from the uuid from
     * @param to the uuid to
     * @return Whether the 2 users are friends or not
     */
    fun areFriends(from: UUID, to: UUID): Boolean {
        return get(from).any { it.to == to && it.status == FriendStatus.ACCEPTED} ||
                get(to).any { it.to == from && it.status == FriendStatus.ACCEPTED}
    }

    /**
     * Whether the user has blocked the other user or not
     * @param from the uuid from
     * @param to the uuid to
     * @return Whether the user has blocked the other user or not
     */
    fun hasBlocked(from: UUID, to: UUID): Boolean {
        return get(from).any { it.from == from && it.to == to && it.status == FriendStatus.BLOCKED}
    }

    /**
     * Whether the user has already sent a request or not
     * @param from user who sent the request
     * @param to user who receives the request
     * @return Whether the user has already sent a request or not
     */
    fun sendRequest(from: UUID, to: UUID): Boolean {
        return get(from).any { it.from == from && it.to == to && it.status == FriendStatus.PENDING}
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

    /**
     * Updates the status of a relationship
     * @param from user1
     * @param from user2
     * @param status the new status
     */
    fun updateStatus(from: UUID, to: UUID, status: FriendStatus) {
        transaction {
            update({(root eq from.toString()) and (UserContactDB.to eq to.toString())}) {
                it[UserContactDB.status] = status.toString()
            }
        }
    }
}