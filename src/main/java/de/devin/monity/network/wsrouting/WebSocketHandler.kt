package de.devin.monity.network.wsrouting

import de.devin.monity.model.OnlineUser
import de.devin.monity.network.auth.AuthHandler
import de.devin.monity.network.auth.AuthLevel
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.Error
import de.devin.monity.util.SimpleJSONReader
import de.devin.monity.util.dataconnectors.UserHandler
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

    /**
     * Handles incoming websockets request
     *
     * This will give the incoming connection a windows of 5 seconds to provide its auth key
     * @see AuthHandler
     * If the key is not provided the connection will be closed.
     * To be accepted the Key has to be at least LEVEL.USER otherwise the connection will be closed
     *
     * This function should be called from an asynchronous function because it will use blocking threads
     *
     * @param socket the incoming socket
     */
    suspend fun handleIncomingRequest(socket: DefaultWebSocketSession) {
        var valid = true
        Timer("WebSocketTimeOutTimer", false).schedule(5000) {
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

                    if (!reader.json.has("auth")) return socket.send(Error.INVALID_JSON_FORMAT)
                    if (!reader.json.has("user")) return socket.send(Error.INVALID_JSON_FORMAT)

                    val authKey = reader.json.getString("auth")
                    val userName = reader.json.getString("user")

                    if (!validUUID(authKey)) return socket.send(Error.INVALID_UUID_FORMAT)
                    if (!UserDB.hasEmailOrUser(userName)) return socket.send(Error.USER_NOT_FOUND)

                    val auth = UUID.fromString(authKey)

                    if (!AuthHandler.isAuthenticated(auth)) {
                        return socket.send(Error.UNAUTHORIZED)
                    }

                    val user = UserDB.getByUserOrEmail(userName)

                    socketAuthMap[user.uuid] = socket
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

    /**
     * Will handle incoming action requests from the client
     *
     * Whenever a client will send information via. the websocket this function will process it.
     * It will look for a correct JSON Syntax as well for a correct content
     *
     * In the last step it will give the processed content to the ActionHandler
     * @see ActionHandler.handleIncomingActionRequest
     *
     * @param content the incoming request
     * @param session the sender
     * @return Error if any occurs
     */
    fun handleIncomingContent(content: Frame, session: DefaultWebSocketSession): JSONObject {
        val text = when (content) {
            is Frame.Text -> {
                content.readText()
            }
            else -> return Error.INVALID_JSON_FORMAT.toJson()
        }

        logInfo("Received request $text")

        val reader = SimpleJSONReader(text)
        if (!reader.valid) return Error.INVALID_JSON_FORMAT.toJson()

        val json = reader.json

        if (!json.has("action")) return Error.INVALID_JSON_STRUCTURE.toJson()

        val action = json.getString("action")

        val back = ActionHandler.handleIncomingActionRequest(getAccordingUserTo(session), action, json)
        return back
    }

    private fun getAccordingUserTo(session: DefaultWebSocketSession): UUID {
        if (session !in socketAuthMap.values) error ("Session is not valid")
        return socketAuthMap.keys.first { socketAuthMap[it] == session }
    }

    fun closed(session: DefaultWebSocketSession) {
        val user = getAccordingUserTo(session)
        socketAuthMap.remove(user)
    }

    fun getUserFrom(session: DefaultWebSocketSession): OnlineUser {
        return UserHandler.getOnlineUser(getAccordingUserTo(session))
    }

    /**
     * Allows to send direct JSON Objects to the socket
     *
     * Extension function for
     * @see WebSocketSession
     *
     *
     * @param json the json to send
     */
    suspend fun WebSocketSession.send(json: JSONObject) {
        send(json.toString())
    }


    /**
     * Allows to send direct Error Objects to the socket
     *
     * Extension function for
     * @see WebSocketSession
     *
     *
     * @param error the error to send
     */
    suspend fun WebSocketSession.send(error: Error) {
        send(error.toJson())
    }

    /**
     * Allows to close the connection with a reason and error directly
     *
     * Extension function for
     * @see WebSocketSession
     *
     *
     * @param reason the reason the connection was closed
     * @param error the error to send
     */
    suspend fun WebSocketSession.close(reason: CloseReason.Codes, error: Error) {
        close(CloseReason(reason, toJSONString(error)))
    }

    fun isConnected(uuid: UUID): Boolean {
        return socketAuthMap.containsKey(uuid)
    }

    fun getConnection(uuid: UUID): DefaultWebSocketSession {
        return socketAuthMap[uuid]!!
    }
    fun isValidConnection(session: DefaultWebSocketSession): Boolean {
        return socketAuthMap.containsValue(session)
    }
}