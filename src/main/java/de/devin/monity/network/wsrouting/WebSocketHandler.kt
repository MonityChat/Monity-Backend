package de.devin.monity.network.wsrouting

import com.google.gson.JsonObject
import io.ktor.http.cio.websocket.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.concurrent.schedule

object WebSocketHandler {

    private val socketAuthMap = HashMap<UUID, DefaultWebSocketSession>()

    suspend fun handleIncomingRequest(socket: DefaultWebSocketSession) {
        var valid = true
        Timer("schedule", false).schedule(5000) {
            valid = false
        }

        for (frame in socket.incoming) {
            if (!valid && socketAuthMap.containsValue(socket)) {
                socket.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid request Time"))
                return
            }

            when (frame) {
                is Frame.Text -> {
                    val text = frame.readText()
                    val json = JSONObject(text)

                    if (!json.has("key")) {
                        socket.send()
                    }

                }
                else -> {
                    socket.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Invalid request Time"))
                    return
                }
            }
        }
    }
}