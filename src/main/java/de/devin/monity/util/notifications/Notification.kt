package de.devin.monity.util.notifications

import io.ktor.websocket.*
import org.json.JSONObject
import java.util.*

interface Notification {

    val from: UUID
    val name: String
    fun toJson(): JSONObject

}

suspend fun DefaultWebSocketSession.send(notification: Notification) {
    send(notification.toJson().toString())
}