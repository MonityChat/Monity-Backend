package de.devin.monity.util

import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.UserStoppedTypingNotification
import java.util.Timer
import java.util.TimerTask
import java.util.UUID


data class TypeData(val typer: UUID, val target: UUID, val chatID: UUID)
object TypingManager {

    private val typingTimer = Timer("TypingTimer")
    private val typingUsers = mutableMapOf<TypeData, Int>()

    fun loadTimer() {
        typingTimer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                typingUsers.keys.forEach { typingUsers[it] = typingUsers[it]!! - 1 }
                typingUsers.keys.forEach {
                    if (typingUsers[it]!! <= 0) {
                        val data = it
                        typingUsers.remove(it)
                        UserHandler.sendNotificationIfOnline(data.target, UserStoppedTypingNotification(data.typer, data.chatID))
                    }
                }
            }
        }, 0, 1500)
    }

    fun startedTyping(user: UUID, target: UUID, chatID: UUID) {
        typingUsers[TypeData(user, target, chatID)] = 2
    }

    fun stoppedTypingBecauseSendMessage(user: UUID, target: UUID, chatID: UUID) {
        val data = typingUsers.keys.first { it.typer == user && it.target == target && it.chatID == chatID }
        typingUsers.remove(data)
        UserHandler.sendNotificationIfOnline(data.target, UserStoppedTypingNotification(data.typer, data.chatID))
    }

    fun typeUpdate(user: UUID, target: UUID, chatID: UUID) {
        val data = typingUsers.keys.first { it.typer == user && it.target == target && it.chatID == chatID }
        typingUsers[data] = 2
    }

    fun isTyping(user: UUID, target: UUID, chatID: UUID): Boolean {
        return typingUsers.keys.any { it.typer == user && it.target == target && it.chatID == chatID }
    }



}