package de.devin.monity.model

import de.devin.monity.network.db.chat.ChatDB
import de.devin.monity.network.db.chat.GroupDB
import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.network.db.user.UserContactDB
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.dataconnectors.UserHandler
import java.util.UUID


/**
 * This serves as a directly usable model of a user.
 */
open class User(val uuid: UUID) {

    val profile = DetailedUserDB.get(uuid)
    val data = UserDB.get(uuid)
    val contacts: List<User> = UserContactDB.getContactsFrom(uuid).map { UserHandler.getOfflineUser(uuid) }
    val privateChats = ChatDB.getChatsFor(uuid)
    val groupChats = GroupDB.getGroupsWhereUserIsIncluded(uuid)
    fun getUserName(): String {
        return data.username
    }
    fun getEmail(): String {
        return data.email
    }
}