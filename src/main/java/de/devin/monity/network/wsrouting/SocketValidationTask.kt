package de.devin.monity.network.wsrouting

import de.devin.monity.util.logInfo
import io.ktor.websocket.*
import de.devin.monity.util.Error
import de.devin.monity.network.wsrouting.WebSocketHandler.close
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking


/**
 * This util class is used to handle the verification timeout which can occur while a new websocket is connecting to the system
 * @see WebSocketHandler.handleIncomingRequest
 *
 * To achieve this every new incoming request will be saved here with the time they did connected.
 * A function which runs every second then checks if the connection is valid if the time between connection and now is greater than 5 seconds.
 * If the connection is invalid at this point it will close the connection.
 * @see SocketValidationTask.run
 *
 */
object SocketValidationTask: Runnable {

    private val waitingConnections = HashMap<DefaultWebSocketSession, Pair<Long, () -> Unit>>()

    /**
     * Adds a new pending connection which has to be verified
     * @param session the session
     * @param onFail a method which will execute when the connection request fails
     */
    fun addWaitingConnection(session: DefaultWebSocketSession, onFail: () -> Unit) {
        waitingConnections[session] = Pair(System.currentTimeMillis(), onFail)
    }

    private fun removeWaitingConnection(session: DefaultWebSocketSession) {
        waitingConnections.remove(session)
    }

    override fun run() {
        runBlocking {
            for (waitingConnection in waitingConnections.keys) {
                val pair = waitingConnections[waitingConnection]!!
                val delta = System.currentTimeMillis() - pair.first
                if (delta > 5000) {
                    removeWaitingConnection(waitingConnection)

                    if (!WebSocketHandler.isValidConnection(waitingConnection)) {
                        logInfo("Closing Websocket connection because verification timed out")
                        waitingConnection.close(CloseReason.Codes.CANNOT_ACCEPT, Error.IDENTIFICATION_WINDOW_TIMEOUT)
                        waitingConnection.cancel()
                        pair.second() //execute given onFail method
                    }
                }
            }
        }
    }
}