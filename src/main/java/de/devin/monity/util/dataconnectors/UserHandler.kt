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

    fun exists(uuid: UUID): Boolean {
        return UserDB.has(uuid)
    }

    fun isOnline(uuid: UUID): Boolean {
        return WebSocketHandler.isConnected(uuid)
    }


    fun getOnlineUser(uuid: UUID): OnlineUser {
        if (!exists(uuid)) error("UUID not found")
        if (!isOnline(uuid)) error("UUID is not online")

        val data = UserDB.get(uuid)
        val profile = DetailedUserDB.get(uuid)

        val socket = WebSocketHandler.getConnection(uuid)
        return OnlineUser(data, profile, socket)
    }

    fun sendNotificationIfOnline(target: UUID, notification: Notification) {
        if (!isOnline(target)) return
        getOnlineUser(target).sendNotification(notification)
    }

    fun getOfflineUser(): User {
        TODO("IMPLEMENT")
    }


}