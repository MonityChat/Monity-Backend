package de.devin.monity.util.notifications

import io.ktor.http.cio.websocket.*
import org.json.JSONObject
import java.util.UUID

interface Notification {

    val from: UUID
    val name: String
    fun toJson(): JSONObject

}

suspend fun DefaultWebSocketSession.send(notification: Notification) {
    send(notification.toJson().toString())
}