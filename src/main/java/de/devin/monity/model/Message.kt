package de.devin.monity.model

import java.util.UUID

class Message(val content: String, val sender: UUID, val sent: Long, val embed: UUID?) {
}