package de.devin.monity.util

import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.UserStoppedTypingNotification
import java.util.Timer
import java.util.TimerTask
import java.util.UUID


data class TypeData(val typer: UUID, val target: UUID, val chatID: UUID)

/**
 * Util class to handle users typing on the frontend.
 *
 * If a user types he will be remembered with a given integer value.
 * This value will be decreased after a certain time amount. If the value goes to 0 the user stopped typing.
 * If the user types again in the time the value will go back to the original value and be again count down.
 *
 * This is a simple way to know when the user types and when he stopped. It will add a bit of delay when the user stopped typing
 * and when this util class recognizes it. But the delay is minimal.
 *
 */
object TypingManager {

    private val typingTimer = Timer("TypingTimer")
    private val typingUsers = mutableMapOf<TypeData, Int>()

    /**
     * Will start the timer which will check if the user is still typing.
     */
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

    /**
     * When a user started typing.
     * @param user the user who is typing
     * @param target who the user is typing to
     * @param chatID in which chat the user is typing
     */
    fun startedTyping(user: UUID, target: UUID, chatID: UUID) {
        typingUsers[TypeData(user, target, chatID)] = 2
    }

    /**
     * When the user stopped because he sent a message.
     *
     * @param user the user who stopped typing
     * @param target who he stopped typing to
     * @param chatID in which chat he stopped typing
     */
    fun stoppedTypingBecauseSendMessage(user: UUID, target: UUID, chatID: UUID) {
        val data = typingUsers.keys.first { it.typer == user && it.target == target && it.chatID == chatID }
        typingUsers.remove(data)
        UserHandler.sendNotificationIfOnline(data.target, UserStoppedTypingNotification(data.typer, data.chatID))
    }

    /**
     * When the user is already typing and typed again
     * @param user the user who typed again
     * @param target the user he typed to
     * @param chatID the chat in which he typed
     */
    fun typeUpdate(user: UUID, target: UUID, chatID: UUID) {
        val data = typingUsers.keys.first { it.typer == user && it.target == target && it.chatID == chatID }
        typingUsers[data] = 2
    }

    /**
     * Whether the user is typing or not
     * @param user the user if he is typing or not
     * @param target who he might be typing to
     * @param chatID in which the user might be typing
     * @return whether the user is typing or not
     */
    fun isTyping(user: UUID, target: UUID, chatID: UUID): Boolean {
        return typingUsers.keys.any { it.typer == user && it.target == target && it.chatID == chatID }
    }



}