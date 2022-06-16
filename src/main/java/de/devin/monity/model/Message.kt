package de.devin.monity.model

import java.util.UUID

data class Message(val id: UUID, val chatID: UUID, var index: Long, val content: String, val sender: UUID, val sent: Long, val embed: UUID?)