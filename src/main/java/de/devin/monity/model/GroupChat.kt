package de.devin.monity.model

import de.devin.monity.network.db.chat.GroupProfile

class GroupChat(val members: User, val initiator: User, val profile: GroupProfile, val messages: List<Message>) {
}