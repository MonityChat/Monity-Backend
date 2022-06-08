package de.devin.monity.model

import java.util.*

interface MessageSender {

    fun sendMessageTo(to: OnlineUser, message: Message)
    fun sendMessageTo(to: Group, message: Message)

}