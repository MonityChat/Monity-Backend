package de.devin.monity.model

import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.network.wsrouting.WebSocketHandler
import de.devin.monity.util.Status
import de.devin.monity.util.notifications.Notification
import de.devin.monity.util.notifications.send
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import java.util.*

class OnlineUser(uuid: UUID, private val socketSession: DefaultWebSocketSession) : User(uuid), MessageSender {


    fun sendNotification(notification: Notification) {

        runBlocking {

            if (WebSocketHandler.isValidConnection(socketSession))
                socketSession.send(notification)
        }



    }

    fun setStatus(status: Status) {
        DetailedUserDB.setStatus(uuid, status)
    }

    fun updateLastSeen() {
        DetailedUserDB.updateLastSeen(uuid)
    }

    override fun sendMessageTo(to: OnlineUser, message: Message) {
    }

    override fun sendMessageTo(to: GroupChat, message: Message) {
    }
}