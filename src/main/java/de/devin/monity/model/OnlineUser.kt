package de.devin.monity.model

import de.devin.monity.network.db.UserProfile
import de.devin.monity.network.httprouting.UserData
import de.devin.monity.util.notifications.Notification
import de.devin.monity.util.notifications.send
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.runBlocking

class OnlineUser(userData: UserData, userProfile: UserProfile, socketSession: DefaultWebSocketSession) : User(userData, userProfile,
    socketSession
), MessageSender {


    fun sendNotification(notification: Notification) {
        runBlocking {
            socketSession.send(notification)
        }
    }

    override fun sendMessageTo(to: OnlineUser, message: Message) {
    }

    override fun sendMessageTo(to: Group, message: Message) {
    }
}