package de.devin.monity.model

import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.network.wsrouting.WebSocketHandler
import de.devin.monity.util.Status
import de.devin.monity.util.notifications.Notification
import de.devin.monity.util.notifications.send
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import java.util.*


/**
 * An onlineuser is a normal user which is currently connected via websocket to the monity backend
 * @param uuid of the user
 * @param socketSession connection of the user
 */
class OnlineUser(uuid: UUID, private val socketSession: DefaultWebSocketSession) : User(uuid) {


    /**
     * Sends a notification over the websocket to the user
     * @param notification the notification to send
     */
    fun sendNotification(notification: Notification) {
        runBlocking {
            if (WebSocketHandler.isValidConnection(socketSession))
                socketSession.send(notification)
        }
    }

    /**
     * Sets the status of the user
     * @param status new status
     */

    @Deprecated("Use the database directly", ReplaceWith(
        "DetailedUserDB.setStatus(uuid, status)",
        "de.devin.monity.network.db.user.DetailedUserDB"))
    fun setStatus(status: Status) {
        DetailedUserDB.setStatus(uuid, status)
    }


    /**
     * Sets the last seen of the user to the current time
     */

    @Deprecated("Use the database directly", ReplaceWith(
        "DetailedUserDB.updateLastSeen(uuid)",
        "de.devin.monity.network.db.user.DetailedUserDB"))
    fun updateLastSeen() {
        DetailedUserDB.updateLastSeen(uuid)
    }
}