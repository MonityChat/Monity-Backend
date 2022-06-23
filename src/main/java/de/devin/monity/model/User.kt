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
 * @param uuid of the user
 */
open class User(val uuid: UUID) {

    /**
     * Profile of the user
     */

    val profile = DetailedUserDB.get(uuid)


    /**
     * Basic user data
     */
    val data = UserDB.get(uuid)

    /**
     * Contacts from this user
     */
    val contacts = UserContactDB.getContactsFrom(uuid)

    /**
     * Private chats from this user
     */
    val privateChats = ChatDB.getChatsFor(uuid)

    /**
     * Group chats from this user
     */
    val groupChats = GroupDB.getGroupsWhereUserIsIncluded(uuid)


    /**
     * Returns the username
     * @return username as string
     */
    fun getUserName(): String {
        return data.username
    }

    /**
     * Returns the username
     * @return email as string
     */
    fun getEmail(): String {
        return data.email
    }
}