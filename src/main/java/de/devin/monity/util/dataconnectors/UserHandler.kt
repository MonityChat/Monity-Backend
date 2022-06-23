package de.devin.monity.util.dataconnectors

import de.devin.monity.model.OnlineUser
import de.devin.monity.model.User
import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.network.wsrouting.WebSocketHandler
import de.devin.monity.util.notifications.Notification
import java.util.UUID


/**
 * This util object acts as a connection between DAO and really workable objects
 *
 * For example the database needs data classes to really read/write data
 * But because data classes are only able to stare data and don't do anything with that
 * a real class is required which has the required actions. Therefore, this Handler exists
 * to connect these two easily with some functions
 */
object UserHandler {

    /**
     * Whether a user exists or not
     * @return true if exists false otherwise
     */
    fun exists(uuid: UUID): Boolean {
        return UserDB.has(uuid)
    }

    /**
     * Checks if a user is currently connected
     * @param uuid of the user
     * @return true if connected false otherwise
     */
    fun isOnline(uuid: UUID): Boolean {
        return WebSocketHandler.isConnected(uuid)
    }


    /**
     * Gets the online user if online
     * @param uuid of the user
     * @return OnlineUSer if online
     */
    fun getOnlineUser(uuid: UUID): OnlineUser {
        if (!exists(uuid)) error("UUID not found")
        if (!isOnline(uuid)) error("UUID is not online")


        val socket = WebSocketHandler.getConnection(uuid)
        return OnlineUser(uuid, socket)
    }

    /**
     * Sends the given notification to the user if the user is online
     * If the user is not online nothing will happeen
     * @param target uuid of the user to send the notification to
     * @param notification the notifcation to send
     */
    fun sendNotificationIfOnline(target: UUID, notification: Notification) {
        if (!isOnline(target)) return
        getOnlineUser(target).sendNotification(notification)
    }

    /**
     * Gets the offline user
     * @param uuid of the offline user
     * @return User
     */
    fun getOfflineUser(uuid: UUID): User {
        return User(uuid)
    }


}