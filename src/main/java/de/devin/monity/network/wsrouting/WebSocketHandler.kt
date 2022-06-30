package de.devin.monity.network.wsrouting

import de.devin.monity.model.OnlineUser
import de.devin.monity.network.auth.AuthHandler
import de.devin.monity.network.auth.AuthLevel
import de.devin.monity.network.db.chat.MessageDB
import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.network.db.user.UserContactDB
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.*
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.PrivateChatUserReceivedMessagesNotification
import de.devin.monity.util.notifications.UserWentOfflineNotification
import de.devin.monity.util.notifications.UserWentOnlineNotification
import de.devin.monity.util.logInfo
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Util class helping to manage incoming websockets and their packages
 */
object WebSocketHandler {

    private val socketAuthMap = HashMap<UUID, DefaultWebSocketSession>()
    private val socketValidationExecutor = ScheduledThreadPoolExecutor(12)


    fun startValidationService() {
        socketValidationExecutor.scheduleAtFixedRate(SocketValidationTask, 0, 1000, TimeUnit.MILLISECONDS)
    }

    fun startActivityService() {
        socketValidationExecutor.scheduleAtFixedRate(UserActivityTimer, 0, 1, TimeUnit.MINUTES)
    }

    /**
     * Handles incoming websockets request
     *
     * This will give the incoming connection a windows of 3 seconds to provide its auth key
     * @see AuthHandler
     * If the key is not provided the connection will be closed.
     * To be accepted the Key has to be at least LEVEL.USER otherwise the connection will be closed
     *
     * This function does not work in a normal way. It will set the connection to valid or invalid latest at 3 seconds.
     * If the connection is valid before it will already set the connection to valid and allow for further executions.
     * But if the connection is not valid this method can take up to 3 seconds. It will execute the 3 seconds timer
     * asynchronously
     * The thread will automatically cancel itself after completion.
     *
     * @param socket the incoming socket
     */
    suspend fun handleIncomingRequest(socket: DefaultWebSocketSession) {
        var valid = true
        SocketValidationTask.addWaitingConnection(socket) { valid = false }

        for (frame in socket.incoming) { //everything the client sends in this 3-second window will be handled here
            if (!valid && socketAuthMap.containsValue(socket)) {
                socket.close(CloseReason.Codes.CANNOT_ACCEPT, Error.IDENTIFICATION_WINDOW_TIMEOUT)
                return
            }

            when (frame) {
                is Frame.Text -> {
                    val text = frame.readText()

                    val reader = SimpleJSONReader(text)
                    if (!reader.valid) {
                        socket.sendAndClose(Error.INVALID_JSON_FORMAT)
                        return
                    }

                    if (!reader.json.has("auth")) return socket.sendAndClose(Error.INVALID_JSON_FORMAT)
                    if (!reader.json.has("user")) return socket.sendAndClose(Error.INVALID_JSON_FORMAT)

                    val authKey = reader.json.getString("auth")
                    val userName = reader.json.getString("user")

                    if (!validUUID(authKey)) return socket.sendAndClose(Error.INVALID_UUID_FORMAT)

                    val auth = UUID.fromString(authKey)
                    val user = UserDB.getByUserOrEmail(userName)

                    if (!UserDB.hasEmailOrUser(userName)) return socket.sendAndClose(Error.USER_NOT_FOUND)
                    if (!AuthHandler.isAuthInUse(auth)) return socket.sendAndClose(Error.AUTH_NOT_BOUND)
                    if (AuthHandler.getUserForAuth(auth) != user.uuid) return socket.sendAndClose(Error.AUTH_IN_USE)


                    if (!AuthHandler.isAuthenticated(auth)) {
                        return socket.sendAndClose(Error.UNAUTHORIZED)
                    }

                    if (AuthHandler.getLevel(auth).weight < AuthLevel.AUTH_LEVEL_USER.weight)
                        return socket.sendAndClose(Error.UNAUTHORIZED)


                    logInfo("Allowing connection to pass from $userName")
                    socketAuthMap[user.uuid] = socket

                    executeLoginActions(user.uuid)
                    UserActivityTimer.userExecutedAction(user.uuid)
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
     * Will execute all default actions when a user connects to the webserver
     * @param user who logged in
     */
    private fun executeLoginActions(user: UUID) {
        if (!UserHandler.isOnline(user)) error("Cant execute login actions when user is offline")

        //Alle nachrichten auf erhalten setzen
        val onlineUser = UserHandler.getOnlineUser(user)
        for (chat in onlineUser.privateChats) {
            for (message in chat.messages.filter { it.sender != onlineUser.uuid }) {
                if (message.status == MessageStatus.PENDING) {
                    MessageDB.editMessageStatus(message.messageID, MessageStatus.RECEIVED)
                    UserHandler.sendNotificationIfOnline(if (chat.initiator == user) chat.otherUser else chat.initiator,
                        PrivateChatUserReceivedMessagesNotification(user, chat.id))
                }
            }
        }
        //Seinen eingestellten Status setzen
        DetailedUserDB.setStatus(onlineUser.uuid, DetailedUserDB.get(user).preferredStatus)

        for (contact in onlineUser.contacts) {
            UserHandler.sendNotificationIfOnline(contact, UserWentOnlineNotification(user))
        }

        DetailedUserDB.updateLastSeen(onlineUser.uuid)
    }

    /**
     * Will execute when a user logs of
     * @param user logging off
     */
    private fun executeLogoutActions(user: UUID) {
        DetailedUserDB.setStatus(user, Status.OFFLINE)

        for (contact in UserContactDB.getContactsFrom(user)) {
            UserHandler.sendNotificationIfOnline(contact, UserWentOfflineNotification(user))
        }

        DetailedUserDB.updateLastSeen(user)
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

        val reader = SimpleJSONReader(text)
        if (!reader.valid) return Error.INVALID_JSON_FORMAT.toJson()

        val json = reader.json

        if (!json.has("action")) return Error.INVALID_JSON_STRUCTURE.toJson()



        val action = json.getString("action")

        return ActionHandler.handleIncomingActionRequest(getAccordingUserTo(session), action, json)
    }

    /**
     * This will close the connection to the websocket and execute all log out actions
     * Also it will end the websocket thread
     */
    fun closeConnection(session: DefaultWebSocketSession) {
        val uuid = getOldUUIDFrom(session) //this is an old session because the connection is already not active anymore
        logInfo("Closing websocket to User $uuid")
        AuthHandler.removeAuthBoundToUser(uuid)
        closed(session)
        executeLogoutActions(uuid)
    }

    private fun getAccordingUserTo(session: DefaultWebSocketSession): UUID {
        if (session !in socketAuthMap.values) error ("Session is not valid")
        return socketAuthMap.keys.first { socketAuthMap[it] == session }
    }

    private fun closed(session: DefaultWebSocketSession) {
        val user = getAccordingUserTo(session)
        socketAuthMap.remove(user)
    }

    private fun getOldUUIDFrom(session: DefaultWebSocketSession): UUID {
        return socketAuthMap.keys.first { socketAuthMap[it] == session }
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
     * Allows to send direct Error Objects to the socket and close the connection afterwards
     *
     * Extension function for
     * @see WebSocketSession
     *
     *
     * @param error the error to send
     */
    suspend fun WebSocketSession.sendAndClose(error: Error) {
        send(error.toJson())
        close(CloseReason.Codes.PROTOCOL_ERROR, error)
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

    /**
     * Allows to close the connection with a reason and error directly
     *
     * Extension function for
     * @see WebSocketSession
     *
     *
     * @param reason the reason the connection was closed
     * @param error the error to send
     * @param errorReason why did the error occur
     */
    suspend fun WebSocketSession.close(reason: CloseReason.Codes, error: Error, errorReason: String) {
        close(CloseReason(reason, error.toJson().put("reason", errorReason).toString()))
    }


    /**
     * Whether the user is connected or not
     * @return true if connected false otherwise
     */
    fun isConnected(uuid: UUID): Boolean {
        return socketAuthMap.containsKey(uuid)
    }

    /**
     * Gets the connection from the UUID
     * @warning this can cause a NPE if there is no valid connection. Use
     * @see WebSocketHandler.isConnected before using this function
     * @return the DefaultWebSocketSession for the user
     */
    fun getConnection(uuid: UUID): DefaultWebSocketSession {
        return socketAuthMap[uuid]!!
    }

    /**
     * Checks if the connection is still valid
     * This means if it still connection and if its active
     * @return true if valid false otherwsie
     */
    fun isValidConnection(session: DefaultWebSocketSession): Boolean {
        return socketAuthMap.containsValue(session) && session.isActive
    }
}