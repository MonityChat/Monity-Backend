package de.devin.monity.model

interface MessageSender {

    fun sendMessageTo(to: OnlineUser, message: Message)
    fun sendMessageTo(to: GroupChat, message: Message)

}