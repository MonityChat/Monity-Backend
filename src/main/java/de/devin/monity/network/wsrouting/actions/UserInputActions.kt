package de.devin.monity.network.wsrouting.actions

import de.devin.monity.util.TypingManager
import org.json.JSONObject
import java.util.*
import de.devin.monity.util.Error
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.UserStartedTypingNotification

class UserTypingAction: Action {

    override val name: String
        get() = "user:action:typing"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("target"), Parameter("chatID"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val targetUUID = UUID.fromString(request.getString("target"))
        val chatID = UUID.fromString(request.getString("chatID"))

        if (TypingManager.isTyping(sender, targetUUID, chatID)) {
            TypingManager.typeUpdate(sender, targetUUID, chatID)
        } else {
            TypingManager.startedTyping(sender, targetUUID, chatID)
        }

        UserHandler.sendNotificationIfOnline(targetUUID, UserStartedTypingNotification(sender, chatID))

        return Error.NONE.toJson()
    }
}