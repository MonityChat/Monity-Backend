package de.devin.monity.network.wsrouting

import com.google.gson.Gson
import de.devin.monity.network.auth.AuthHandler
import de.devin.monity.network.auth.AuthLevel
import de.devin.monity.util.Error
import de.devin.monity.util.SimpleJSONReader
import de.devin.monity.util.toJSONString
import de.devin.monity.util.validUUID
import filemanagment.util.logInfo
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.schedule

object WebSocketHandler {

    private val socketAuthMap = HashMap<UUID, DefaultWebSocketSession>()

    suspend fun handleIncomingRequest(socket: DefaultWebSocketSession) {
        var valid = true
        Timer("schedule", false).schedule(5000) {
            valid = false
            runBlocking {
                if (!isValidConnection(socket)) {
                    logInfo("Closing Websocket connection because verification timed out")
                    socket.close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Verification time timed out"))
                }
            }
        }

        for (frame in socket.incoming) {
            if (!valid && socketAuthMap.containsValue(socket)) {
                socket.close(CloseReason.Codes.CANNOT_ACCEPT, Error.IDENTIFICATION_WINDOW_TIMEOUT)
                return
            }

            when (frame) {
                is Frame.Text -> {
                    val text = frame.readText()

                    val reader = SimpleJSONReader(text)
                    if (!reader.valid) {
                        socket.send(Error.INVALID_JSON_FORMAT)
                        return
                    }

                    val authKey = reader.json.getString("auth")

                    if (!validUUID(authKey)) return socket.send(Error.INVALID_UUID_FORMAT)

                    val auth = UUID.fromString(authKey)

                    if (!AuthHandler.isAuthenticated(auth)) {
                        return socket.send(Error.UNAUTHORIZED)
                    }

                    socketAuthMap[auth] = socket
                    valid = true

                    if (AuthHandler.getLevel(auth).weight < AuthLevel.AUTH_LEVEL_USER.weight)
                        return socket.send(Error.UNAUTHORIZED)

                    return socket.send(Error.NONE)
                }
                else -> {
                    socket.close(CloseReason.Codes.PROTOCOL_ERROR, Error.INVALID_JSON_FORMAT)
                    return
                }
            }
        }
    }

    suspend fun handleIncomingContent(content: Frame, session: DefaultWebSocketSession): Error {
        val text = when (content) {
            is Frame.Text -> {
                content.readText()
            }
            else -> return Error.INVALID_JSON_FORMAT
        }

        val reader = SimpleJSONReader(text)
        if (!reader.valid) return Error.INVALID_JSON_FORMAT

        val json = reader.json

        if (!json.has("action")) return Error.INVALID_JSON_STRUCTURE

        val action = json.getString("action")

        val back = ActionHandler.handleIncomingActionRequest(getAccordingUserTo(session), action, json)
        session.send(back)
        return Error.NONE
    }

    private fun getAccordingUserTo(session: DefaultWebSocketSession): UUID {
        if (session !in socketAuthMap.values) error ("Session is not valid")
        return socketAuthMap.keys.first { socketAuthMap[it] == session }
    }

    suspend fun DefaultWebSocketSession.send(json: JSONObject) {
        send(json.toString())
    }

    suspend fun DefaultWebSocketSession.send(error: Error) {
        send("{\"error\":\"${error}\"}")
    }

    suspend fun DefaultWebSocketSession.close(reason: CloseReason.Codes, error: Error) {
        close(CloseReason(reason, toJSONString(error)))
    }

    fun isValidConnection(session: DefaultWebSocketSession): Boolean {
        return socketAuthMap.containsValue(session)
    }
}