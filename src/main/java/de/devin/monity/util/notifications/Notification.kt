package de.devin.monity.util.notifications

import io.ktor.websocket.*
import org.json.JSONObject
import java.util.*


/**
 * A notification is sent when any action occurs on the server and a user has to be notified about it
 *
 * For example when a user sends a message to another user, the other user has to be notified about it
 *
 */
interface Notification {

    /**
     * The user who triggered the notification
     */
    val from: UUID

    /**
     * the name of the notification
     */
    val name: String

    /**
     * converts the notification to a sendable json
     */
    fun toJson(): JSONObject

}


/**
 * Extension function for
 * @see DefaultWebSocketSession
 *
 * sends a notification to the socket
 * @param notification the notification to send
 */
suspend fun DefaultWebSocketSession.send(notification: Notification) {
    send(notification.toJson().toString())
}